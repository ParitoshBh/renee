package com.yopers.renee

import android.os.AsyncTask
import io.minio.MinioClient
import io.minio.Result
import io.minio.messages.Item

class BackgroundBucketTask(bucket: String, bucketPrefix: String, listener: DataLoadListener): AsyncTask<Any, Any, List<Result<Item>>>()
{
    private var mBucket: String
    private var mListener: DataLoadListener
    private var mBucketPrefix: String

    init {
        this.mBucket = bucket
        this.mListener = listener
        this.mBucketPrefix= bucketPrefix
    }

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun doInBackground(vararg params: Any?): List<Result<Item>>? {
        val minioClient = params[0] as? MinioClient
        val bucketObjects = minioClient?.listObjects(mBucket, mBucketPrefix, false)
        return bucketObjects?.toList()
    }

    override fun onPostExecute(result: List<Result<Item>>?) {
        super.onPostExecute(result)
        this.mListener.onDataLoaded(result, mBucketPrefix, mBucket)
    }

    interface DataLoadListener {
        fun onDataLoaded(results: List<Result<Item>>?, bucketPrefix: String, bucket: String)
    }
}
