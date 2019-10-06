package com.yopers.renee.utils

import com.yopers.renee.fragments.BucketListFragment
import io.minio.MinioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class Bucket {
    fun createDirectory(coroutineScope: CoroutineScope, minioClient: MinioClient,
                        selectedBucket: String, selectedBucketPrefix: String, name: String,
                        fragment: BucketListFragment) {
        coroutineScope.launch(Dispatchers.Main) {
            createDirectoryInBucket(minioClient, selectedBucket, selectedBucketPrefix, name)
            fragment.loadBucketObjects(selectedBucketPrefix)
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