package com.yopers.renee.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yopers.renee.ObjectBox
import com.yopers.renee.models.Task
import com.yopers.renee.models.Task_
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import timber.log.Timber

class SyncManager(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val taskBox: Box<Task> = ObjectBox.boxStore.boxFor()
        val task: Task? = taskBox.query().equal(Task_.taskId, id.toString()).build().findFirst()

        if (task != null) {
            Timber.i("Background id ${task.taskId} from source ${task.source} to destination ${task.destination} successfull")
            return Result.success()
        } else {
            Timber.i("Background id $id failed")
            return Result.failure()
        }
    }
}