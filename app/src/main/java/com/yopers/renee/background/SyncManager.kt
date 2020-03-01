package com.yopers.renee.background

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
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
import java.io.File
import java.lang.NumberFormatException

class SyncManager(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val taskBox: Box<Task> = ObjectBox.boxStore.boxFor()
        val task: Task? = taskBox.query().equal(Task_.taskId, id.toString()).build().findFirst()

        Bugsnag.leaveBreadcrumb("Called background sync task")

        if (task != null) {
            Bugsnag.leaveBreadcrumb("Background id ${task.taskId} from source ${task.source} to destination ${task.destination} successful")
            Timber.i("Background id ${task.taskId} from source ${task.source} to destination ${task.destination} successful")

            // Retrieve user's credentials (who created the task)
            val user: User = Builder().user(id = task.userId!!)
            if (user.accessKey != null) {
                val minioClient = Builder().minioClient(user)
                beginSync(minioClient, task.source!!, task.destination!!)
                Bugsnag.notify(Error("Finished beginSync function call"))
                return Result.success()
            } else {
                Timber.i("Background id $id failed - no user found")
                Bugsnag.notify(Error("Background id $id failed - no user found"))
                return Result.failure()
            }
        } else {
            Timber.i("Background id $id failed - no task found")
            Bugsnag.notify(Error("Background id $id failed - no task found"))
            return Result.failure()
        }
    }

    private fun beginSync(minioClient: MinioClient, bucketPath: String, destination: String) {
        Bugsnag.leaveBreadcrumb("beginSync function")
        var bucketObjects: List<io.minio.Result<Item>> = emptyList()
        var bucketMap : HashMap<String, Int> = HashMap<String, Int>()

        try {
            Bugsnag.leaveBreadcrumb("beginSync function try/catch")
            bucketObjects = minioClient.listObjects("music", "", false).toList()
            Bugsnag.leaveBreadcrumb(bucketObjects.toString())
            bucketObjects.forEach {
                Download().bucketObject(
                    selectedBucket = "music",
                    selectedBucketPrefix = "",
                    bucketObject = it.get().objectName(),
                    context = applicationContext,
                    minioClient = minioClient,
                    downloadLocation = destination
                )

                // Build bucket map
                bucketMap.set(it.get().objectName(), it.hashCode())
            }

            // Remove objects not in source
            syncDestination(bucketMap, destination)

            Bugsnag.leaveBreadcrumb("Bucket objects $bucketObjects")
            Timber.i("Bucket objects $bucketObjects")
        } catch (e: MinioException) {
            Bugsnag.notify(e)
            Timber.i("Exception occurred ${e}")
        }
    }

    private fun syncDestination(bucketMap: HashMap<String, Int>, destination: String) {
        Timber.i("Check destination path for any deletions")
        Bugsnag.leaveBreadcrumb("Check destination path for any deletions")
        val pickedDir = DocumentFile.fromTreeUri(applicationContext, destination.toUri())

        pickedDir?.listFiles()?.forEach {
            if (bucketMap.get(it.name) == null) {
                // Delete file - removed from source
                Timber.i("Delete file '${it.name}'. File status - ${bucketMap.get(it.name)}")

                // Attempt file deletion
                if (it.delete()) {
                    Timber.i("Deleted file")
                    Bugsnag.leaveBreadcrumb("Deleted file ${it.name}")
                } else {
                    Bugsnag.leaveBreadcrumb("Unable to delete file ${it.name}")
                    Timber.i("Unable to delete file")
                }
            }
        }
        Bugsnag.leaveBreadcrumb("Finished cleaning up desitnation path")
    }
}