package arcs.core.storage

import arcs.core.crdt.CrdtData
import arcs.core.crdt.CrdtModel
import arcs.core.crdt.CrdtOperationAtTime
import arcs.core.crdt.VersionMap
import arcs.core.util.Scheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred

/**
 * [IStorageProxy] is an intermediary between a [Handle] and [Store]. It provides up-to-date CRDT
 * state to readers, and ensures write operations apply cleanly before forwarding to the store.
 *
 * @param T the consumer data type for the model behind this proxy
 * @param initialCrdt the CrdtModel instance [IStorageProxy] will apply ops to.
 */
interface IStorageProxy<Data : CrdtData, Op : CrdtOperationAtTime, T> {
    /**
     * If you need to interact with the data managed by this [IStorageProxy], and you're not a
     * [Store], you must either be performing your interactions within a handle callback or on this
     * [CoroutineDispatcher].
     */
    val dispatcher: CoroutineDispatcher

    /** The [StorageKey] that identifies the [Store] this proxy talks to. */
    val storageKey: StorageKey

    /**
     * Return a copy of the current version map.
     */
    fun getVersionMap(): VersionMap

    /**
     * [AbstractArcHost] calls this (via [Handle]) to thread storage events back
     * to the [ParticleContext], which manages the [Particle] lifecycle API.
     */
    fun registerForStorageEvents(id: CallbackIdentifier, notify: (StorageEvent) -> Unit)

    /**
     *  Remove all `onUpdate`, `onReady`, `onDesync` and `onResync` callbacks associated with the
     *  provided `handleName`.
     *
     * A [Handle] that is being removed from active usage should make sure to trigger this method
     * on its associated [IStorageProxy].
     */
    fun removeCallbacksForName(id: CallbackIdentifier)

    /**
     * Add a [Handle] `onReady` action associated with a [Handle] name.
     *
     * If the [StorageProxyImpl] is synchronized when the action is added, it will be called
     * on the next iteration of the [Scheduler].
     */
    fun addOnReady(id: CallbackIdentifier, action: () -> Unit)

    /**
     * Add a [Handle] `onUpdate` action associated with a [Handle] name.
     */
    fun addOnUpdate(id: CallbackIdentifier, action: (oldValue: T, newValue: T) -> Unit)

    /**
     * Add a [Handle] `onDesync` action associated with a [Handle] name.
     *
     * If the [StorageProxyImpl] is desynchronized when the action is added, it will be called
     * on the next iteration of the [Scheduler].
     */
    fun addOnDesync(id: CallbackIdentifier, action: () -> Unit)

    /**
     * Add a [Handle] `onResync` action associated with a [Handle] name.
     */
    fun addOnResync(id: CallbackIdentifier, action: () -> Unit)

    /**
     * Similar to [getParticleView], but requires the current proxy to have been synced at least
     * once, and also requires the caller to be running within the [Scheduler]'s thread.
     */
    fun getParticleViewUnsafe(): T

    /**
     * Apply a CRDT operation to the [CrdtModel] that this [StorageProxyImpl] manages, notifies read
     * handles, and forwards the write to the [Store].
     */
    @Suppress("DeferredIsResult")
    fun applyOp(op: Op): Deferred<Boolean> = applyOps(listOf(op))

    /**
     * Applies an ordered [List] of CRDT operations to the [CrdtModel] that this [IStorageProxy]
     * manages, notifies read handles, and forwards the writes to the [Store].
     */
    @Suppress("DeferredIsResult")
    fun applyOps(ops: List<Op>): Deferred<Boolean>

    /**
     * If the [IStorageProxy] has previously been set up for synchronized mode, send a sync request
     * to the backing store and move to [AWAITING_SYNC].
     */
    fun maybeInitiateSync()

    /**
     * If the [IStorageProxy] is associated with any readable handles, it will need to operate
     * in synchronized mode. This is done via a two-step process:
     *   1) When constructed, all readable handles call this method to move the proxy from its
     *      initial state of [NO_SYNC] to [READY_TO_SYNC].
     *   2) [ParticleContext] then triggers the actual sync request after the arc has been
     *      set up and all particles have received their onStart events.
     */
    fun prepareForSync()

    /**
     * Closes this [IStorageProxy]. It will no longer receive messages from its associated [Store].
     * Attempting to perform an operation on a closed [IStorageProxy] will result in an exception
     * being thrown.
     */
    suspend fun close()

    /**
     * Two-dimensional identifier for handle callbacks. Typically this will be the handle's name,
     * as well as its particle's ID.
     */
    data class CallbackIdentifier(val handleName: String, val namespace: String = "")

    /**
     * Event types used for notifying the [ParticleContext] to drive the [Particle]'s
     * storage events API.
     */
    enum class StorageEvent {
        READY,
        UPDATE,
        DESYNC,
        RESYNC
    }
}
