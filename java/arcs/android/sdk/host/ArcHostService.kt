/*
 * Copyright 2020 Google LLC.
 *
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 *
 * Code distributed by Google as part of this project is also subject to an additional IP rights
 * grant found at
 * http://polymer.github.io/PATENTS.txt
 */
package arcs.android.sdk.host

import android.content.Intent
import androidx.lifecycle.LifecycleService
import arcs.core.host.AbstractArcHost
import arcs.core.host.ArcHost
import arcs.sdk.android.storage.AndroidStorageServiceResurrectionManager
import arcs.sdk.android.storage.service.StorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Base [Service] for embedders of [ArcHost].
 */
@ExperimentalCoroutinesApi
abstract class ArcHostService : LifecycleService() {
    protected val scope: CoroutineScope = MainScope()

    abstract val arcHost: ArcHost

    open val storageServiceClass: Class<out StorageService> = StorageService::class.java

    private val resurrectionManager by lazy {
        AndroidStorageServiceResurrectionManager(
            applicationContext,
            storageServiceClass
        )
    }

    /**
     * Subclasses must override this with their own [ArcHost]s.
     */
    open val arcHosts: List<ArcHost> by lazy {
        listOf(arcHost).onEach {
            if (it is AbstractArcHost) {
                it.resurrectionManager = resurrectionManager
            }
        }
    }

    private val arcHostHelper: ArcHostHelper by lazy {
        ArcHostHelper(this, resurrectionManager, *arcHosts.toTypedArray())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        scope.launch {
            arcHostHelper.onStartCommandSuspendable(intent)
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
