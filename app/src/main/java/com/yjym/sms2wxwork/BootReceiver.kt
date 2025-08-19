package com.yjym.sms2wxwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.yjym.sms2wxwork.data.ConfigManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "收到启动广播: ${intent.action}")
        
        val enabled = ConfigManager.getInstance(context).isEnabled
        
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
