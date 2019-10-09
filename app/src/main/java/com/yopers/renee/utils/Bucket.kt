package com.yopers.renee.utils

import com.google.android.material.snackbar.Snackbar
import com.yopers.renee.MainActivity
import com.yopers.renee.R
import com.yopers.renee.fragments.BucketListFragment
import io.minio.MinioClient
import io.minio.errors.InvalidBucketNameException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class Bucket {
    fun create(coroutineScope: CoroutineScope, minioClient: MinioClient, name: String,
               activity: MainActivity) {
        coroutineScope.launch(Dispatchers.Main) {
            val response: String = createNewBucket(minioClient, name)

            if (response.isEmpty()) {
                Snackbar.make(
                    activity.findViewById(R.id.root_layout),
                    "Created bucket successfully",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Snackbar.make(
                    activity.findViewById(R.id.root_layout),
                    response,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    fun createDirectory(coroutineScope: CoroutineScope, minioClient: MinioClient,
                        selectedBucket: String, selectedBucketPrefix: String, name: String,
                        fragment: BucketListFragment) {
        coroutineScope.launch(Dispatchers.Main) {
            createDirectoryInBucket(minioClient, selectedBucket, selectedBucketPrefix, name)
            fragment.loadBucketObjects(selectedBucketPrefix)
        }
    }

    fun removeObject(coroutineScope: CoroutineScope, minioClient: MinioClient,
                     selectedBucket: String, selectedBucketPrefix: String, name: String,
                     fragment: BucketListFragment) {
        coroutineScope.launch(Dispatchers.Main) {
            removeObjectFromBucket(minioClient, selectedBucket, selectedBucketPrefix, name)
            fragment.loadBucketObjects(selectedBucketPrefix)
        }
    }

    private suspend fun createNewBucket(minioClient: MinioClient, name: String): String {
        return withContext(Dispatchers.IO) {
            try {
                minioClient.makeBucket(name)
                ""
            } catch (i: InvalidBucketNameException) {
                i.message.orEmpty()
            }
        }
    }

    private suspend fun removeObjectFromBucket(minioClient: MinioClient, selectedBucket: String,
                                                selectedBucketPrefix: String, name: String) {
        return withContext(Dispatchers.IO) {
            minioClient.removeObject(
                selectedBucket,
                "${selectedBucketPrefix}/${name}"
            )
        }
    }

    private suspend fun createDirectoryInBucket(minioClient: MinioClient, selectedBucket: String,
                                                selectedBucketPrefix: String, name: String) {
        return withContext(Dispatchers.IO) {
            minioClient.putObject(
                selectedBucket,
                "${selectedBucketPrefix}/${name}/",
                ByteArrayInputStream("".toByteArray()), 0.toLong(),
                "application/octet-stream"
            )
        }
    }
}