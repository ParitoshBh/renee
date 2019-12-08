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
        Timber.i("Storage write permissions ${Permission().isAvailable(activity)}")
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
                val isSuccessful = initiateTransfer(
                    downloadLocation.toUri(),
                    context,
                    minioClient,
                    activity,
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

    private suspend fun initiateTransfer(uri: Uri, context: Context, minioClient: MinioClient,
                                         activity: Activity, selectedBucket: String,
                                         selectedBucketPrefix: String, bucketObject: String
    ) = withContext(Dispatchers.IO) {
        val pickedDir = DocumentFile.fromTreeUri(context, uri)
        try {
            if (pickedDir!!.findFile(bucketObject) === null) {
                val newFile = pickedDir!!.createFile("application/gzip", bucketObject)
                val inputStream: InputStream = minioClient.getObject(selectedBucket, selectedBucketPrefix + bucketObject)

                val source = inputStream.source().buffer()
                val sink = activity.contentResolver.openOutputStream(newFile!!.uri)!!.sink().buffer()

                source.use { input ->
                    sink.use { output ->
                        output.writeAll(input)
                    }
                }
            } else {
                Timber.i("File presence check - Object ${bucketObject} already present. Skipping download")
            }

            return@withContext true
        } catch (e: Exception) {
            Timber.i("New File exception ${e}")
            return@withContext false
        }
    }
}