package com.yjym.sms2wxwork.sms

import android.content.Context
import android.telephony.SmsMessage
import android.util.Log
import com.yjym.sms2wxwork.data.ConfigManager
import com.yjym.sms2wxwork.network.NetworkManager
import com.yjym.sms2wxwork.utils.VerificationCodeDetector
import com.yjym.sms2wxwork.WechatWorkNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 短信处理器
 * 专门处理短信解析和转发逻辑，从SmsReceiver中分离业务逻辑
 */
class SmsProcessor(private val context: Context) {
    
    private val configManager = ConfigManager.getInstance(context)
    private val networkManager = NetworkManager
    
    companion object {
        private const val TAG = "SmsProcessor"
    }
    
    /**
     * 处理收到的短信
     */
    fun processSmsMessage(smsMessage: SmsMessage) {
        try {
            // 检查服务是否启用
            if (!configManager.isEnabled) {
                Log.d(TAG, "短信转发服务未启用")
                return
            }
            
            // 检查配置是否完整
            if (!configManager.isConfigComplete()) {
                Log.d(TAG, "配置不完整，无法转发")
                return
            }
            
            // 提取短信信息
            val sender = extractSender(smsMessage)
            val content = extractContent(smsMessage)
            
            if (content.isEmpty()) {
                Log.d(TAG, "短信内容为空，跳过")
                return
            }
            
            Log.d(TAG, "收到短信: 来自 $sender, 内容: $content")
            
            // 检查是否需要转发
            if (shouldForwardMessage(content, sender)) {
                forwardMessage(sender, content)
            } else {
                Log.d(TAG, "短信被过滤，不转发")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "处理短信时出错", e)
        }
    }
    
    /**
     * 提取发送方号码
     */
    private fun extractSender(smsMessage: SmsMessage): String {
        return smsMessage.displayOriginatingAddress 
            ?: smsMessage.originatingAddress 
            ?: "未知号码"
    }
    
    /**
     * 提取短信内容
     */
    private fun extractContent(smsMessage: SmsMessage): String {
        return smsMessage.messageBody ?: ""
    }
    
    /**
     * 判断是否应该转发此短信
     */
    private fun shouldForwardMessage(content: String, sender: String): Boolean {
        // 如果开启了仅验证码模式
        if (configManager.onlyVerificationCodes) {
            val analysis = VerificationCodeDetector.analyzeMessage(content, sender)
            
            Log.d(TAG, "验证码检测结果: " +
                "isVerification=${analysis.isVerification}, " +
                "confidence=${analysis.confidence}, " +
                "code=${analysis.extractedCode}")
            
            return analysis.isVerification && analysis.confidence >= 30
        }
        
        // 否则转发所有短信
        return true
    }
    
    /**
     * 转发短信到企业微信
     */
    private fun forwardMessage(sender: String, content: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 检查网络状态
                if (!networkManager.isNetworkAvailable(context)) {
                    Log.e(TAG, "网络不可用，无法转发短信")
                    return@launch
                }
                
                // 使用网络管理器发送消息（包含重试机制）
                val success = networkManager.sendWithRetry(
                    context,
                    configManager.webhookUrl,
                    sender,
                    content
                )
                
                if (success) {
                    configManager.incrementForwardCount()
                    Log.d(TAG, "短信转发成功，总计转发: ${configManager.totalForwardCount} 条")
                } else {
                    Log.e(TAG, "短信转发失败")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "转发短信时发生异常", e)
            }
        }
    }
    
    /**
     * 批量处理多条短信
     */
    fun processMultipleSms(smsMessages: List<SmsMessage>) {
        smsMessages.forEach { smsMessage ->
            processSmsMessage(smsMessage)
        }
    }
    
    /**
     * 获取处理统计信息
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            totalForwarded = configManager.totalForwardCount,
            lastForwardTime = configManager.lastForwardTime,
            isEnabled = configManager.isEnabled,
            configComplete = configManager.isConfigComplete()
        )
    }
    
    /**
     * 测试配置有效性
     */
    suspend fun testConfiguration(): Result<Boolean> {
        return try {
            if (!configManager.isConfigComplete()) {
                return Result.failure(Exception("配置不完整"))
            }
            
            if (!networkManager.isNetworkAvailable(context)) {
                return Result.failure(Exception("网络不可用"))
            }
            
            val success = WechatWorkNotifier.sendMessage(
                context,
                configManager.webhookUrl,
                "测试消息",
                "这是一条测试短信，用于验证配置是否正确"
            )
            
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 处理统计信息
 */
data class ProcessingStats(
    val totalForwarded: Int,
    val lastForwardTime: Long,
    val isEnabled: Boolean,
    val configComplete: Boolean
)