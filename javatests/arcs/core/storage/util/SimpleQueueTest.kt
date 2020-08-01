package arcs.core.storage.util

import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class SimpleQueueTest {

    @Test
    fun enqueueSingleJob() = runBlockingTest {
        val queue = simpleQueue()
        val deferred = CompletableDeferred<Unit>()
        queue.enqueue {
            delay(10)
            deferred.complete(Unit)
        }
        withTimeout(5000) {
            deferred.await()
        }
        queue.close()
    }

    @Test
    fun enqueueSingleJ2obAndWait() = runBlockingTest {
        val queue = simpleQueue()
        var ran = false
        withTimeout(5000) {
            queue.enqueueAndWait { ran = true }
        }
        assertThat(ran).isTrue()
        queue.close()
    }

    @Test
    fun onEmptyIsCalled() = runBlockingTest {
        val deferred = CompletableDeferred<Unit>()
        val queue = simpleQueue(
            onEmpty = {
                deferred.complete(Unit)
            }
        )
        queue.enqueue { }

        withTimeout(5000) {
            deferred.await()
        }
        queue.close()
    }

    @Test
    fun enqueueSerialized() = runBlockingTest {
        val active = atomic(0)
        val ran = atomic(0)
        val deferred = CompletableDeferred<Unit>()
        val queue = simpleQueue(
            onEmpty = {
                assertThat(active.incrementAndGet()).isEqualTo(1)
                active.decrementAndGet()
                deferred.complete(Unit)
            }
        )

        val jobCount = 1000
        repeat(jobCount) {
            queue.enqueue {
                assertThat(active.incrementAndGet()).isEqualTo(1)
                suspendCancellableCoroutine<Unit> { it.resume(Unit) {} }
                active.decrementAndGet()
                ran.incrementAndGet()
            }
        }
        deferred.await()
        assertThat(ran.value).isEqualTo(jobCount)
        queue.close()
    }
}
