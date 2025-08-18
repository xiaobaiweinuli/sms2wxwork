package com.yjym.sms2wxwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "收到启动广播: ${intent.action}")
        
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        
        if (enabled) {
            Log.d("BootReceiver", "开机启动短信转发服务")
            val serviceIntent = Intent(context, SmsForwardService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
