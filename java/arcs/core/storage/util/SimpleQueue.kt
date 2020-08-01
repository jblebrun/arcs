package arcs.core.storage.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * A very basic implementation of [OperationQueue]. It dispatches jobs on coroutines in the
 * coroutine context of callers of the [enqueue] method.
 *
 * If an [onEmpty] method is provided at construction, that method will run at the end of
 * any drain cycle.
 */
class SimpleQueue(
    coroutineScope: CoroutineScope,
    private val onEmpty: (suspend () -> Unit)? = null
) : OperationQueue {

    private val queue = Channel<suspend () -> Unit>()
    // Holds up to one "empty" job, which will be selected whenever the queue goes empty
    private val empty = Channel<suspend () -> Unit>(1)

    @Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")
    private val drainJob = coroutineScope.launch {
        while (isActive) {
            select<Unit> {
                // If a job is present, this gets chosen first.
                queue.onReceiveOrNull { it?.invoke() ?: cancel() }
                // If no job was present, there should be 1 empty func pending here.
                empty.onReceiveOrNull { it?.invoke() }
            }
        }
    }

    /**
     * Places the provided block on the internal operation queue.
     *
     * If there is not a coroutine currently draining the queue, one will be started, using
     * the coroutineContext of the caller.
     *
     * It's safe to call this method from any thread/coroutine.
     */
    override suspend fun enqueue(op: Op) {
        queue.send(op)
        onEmpty?.let { empty.offer(it) }
    }

    fun close() {
        queue.close()
    }

    suspend fun join() {
        drainJob.join()
    }
}

fun CoroutineScope.simpleQueue(onEmpty: (suspend () -> Unit)? = null) =
    SimpleQueue(this, onEmpty)
