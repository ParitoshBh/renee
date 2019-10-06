package com.yopers.renee.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.yopers.renee.R
import com.yopers.renee.fragments.BucketListFragment
import io.minio.MinioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class Upload {
    private val notification = Notification()

    fun bucketObject(uri: Uri, context: Context, coroutineScope: CoroutineScope,
                     minioClient: MinioClient, selectedBucket: String, selectedBucketPrefix: String,
                     fragment: BucketListFragment) {
        notification.createChannel(
            context,
            false,
            context.getString(R.string.app_name),
            "App notification channel"
        )

        val pickedFile = DocumentFile.fromSingleUri(context, uri)
        val inputStream: InputStream = context.contentResolver!!.openInputStream(uri)!!

        notification.create(
            context,
            "${context.packageName}-${context.getString(R.string.app_name)}",
            "Uploading selected file",
            pickedFile!!.name!!
        )

        coroutineScope.launch(Dispatchers.Main) {
            initiateUpload(pickedFile, inputStream, minioClient, selectedBucket, selectedBucketPrefix)
            notification.update("Successfully uploaded file")
            fragment.loadBucketObjects(selectedBucketPrefix)
        }
    }

    private suspend fun initiateUpload(pickedFile: DocumentFile, inputStream: InputStream,
                                       minioClient: MinioClient, selectedBucket: String,
                                       selectedBucketPrefix: String) {
        return withContext(Dispatchers.IO) {
            minioClient.putObject(
                selectedBucket,
                "${selectedBucketPrefix}/${pickedFile.name}",
                inputStream,
                "application/octet-stream"
            )
        }
    }
}