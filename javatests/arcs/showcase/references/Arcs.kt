@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package arcs.showcase.references

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import arcs.android.sdk.host.AndroidHost
import arcs.core.allocator.Allocator
import arcs.core.allocator.Arc
import arcs.core.common.ArcId
import arcs.core.data.Plan
import arcs.core.host.EntityHandleManager
import arcs.core.host.ParticleState
import arcs.core.host.toRegistration
import arcs.core.host.SchedulerProvider
import arcs.jvm.host.ExplicitHostRegistry
import arcs.jvm.host.JvmSchedulerProvider
import arcs.jvm.util.JvmTime
import arcs.sdk.Particle
import arcs.sdk.android.storage.ServiceStoreFactory
import arcs.sdk.android.storage.service.ConnectionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.EmptyCoroutineContext

/** Container for WriteRecipe specific things */
class ArcsStorage(private val arcs: Arcs) {
    fun all0(): List<MyLevel0> {
        return runBlocking {
            arcs.getParticle<Reader0>(WriteRecipePlan).read()
        }
    }

    fun put0(item: MyLevel0) {
        runBlocking {
            arcs.getParticle<Writer0>(WriteRecipePlan).write(item)
        }
    }

    fun all1(): List<MyLevel1> {
        return runBlocking {
            arcs.getParticle<Reader1>(WriteRecipePlan).read()
        }
    }

    fun put1(item: MyLevel1) {
        runBlocking {
            arcs.getParticle<Writer1>(WriteRecipePlan).write(item)
        }
    }

    fun all2(): List<MyLevel2> {
        return runBlocking {
            arcs.getParticle<Reader2>(WriteRecipePlan).read()
        }
    }

    fun put2(item: MyLevel2) {
        runBlocking {
            arcs.getParticle<Writer2>(WriteRecipePlan).write(item)
        }
    }

    fun stop() {
        runBlocking {
            arcs.stopArcForPlan(WriteRecipePlan)
        }
    }
}

/** Container to own the allocator and start the long-running arc. */
class Arcs(
    private val context: Context,
    // A test [ConnectionFactory] can be provided here under test.
    // In production, leave this parameter as null. Arcs will provide a default implementation.
    connectionFactory: ConnectionFactory? = null
) {
    val schedulerProvider = JvmSchedulerProvider(EmptyCoroutineContext)

    private val fakeLifecycleOwner = object : LifecycleOwner {
        private val lifecycle = LifecycleRegistry(this)
        override fun getLifecycle() = lifecycle
    }

    val arcHost = ArcHost(
        context,
        fakeLifecycleOwner.lifecycle,
        schedulerProvider,
        connectionFactory
    )

    private val allocatorMutex = Mutex()
    private lateinit var _allocator: Allocator

    private suspend fun allocator(): Allocator = allocatorMutex.withLock {
        if(!::_allocator.isInitialized) {
            val hostRegistry = ExplicitHostRegistry().apply {
                registerHost(arcHost)
            }
            _allocator = Allocator.create(
                hostRegistry,
                EntityHandleManager(
                    arcId = "allocator",
                    hostId = "allocator",
                    time = JvmTime,
                    scheduler = schedulerProvider.invoke("allocator")
                )
            )
        }
        _allocator
    }

    suspend fun <T : Particle> getParticle(
        plan: Plan,
        particleName: String
    ): T {
        val arc = startArc(plan)
        return getParticle(arc.id, particleName)
    }

    suspend fun startArc(plan: Plan): Arc {
        val arc = allocator().startArcForPlan(plan)
        arc.waitForStart()
        return arc
    }

    suspend fun stopArcForPlan(plan: Plan) {
        // Start the arc so that we have an instance of an arc to call stop on. Note this means that
        // calling stop on an unstarted Arc will result in the Arc being started and immediately
        // stopped.
        val allocatorArc = allocator().startArcForPlan(plan)
        allocatorArc.stop()
        allocatorArc.waitForStop()
    }
    suspend inline fun <reified T: Particle> getParticle(plan: Plan) = getParticle<T>(plan, T::class.simpleName!!)

    fun <T : Particle> getParticle(arcId: ArcId, particleName: String): T {
        return arcHost.getParticle(arcId.toString(), particleName)
    }


}

/**
 * An [ArcHost] that exposes the ability to get instances of particles.
 */
class ArcHost(
    context: Context,
    lifecycle: Lifecycle,
    schedulerProvider: SchedulerProvider,
    connectionFactory: ConnectionFactory? = null
) : AndroidHost(
    context,
    lifecycle,
    schedulerProvider,
    ::Reader0.toRegistration(),
    ::Writer0.toRegistration(),
    ::Reader1.toRegistration(),
    ::Writer1.toRegistration(),
    ::Reader2.toRegistration(),
    ::Writer2.toRegistration()
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val activationFactory = ServiceStoreFactory(
        context,
        lifecycle,
        connectionFactory = connectionFactory
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> getParticle(arcId: String, particleName: String): T {
        val arcHostContext = requireNotNull(getArcHostContext(arcId)) {
            "ArcHost: No arc host context found for $arcId"
        }
        val particleContext = requireNotNull(arcHostContext.particles[particleName]) {
            "ArcHost: No particle named $particleName found in $arcId"
        }
        val allowableStartStates = arrayOf(ParticleState.Running, ParticleState.Waiting)
        check(particleContext.particleState in allowableStartStates) {
            "ArcHost: Particle $particleName has failed, or not been started"
        }

        @Suppress("UNCHECKED_CAST")
        return particleContext.particle as T
    }
}

