package com.shop.billing.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object UpdateNotificationManager {
    private const val CHANNEL_ID = "billing_update_channel"
    private const val NOTIFICATION_ID = 1002
    const val EXTRA_OPEN_SETTINGS = "open_settings"
    const val EXTRA_AUTO_DOWNLOAD = "auto_download"
    const val EXTRA_DOWNLOAD_URL = "download_url"
    const val EXTRA_VERSION_NAME = "version_name"

    fun showUpdateNotification(context: Context, appVersion: AppVersion) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createChannel(context)

        val openIntent = Intent(context, com.shop.billing.MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_SETTINGS, true)
            putExtra(EXTRA_AUTO_DOWNLOAD, true)
            putExtra(EXTRA_DOWNLOAD_URL, appVersion.downloadUrl)
            putExtra(EXTRA_VERSION_NAME, appVersion.versionName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Update Available")
            .setContentText("v${appVersion.versionName.removePrefix("v")} is ready to download")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelUpdateNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about new app updates"
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
