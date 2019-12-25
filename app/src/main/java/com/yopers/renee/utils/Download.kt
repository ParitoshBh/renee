package com.yopers.renee.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.snackbar.Snackbar
import com.yopers.renee.R
import io.minio.MinioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.InputStream
import java.lang.Exception

class Download {
    fun bucketObject(selectedBucket: String, selectedBucketPrefix: String, bucketObject: String,
                     context: Context, coroutineScope: CoroutineScope, minioClient: MinioClient,
                     activity: Activity, downloadLocation: String, notification: Notification) {
        Timber.i("Storage write permissions ${Permission().isAvailable(context)}")
        if (!Permission().isAvailable(activity)) {
            Snackbar.make(
                activity.findViewById(R.id.root_layout),
                "Missing storage write permissions",
                Snackbar.LENGTH_LONG
            ).show()
            Permission().request(activity)
        } else {
            Timber.i("Started downloading object ${bucketObject} from ${selectedBucket} bucket")
            coroutineScope.launch(Dispatchers.Main) {
                val isSuccessful = initiateBackgroundTransfer(
                    downloadLocation.toUri(),
                    context,
                    minioClient,
                    selectedBucket,
                    selectedBucketPrefix,
                    bucketObject
                )

                if (isSuccessful) {
                    notification.update("Downloaded ${bucketObject}")
                } else {
                    notification.update("Failed to download ${bucketObject}")
                }
            }
        }
    }

    fun bucketObject(selectedBucket: String, selectedBucketPrefix: String, bucketObject: String,
                     context: Context, minioClient: MinioClient, downloadLocation: String
//                     ,notification: Notification
    ) {
        Timber.i("Storage write permissions ${Permission().isAvailable(context)}")
        if (!Permission().isAvailable(context)) {
//            Snackbar.make(
//                activity.findViewById(R.id.root_layout),
//                "Missing storage write permissions",
//                Snackbar.LENGTH_LONG
//            ).show()
//            Permission().request(activity)
            Timber.i("Missing storage write permissions")
        } else {
            Timber.i("Started downloading object ${bucketObject} from ${selectedBucket} bucket")
            val isSuccessful = initiateTransfer(
                downloadLocation.toUri(),
                context,
                minioClient,
                selectedBucket,
                selectedBucketPrefix,
                bucketObject
            )

            if (isSuccessful) {
                Timber.i("Downloaded ${bucketObject}")
//                notification.update("Downloaded ${bucketObject}")
            } else {
                Timber.i("Failed to download ${bucketObject}")
//                notification.update("Failed to download ${bucketObject}")
            }
        }
    }

    private suspend fun initiateBackgroundTransfer(uri: Uri, context: Context, minioClient: MinioClient,
                                         selectedBucket: String, selectedBucketPrefix: String, bucketObject: String
    ) = withContext(Dispatchers.IO) {
            return@withContext initiateTransfer(
                uri,
                context,
                minioClient,
                selectedBucket,
                selectedBucketPrefix,
                bucketObject
            )
    }

    private fun initiateTransfer(uri: Uri, context: Context, minioClient: MinioClient, selectedBucket: String,
                                 selectedBucketPrefix: String, bucketObject: String
    ): Boolean {
        val pickedDir = DocumentFile.fromTreeUri(context, uri)
        try {
            if (pickedDir!!.findFile(bucketObject) === null) {
                val objectStat = minioClient.statObject(selectedBucket, selectedBucketPrefix + bucketObject)
                Timber.i("Object stat - $objectStat")
                Timber.i("Object content type - ${objectStat.contentType()}")

                val newFile = pickedDir!!.createFile(objectStat.contentType(), bucketObject)
                val inputStream: InputStream = minioClient.getObject(selectedBucket, selectedBucketPrefix + bucketObject)

                val source = inputStream.source().buffer()
                val sink = context.contentResolver.openOutputStream(newFile!!.uri)!!.sink().buffer()

                source.use { input ->
                    sink.use { output ->
                        output.writeAll(input)
                    }
                }
            } else {
                Timber.i("File presence check - Object ${bucketObject} already present. Skipping download")
            }

            return true
        } catch (e: Exception) {
            Timber.i("New File exception ${e}")
            return false
        }
    }
}