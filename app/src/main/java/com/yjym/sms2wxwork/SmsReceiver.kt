package com.yjym.sms2wxwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.yjym.sms2wxwork.sms.SmsProcessor

/**
 * 短信广播接收器
 * 职责：接收系统短信广播，委托给SmsProcessor处理
 */
class SmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到短信广播")
        
        try {
            val bundle: Bundle? = intent.extras
            val pdus = if (bundle != null) {
                @Suppress("DEPRECATION")
                bundle.get("pdus") as? Array<*>
            } else null
            val format = bundle?.getString("format")
            
            Log.d(TAG, "收到 ${pdus?.size ?: 0} 条短信")
            
            if (pdus != null) {
                val smsMessages = pdus.map { pdu ->
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                }
                
                // 使用新的SmsProcessor处理短信
                val processor = SmsProcessor(context)
                processor.processMultipleSms(smsMessages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理短信广播时出错", e)
        }
    }
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
}
