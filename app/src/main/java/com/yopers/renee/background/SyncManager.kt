package com.yopers.renee.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

class SyncManager(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Timber.i("Background task ran at ${System.currentTimeMillis()}")
        return Result.success()
    }
}