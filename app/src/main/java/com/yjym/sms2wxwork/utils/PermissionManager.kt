package com.yjym.sms2wxwork.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 统一处理所有权限相关的逻辑
 */
object PermissionManager {
    
    // 基础必需权限
    private val BASE_PERMISSIONS = listOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.INTERNET
    )
    
    // Android 13+ 通知权限
    private val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null
    
    // 电池优化权限
    private val BATTERY_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    } else null
    
    // 获取所有需要的权限
    fun getAllRequiredPermissions(): List<String> {
        return BASE_PERMISSIONS + listOfNotNull(
            NOTIFICATION_PERMISSION,
            BATTERY_PERMISSION
        )
    }
    
    // 检查单个权限
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
               PackageManager.PERMISSION_GRANTED
    }
    
    // 检查所有权限
    fun hasAllPermissions(context: Context): Boolean {
        return getAllRequiredPermissions().all { permission ->
            hasPermission(context, permission)
        }
    }
    
    // 获取缺失的权限
    fun getMissingPermissions(context: Context): List<String> {
        return getAllRequiredPermissions().filter { permission ->
            !hasPermission(context, permission)
        }
    }
    
    // 获取权限描述
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.RECEIVE_SMS -> "接收短信"
            Manifest.permission.READ_SMS -> "读取短信"
            Manifest.permission.READ_PHONE_STATE -> "读取设备状态"
            Manifest.permission.INTERNET -> "访问网络"
            Manifest.permission.POST_NOTIFICATIONS -> "显示通知"
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> "忽略电池优化"
            else -> "未知权限"
        }
    }

    // 获取权限状态映射
    fun getPermissionStatusMap(context: Context): Map<String, Boolean> {
        return getAllRequiredPermissions().associateWith { permission ->
            hasPermission(context, permission)
        }
    }
    
    // 获取权限状态文本（供UI使用）
    fun getPermissionStatusText(context: Context): String {
        val permissions = getAllRequiredPermissions()
        val permissionStatus = permissions.map { permission ->
            val granted = hasPermission(context, permission)
            "${getPermissionDescription(permission)}: ${if (granted) "已授权" else "未授权"}"
        }.joinToString("\n")
        return "权限状态：\n$permissionStatus"
    }
}