package com.yjym.sms2wxwork

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WechatWorkNotifier {
    companion object {
        private const val TAG = "WechatWorkNotifier"
        
        suspend fun sendMessage(context: Context, webhookUrl: String, sender: String, content: String): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL(webhookUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    
                    connection.apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "application/json")
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 15000
                    }
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentTime = dateFormat.format(Date())
                    
                    val message = """
                        **收到新短信**
                        
                        **发件人：** $sender
                        **内容：** ${content.trim()}
                        **时间：** $currentTime
                        
                        ---
                        来自短信转发器
                    """.trimIndent()
                    
                    val jsonPayload = JSONObject().apply {
                        put("msgtype", "markdown")
                        put("markdown", JSONObject().apply {
                            put("content", message)
                        })
                    }
                    
                    OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                        writer.write(jsonPayload.toString())
                        writer.flush()
                    }
                    
                    val responseCode = connection.responseCode
                    val responseMessage = connection.responseMessage
                    
                    Log.d(TAG, "企业微信推送响应码: $responseCode, 消息: $responseMessage")
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "消息成功推送到企业微信")
                        true
                    } else {
                        Log.e(TAG, "推送失败: HTTP $responseCode - $responseMessage")
                        false
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "推送消息时发生异常", e)
                    false
                }
            }
        }
        
        fun isValidWebhookUrl(url: String): Boolean {
            return url.startsWith("https://qyapi.weixin.qq.com/cgi-bin/webhook/send") ||
                   url.contains("weixin") ||
                   url.contains("qyapi")
        }
    }
}