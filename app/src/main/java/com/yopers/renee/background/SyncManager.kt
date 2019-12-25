package com.yopers.renee.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yopers.renee.ObjectBox
import com.yopers.renee.models.Task
import com.yopers.renee.models.Task_
import com.yopers.renee.models.User
import com.yopers.renee.models.User_
import com.yopers.renee.utils.Builder
import com.yopers.renee.utils.Download
import io.minio.MinioClient
import io.minio.Result
import io.minio.errors.MinioException
import io.minio.messages.Item
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.equal
import timber.log.Timber

class SyncManager(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val taskBox: Box<Task> = ObjectBox.boxStore.boxFor()
        val task: Task? = taskBox.query().equal(Task_.taskId, id.toString()).build().findFirst()

        if (task != null) {
            Timber.i("Background id ${task.taskId} from source ${task.source} to destination ${task.destination} successfull")

            // Retrieve user's credentials (who created the task)
            val user: User = Builder().user(id = task.userId!!)
            if (user.accessKey != null) {
                val minioClient = Builder().minioClient(user)
                beginSync(minioClient, task.source!!, task.destination!!)
                return Result.success()
            } else {
                Timber.i("Background id $id failed - no user found")
                return Result.failure()
            }
        } else {
            Timber.i("Background id $id failed - no task found")
            return Result.failure()
        }
    }

    private fun beginSync(minioClient: MinioClient, bucketPath: String, destination: String) {
        var bucketObjects: List<io.minio.Result<Item>> = emptyList()
        try {
            bucketObjects = minioClient.listObjects("music", "", false).toList()
            bucketObjects.forEach {
                Download().bucketObject(
                    selectedBucket = "music",
                    selectedBucketPrefix = "",
                    bucketObject = it.get().objectName(),
                    context = applicationContext,
                    minioClient = minioClient,
                    downloadLocation = destination
                )
            }
            Timber.i("Bucket objects $bucketObjects")
        } catch (e: MinioException) {
            Timber.i("Exception occurred ${e}")
        }
    }
}