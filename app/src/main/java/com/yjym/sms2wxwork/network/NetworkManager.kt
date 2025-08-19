package com.yjym.sms2wxwork.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.yjym.sms2wxwork.WechatWorkNotifier
import kotlinx.coroutines.delay
import java.net.InetAddress

/**
 * 网络管理类
 * 统一处理网络状态检查和重试机制
 */
object NetworkManager {
    
    private const val TAG = "NetworkManager"
    private const val MAX_RETRY_COUNT = 3
    private const val INITIAL_RETRY_DELAY = 1000L // 1秒
    private const val MAX_RETRY_DELAY = 5000L // 5秒
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }
    
    /**
     * 检查特定域名是否可达
     */
    suspend fun isHostReachable(host: String, timeout: Int = 5000): Boolean {
        return try {
            val inetAddress = InetAddress.getByName(host)
            inetAddress.isReachable(timeout)
        } catch (e: Exception) {
            Log.e(TAG, "检查主机可达性失败: ${e.message}")
            false
        }
    }
    
    /**
     * 检查企业微信API是否可达
     */
    suspend fun isWechatApiReachable(): Boolean {
        return isHostReachable("qyapi.weixin.qq.com")
    }
    
    /**
     * 执行带重试的网络操作
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = MAX_RETRY_COUNT,
        initialDelay: Long = INITIAL_RETRY_DELAY,
        maxDelay: Long = MAX_RETRY_DELAY,
        operation: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        var lastException: Exception? = null
        
        for (attempt in 0..maxRetries) {
            try {
                val result = operation()
                if (attempt > 0) {
                    Log.d(TAG, "网络操作成功，重试次数: $attempt")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                
                if (attempt == maxRetries) {
                    Log.e(TAG, "网络操作失败，已达到最大重试次数: $maxRetries")
                    return Result.failure(lastException ?: Exception("网络操作失败，已达到最大重试次数: $maxRetries"))
                }
                
                Log.w(TAG, "网络操作失败，准备重试: ${e.message}")
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
            }
        }
        
        return Result.failure(lastException ?: Exception("未知网络错误"))
    }
    
    /**
     * 获取网络类型
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "NONE"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "NONE"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.typeName ?: "NONE"
        }
    }
    
    /**
     * 网络状态监听包装器
     */
    data class NetworkStatus(
        val isAvailable: Boolean,
        val networkType: String,
        val isWechatReachable: Boolean
    )
    
    /**
     * 获取完整网络状态
     */
    suspend fun getNetworkStatus(context: Context): NetworkStatus {
        return NetworkStatus(
            isAvailable = isNetworkAvailable(context),
            networkType = getNetworkType(context),
            isWechatReachable = isWechatApiReachable()
        )
    }

    /**
     * 发送企业微信消息（带重试机制）
     */
    suspend fun sendWithRetry(
        context: Context,
        webhookUrl: String,
        sender: String,
        content: String
    ): Boolean {
        return try {
            val success = executeWithRetry {
                WechatWorkNotifier.sendMessage(context, webhookUrl, sender, content)
            }
            success.getOrDefault(false)
        } catch (e: Exception) {
            Log.e(TAG, "发送消息失败: ${e.message}")
            false
        }
    }
}