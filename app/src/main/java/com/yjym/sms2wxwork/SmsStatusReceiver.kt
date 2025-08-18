package com.yjym.sms2wxwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "SMS_SENT" -> Log.d("SmsStatusReceiver", "短信已发送")
            "SMS_DELIVERED" -> Log.d("SmsStatusReceiver", "短信已送达")
        }
    }
}