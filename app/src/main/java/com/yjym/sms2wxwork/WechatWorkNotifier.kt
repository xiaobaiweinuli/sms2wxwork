package com.yjym.sms2wxwork

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.yjym.sms2wxwork.network.NetworkManager

/**
 * 企业微信消息通知器
 * 职责：负责将短信内容格式化为企业微信消息并发送
 */
object WechatWorkNotifier {
    private const val TAG = "WechatWorkNotifier"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 发送短信消息到企业微信
     * @param webhookUrl 企业微信机器人Webhook地址
     * @param sender 短信发送方号码
     * @param content 短信内容
     * @return 是否发送成功
     */
    fun sendMessage(context: Context, webhookUrl: String, sender: String, content: String): Boolean {
        if (webhookUrl.isEmpty()) {
            Log.w(TAG, "Webhook地址为空")
            return false
        }
        
        if (!NetworkManager.isNetworkAvailable(context)) {
            Log.w(TAG, "网络不可用")
            return false
        }
        
        val message = buildMessageContent(sender, content)
        return sendToWechatWork(webhookUrl, message)
    }
    
    /**
     * 构建企业微信Markdown消息内容
     */
    private fun buildMessageContent(sender: String, content: String): String {
        val escapedSender = escapeMarkdown(sender)
        val escapedContent = escapeMarkdown(content)
        
        return """
            ## 📱 收到新短信
            
            **发送号码:** `$escapedSender`
            
            **短信内容:**
            ```
            $escapedContent
            ```
            
            **接收时间:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()
    }
    
    /**
     * 转义Markdown特殊字符
     */
    private fun escapeMarkdown(text: String): String {
        return text.replace("`", "\\`")
                  .replace("*", "\\*")
                  .replace("_", "\\_")
                  .replace("#", "\\#")
                  .replace("[", "\\[")
                  .replace("]", "\\]")
                  .replace("(", "\\(")
                  .replace(")", "\\)")
                  .replace("~", "\\~")
                  .replace(">", "\\>")
                  .replace("-", "\\-")
                  .replace("=", "\\=")
                  .replace("|", "\\|")
                  .replace("{", "\\{")
                  .replace("}", "\\}")
                  .replace(".", "\\.")
                  .replace("!", "\\!")
    }
    
    /**
     * 发送消息到企业微信
     */
    private fun sendToWechatWork(webhookUrl: String, message: String): Boolean {
        val json = """
            {
                "msgtype": "markdown",
                "markdown": {
                    "content": "$message"
                }
            }
        """.trimIndent()
        
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (success) {
                    Log.d(TAG, "消息发送成功")
                } else {
                    Log.e(TAG, "消息发送失败: ${response.code} ${response.message}")
                }
                success
            }
        } catch (e: IOException) {
            Log.e(TAG, "网络请求异常", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "发送消息异常", e)
            false
        }
    }
    
    fun isValidWebhookUrl(url: String): Boolean {
        return url.startsWith("https://qyapi.weixin.qq.com/cgi-bin/webhook/send") ||
               url.startsWith("https://work.weixin.qq.com/cgi-bin/webhook/send")
    }
}