package com.yopers.renee

import android.app.Application
import com.bugsnag.android.Bugsnag
import io.paperdb.Paper
import timber.log.Timber

class CustomApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        Paper.init(this)
//        Bugsnag.init(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}