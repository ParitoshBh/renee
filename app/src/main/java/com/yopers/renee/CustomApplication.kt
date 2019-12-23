package com.yopers.renee

import android.app.Application
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.yopers.renee.models.MyObjectBox
import io.objectbox.BoxStore
import timber.log.Timber

class CustomApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        ObjectBox.init(this)
//        Bugsnag.init(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

object ObjectBox {
    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}