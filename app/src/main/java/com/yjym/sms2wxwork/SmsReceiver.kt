package com.yjym.sms2wxwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "收到短信广播")
        
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        val onlyVerificationCodes = prefs.getBoolean("only_verification_codes", false)
        val webhookUrl = prefs.getString("webhook_url", "") ?: ""
        
        Log.d("SmsReceiver", "启用状态: $enabled, 仅验证码: $onlyVerificationCodes, Webhook地址: $webhookUrl")
        
        if(!enabled || webhookUrl.isEmpty()) {
            Log.d("SmsReceiver", "推送未启用或Webhook地址为空")
            return
        }

        try {
            val bundle: Bundle? = intent.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            val format = bundle?.getString("format")
            
            Log.d("SmsReceiver", "收到 ${pdus?.size ?: 0} 条短信")
            
            if(pdus != null){
                for(pdu in pdus){
                    val msg = SmsMessage.createFromPdu(pdu as ByteArray, format)
                    val sender = msg.displayOriginatingAddress ?: msg.originatingAddress ?: "未知号码"
                    val body = msg.messageBody ?: ""
                    
                    Log.d("SmsReceiver", "收到短信: 来自 $sender, 内容: $body")
                    
                    // 如果启用了仅验证码模式，检查短信内容是否包含验证码关键词
                    if (onlyVerificationCodes && !isVerificationCode(body)) {
                        Log.d("SmsReceiver", "跳过非验证码短信: $body")
                        continue
                    }
                    
                    // 使用协程在后台线程中发送HTTP请求
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val success = WechatWorkNotifier.sendMessage(context, webhookUrl, sender, body)
                            if (success) {
                                Log.d("SmsReceiver", "短信内容已成功推送到企业微信")
                            } else {
                                Log.e("SmsReceiver", "推送消息到企业微信失败")
                            }
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "推送消息时发生异常", e)
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "处理短信时出错", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 检查短信内容是否包含验证码相关关键词
     */
    private fun isVerificationCode(content: String): Boolean {
        if (content.isEmpty()) return false
        
        val keywords = listOf(
            "验证码",
            "校验码",
            "验证码为",
            "动态码",
            "密码",
            "动态密码",
            "一次性密码",
            "OTP",
            "code",
            "verification",
            "auth",
            "登录码",
            "确认码",
            "激活码",
            "注册码",
            "安全码",
            "身份验证码",
            "短信验证码",
            "手机验证码",
            "动态验证码"
        )
        
        val lowerContent = content.lowercase()
        return keywords.any { keyword ->
            lowerContent.contains(keyword.lowercase())
        }
    }

    companion object {
        init {
            // 静态初始化，避免重复注册
            Log.d("SmsReceiver", "SmsReceiver已初始化")
        }
    }
}
