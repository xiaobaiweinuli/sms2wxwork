package com.yjym.sms2wxwork.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 统一配置管理类
 * 管理所有配置相关的操作，避免硬编码和重复代码
 */
class ConfigManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ConfigManager? = null
        
        fun getInstance(context: Context): ConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // 配置键名常量
        private const val PREFS_NAME = "sms_forward_config"
        const val KEY_WEBHOOK_URL = "webhook_url"
        const val KEY_ENABLED = "enabled"
        const val KEY_ONLY_VERIFICATION_CODES = "only_verification_codes"
        const val KEY_LAST_FORWARD_TIME = "last_forward_time"
        const val KEY_TOTAL_FORWARD_COUNT = "total_forward_count"
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Webhook URL
    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WEBHOOK_URL, value) }
    
    // 服务启用状态
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }
    
    // 仅转发验证码
    var onlyVerificationCodes: Boolean
        get() = prefs.getBoolean(KEY_ONLY_VERIFICATION_CODES, false)
        set(value) = prefs.edit { putBoolean(KEY_ONLY_VERIFICATION_CODES, value) }
    
    // 最后转发时间
    var lastForwardTime: Long
        get() = prefs.getLong(KEY_LAST_FORWARD_TIME, 0)
        set(value) = prefs.edit { putLong(KEY_LAST_FORWARD_TIME, value) }
    
    // 总转发数量
    var totalForwardCount: Int
        get() = prefs.getInt(KEY_TOTAL_FORWARD_COUNT, 0)
        set(value) = prefs.edit { putInt(KEY_TOTAL_FORWARD_COUNT, value) }
    
    // 增加转发计数
    fun incrementForwardCount() {
        totalForwardCount = totalForwardCount + 1
        lastForwardTime = System.currentTimeMillis()
    }
    
    // 检查配置是否完整
    fun isConfigComplete(): Boolean {
        return webhookUrl.isNotEmpty() && isEnabled
    }
    
    // 清除所有配置
    fun clearAll() {
        prefs.edit { clear() }
    }
    
    // 导出配置
    fun exportConfig(): Map<String, Any> {
        return mapOf(
            KEY_WEBHOOK_URL to webhookUrl,
            KEY_ENABLED to isEnabled,
            KEY_ONLY_VERIFICATION_CODES to onlyVerificationCodes,
            KEY_TOTAL_FORWARD_COUNT to totalForwardCount
        )
    }
    
    // 导入配置
    fun importConfig(config: Map<String, Any>) {
        prefs.edit {
            config[KEY_WEBHOOK_URL]?.let { putString(KEY_WEBHOOK_URL, it.toString()) }
            config[KEY_ENABLED]?.let { putBoolean(KEY_ENABLED, it as Boolean) }
            config[KEY_ONLY_VERIFICATION_CODES]?.let { putBoolean(KEY_ONLY_VERIFICATION_CODES, it as Boolean) }
        }
    }
}