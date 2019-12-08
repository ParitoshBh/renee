package com.yopers.renee.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yopers.renee.R

class Notification {
    private lateinit var builder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private var progressMax = 0
    private var progressCompleted = 0

    fun createChannel(context: Context, showBadge: Boolean, name: String,
                      description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelId = "${context.packageName}-$name"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = description
            channel.setShowBadge(showBadge)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun create(context: Context, channelId: String, title: String, text: String, progressMax: Int) {
        this.progressMax = progressMax
        notificationManager = NotificationManagerCompat.from(context)
        builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(this.progressMax, 0, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1001, builder.build())
    }

    fun update(title: String) {
        this.progressCompleted++
        builder
            .setContentTitle(title)
            .setProgress(this.progressMax, this.progressCompleted, false)

        notificationManager.notify(1001, builder.build())
    }
}