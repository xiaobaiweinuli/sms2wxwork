package com.yjym.sms2wxwork.utils

/**
 * 验证码检测工具类
 * 专门处理验证码识别逻辑，从SmsReceiver中分离出来
 */
object VerificationCodeDetector {
    
    // 验证码关键词库
    private val VERIFICATION_KEYWORDS = listOf(
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
        "动态验证码",
        "verification code",
        "auth code",
        "security code",
        "confirmation code",
        "PIN码",
        "PIN",
        "口令",
        "令牌",
        "token",
        "授权码",
        "校验密码",
        "登录密码",
        "临时密码",
        "有效期",
        "分钟内有效",
        "有效时间",
        "过期时间"
    )
    
    // 数字模式匹配
    private val NUMBER_PATTERNS = listOf(
        Regex("\\d{4,8}"),  // 4-8位数字
        Regex("\\d{3}-\\d{3}"),  // 123-456格式
        Regex("\\d{4}\\s?\\d{4}"),  // 1234 5678格式
        Regex("#[0-9]{4,6}"),  // #1234格式
        Regex("[0-9]{3,4}-[0-9]{3,4}")  // 123-4567格式
    )
    
    // 短信来源白名单（常见的验证码发送方）
    private val TRUSTED_SENDERS = setOf(
        "106",  // 国内服务号前缀
        "10086",  // 中国移动
        "10010",  // 中国联通
        "10000",  // 中国电信
        "95555",  // 招商银行
        "95588",  // 工商银行
        "95533",  // 建设银行
        "95599",  // 农业银行
        "95017",  // 腾讯
        "1069",  // 三网合一短信
        "12520",  // 飞信
        "12306",  // 铁路
        "95516",  // 银联
        "1065"   // 国际短信
    )
    
    /**
     * 检测短信是否包含验证码
     */
    fun containsVerificationCode(content: String, sender: String = ""): Boolean {
        if (content.isEmpty()) return false
        
        val lowerContent = content.lowercase()
        val lowerSender = sender.lowercase()
        
        // 1. 关键词检测
        val hasKeyword = VERIFICATION_KEYWORDS.any { keyword ->
            lowerContent.contains(keyword.lowercase())
        }
        
        // 2. 数字模式检测
        val hasNumberPattern = NUMBER_PATTERNS.any { pattern ->
            pattern.containsMatchIn(content)
        }
        
        // 3. 来源可信度加分
        val isTrustedSender = TRUSTED_SENDERS.any { trusted ->
            lowerSender.contains(trusted.lowercase())
        }
        
        // 4. 综合判断
        return when {
            // 有验证码关键词 + 数字模式 = 高置信度
            hasKeyword && hasNumberPattern -> true
            
            // 有验证码关键词 + 可信发送方 = 中置信度
            hasKeyword && isTrustedSender -> true
            
            // 只有数字模式但来自可信发送方 = 低置信度
            hasNumberPattern && isTrustedSender -> true
            
            // 其他情况
            else -> false
        }
    }
    
    /**
     * 提取可能的验证码
     */
    fun extractVerificationCode(content: String): String? {
        if (content.isEmpty()) return null
        
        // 尝试各种数字模式
        for (pattern in NUMBER_PATTERNS) {
            val match = pattern.find(content)
            if (match != null) {
                return match.value.replace("[^0-9]".toRegex(), "")
            }
        }
        
        // 尝试提取4-8位连续数字
        val numbers = "\\d{4,8}".toRegex().findAll(content)
            .map { it.value }
            .toList()
        
        return numbers.firstOrNull()
    }
    
    /**
     * 获取验证码检测置信度
     */
    fun getVerificationConfidence(content: String, sender: String = ""): Int {
        var confidence = 0
        val lowerContent = content.lowercase()
        val lowerSender = sender.lowercase()
        
        // 关键词匹配加分
        VERIFICATION_KEYWORDS.forEach { keyword ->
            if (lowerContent.contains(keyword.lowercase())) {
                confidence += 20
            }
        }
        
        // 数字模式加分
        if (NUMBER_PATTERNS.any { it.containsMatchIn(content) }) {
            confidence += 15
        }
        
        // 可信发送方加分
        if (TRUSTED_SENDERS.any { lowerSender.contains(it.lowercase()) }) {
            confidence += 10
        }
        
        // 内容长度合理加分
        if (content.length in 20..200) {
            confidence += 5
        }
        
        // 上限100分
        return minOf(confidence, 100)
    }
    
    /**
     * 检测是否为验证码短信（带详细分析）
     */
    fun analyzeMessage(content: String, sender: String = ""): VerificationAnalysis {
        return VerificationAnalysis(
            isVerification = containsVerificationCode(content, sender),
            confidence = getVerificationConfidence(content, sender),
            extractedCode = extractVerificationCode(content),
            keywordsFound = VERIFICATION_KEYWORDS.filter { 
                content.lowercase().contains(it.lowercase()) 
            },
            senderTrustLevel = if (TRUSTED_SENDERS.any { 
                sender.lowercase().contains(it.lowercase()) 
            }) "high" else "normal"
        )
    }
}

/**
 * 验证码分析结果
 */
data class VerificationAnalysis(
    val isVerification: Boolean,
    val confidence: Int,
    val extractedCode: String?,
    val keywordsFound: List<String>,
    val senderTrustLevel: String
)