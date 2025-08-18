package com.yjym.sms2wxwork
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class ForwardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForwardService", "ForwardService started")
        return START_STICKY
    }
}