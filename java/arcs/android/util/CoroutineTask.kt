package arcs.android.util

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * This is a base class that we can use for any workers that we want to run asynchronously on a
 * provided [CorooutineScope].
 */
abstract class CoroutineTask(
    appContext: Context,
    workerParameters: WorkerParameters
) : ListenableWorker(appContext, workerParameters) {

    /** Implementations can provide the [CoroutineScope] for execution here. */
    abstract val jobScope: CoroutineScope

    abstract suspend fun doWork(): Result

    override fun startWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()
        val job = jobScope.launch { future.set(doWork()) }
        future.addJobCancellation(job)
        return future
    }

    private fun ListenableFuture<*>.addJobCancellation(job: Job) {
        addListener(
            { if(isCancelled) job.cancel() },
            this@CoroutineTask.taskExecutor.backgroundExecutor
        )
    }
}
