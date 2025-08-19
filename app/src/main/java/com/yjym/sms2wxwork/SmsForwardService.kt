package com.yjym.sms2wxwork

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SmsForwardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(NOTIFICATION_TEXT)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .build()

    companion object {
        private const val CHANNEL_ID = "sms_forward_channel"
        private const val CHANNEL_NAME = "短信推送"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_TITLE = "短信推送服务"
        private const val NOTIFICATION_TEXT = "正在运行中..."
    }
}
