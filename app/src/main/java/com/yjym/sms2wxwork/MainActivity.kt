package com.yjym.sms2wxwork

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.os.Build
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yjym.sms2wxwork.data.ConfigManager
import com.yjym.sms2wxwork.utils.PermissionManager
import com.yjym.sms2wxwork.WechatWorkNotifier

class MainActivity : AppCompatActivity() {
    private lateinit var configManager: ConfigManager

    private val PERMISSION_REQUEST_CODE = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "应用启动")
        
        configManager = ConfigManager.getInstance(this)
        
        val numberInput = findViewById<TextInputEditText>(R.id.editNumber)
        val saveButton = findViewById<MaterialButton>(R.id.btnSave)
        val switchEnable = findViewById<SwitchCompat>(R.id.switchEnable)
        val switchOnlyVerification = findViewById<SwitchCompat>(R.id.switchOnlyVerification)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPermissions = findViewById<TextView>(R.id.tvPermissions)
        val btnCheckPermissions = findViewById<MaterialButton>(R.id.btnCheckPermissions)
        val ivExpandToggle = findViewById<ImageView>(R.id.ivExpandToggle)
        val layoutInstructions = findViewById<LinearLayout>(R.id.layoutInstructions)
        val ivConfigExpandToggle = findViewById<ImageView>(R.id.ivConfigExpandToggle)
        val layoutConfigContent = findViewById<LinearLayout>(R.id.layoutConfigContent)
        val ivSettingsExpandToggle = findViewById<ImageView>(R.id.ivSettingsExpandToggle)
        val layoutSettingsContent = findViewById<LinearLayout>(R.id.layoutSettingsContent)
        
        updateUI(numberInput, switchEnable, switchOnlyVerification, tvStatus, tvPermissions)
        
        saveButton.setOnClickListener {
            val webhookUrl = numberInput.text.toString().trim()
            if (webhookUrl.isEmpty()) {
                Toast.makeText(this, "请输入企业微信Webhook地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!WechatWorkNotifier.isValidWebhookUrl(webhookUrl)) {
                Toast.makeText(this, "请输入有效的企业微信机器人Webhook地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            configManager.webhookUrl = webhookUrl
            Log.d("MainActivity", "保存Webhook地址: $webhookUrl")
            Toast.makeText(this, "已保存Webhook地址", Toast.LENGTH_SHORT).show()
            updateUI(numberInput, switchEnable, switchOnlyVerification, tvStatus, tvPermissions)
        }
        
        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            Log.d("MainActivity", "切换开关状态: $isChecked")
            if (isChecked && !PermissionManager.hasAllPermissions(this)) {
                switchEnable.isChecked = false
                requestAllPermissions()
                return@setOnCheckedChangeListener
            }
            
            configManager.isEnabled = isChecked
            val svc = Intent(this, SmsForwardService::class.java)
            
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc)
                Log.d("MainActivity", "启动前台服务")
            } else {
                startService(svc)
                Log.d("MainActivity", "启动服务")
            }
            Toast.makeText(this, "短信推送已启用", Toast.LENGTH_SHORT).show()
        } else {
            stopService(svc)
            Log.d("MainActivity", "停止服务")
            Toast.makeText(this, "短信推送已停止", Toast.LENGTH_SHORT).show()
            }
            updateUI(numberInput, switchEnable, switchOnlyVerification, tvStatus, tvPermissions)
        }
        
        switchOnlyVerification.setOnCheckedChangeListener { _, isChecked ->
            Log.d("MainActivity", "切换仅验证码开关状态: $isChecked")
            configManager.onlyVerificationCodes = isChecked
            updateUI(numberInput, switchEnable, switchOnlyVerification, tvStatus, tvPermissions)
        }
        
        btnCheckPermissions.setOnClickListener {
            Log.d("MainActivity", "检查权限按钮点击")
            if (!PermissionManager.hasAllPermissions(this)) {
                requestAllPermissions()
            } else {
                Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
            }
        }

        // 企业微信配置折叠展开功能
        var isConfigExpanded = false
        val configClickListener = View.OnClickListener {
            // 检查点击的是否是卡片内部的交互元素
            if (it !is TextInputEditText && it !is com.google.android.material.button.MaterialButton) {
                isConfigExpanded = !isConfigExpanded
                if (isConfigExpanded) {
                    layoutConfigContent.visibility = View.VISIBLE
                    ivConfigExpandToggle.setImageResource(R.drawable.ic_expand_less)
                } else {
                    layoutConfigContent.visibility = View.GONE
                    ivConfigExpandToggle.setImageResource(R.drawable.ic_expand_more)
                }
            }
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardConfig).setOnClickListener(configClickListener)
        ivConfigExpandToggle.setOnClickListener(configClickListener)

        // 功能设置折叠展开功能
        var isSettingsExpanded = false
        val settingsClickListener = View.OnClickListener {
            // 检查点击的是否是卡片内部的交互元素
            if (it !is androidx.appcompat.widget.SwitchCompat) {
                isSettingsExpanded = !isSettingsExpanded
                if (isSettingsExpanded) {
                    layoutSettingsContent.visibility = View.VISIBLE
                    ivSettingsExpandToggle.setImageResource(R.drawable.ic_expand_less)
                } else {
                    layoutSettingsContent.visibility = View.GONE
                    ivSettingsExpandToggle.setImageResource(R.drawable.ic_expand_more)
                }
            }
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSettings).setOnClickListener(settingsClickListener)
        ivSettingsExpandToggle.setOnClickListener(settingsClickListener)

        // 使用说明折叠展开功能
        var isInstructionsExpanded = false
        val instructionsClickListener = View.OnClickListener {
            // 使用说明卡片内没有交互元素，直接处理点击
            isInstructionsExpanded = !isInstructionsExpanded
            if (isInstructionsExpanded) {
                layoutInstructions.visibility = View.VISIBLE
                ivExpandToggle.setImageResource(R.drawable.ic_expand_less)
            } else {
                layoutInstructions.visibility = View.GONE
                ivExpandToggle.setImageResource(R.drawable.ic_expand_more)
            }
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardInstructions).setOnClickListener(instructionsClickListener)
        ivExpandToggle.setOnClickListener(instructionsClickListener)
        
        if (!PermissionManager.hasAllPermissions(this)) {
            Log.d("MainActivity", "缺少权限，开始请求")
            requestAllPermissions()
        }
    }
    
    override fun onResume() {
        super.onResume()
        val numberInput = findViewById<TextInputEditText>(R.id.editNumber)
        val switchEnable = findViewById<SwitchCompat>(R.id.switchEnable)
        val switchOnlyVerification = findViewById<SwitchCompat>(R.id.switchOnlyVerification)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPermissions = findViewById<TextView>(R.id.tvPermissions)
        updateUI(numberInput, switchEnable, switchOnlyVerification, tvStatus, tvPermissions)
    }

    private fun updateUI(
        numberInput: TextInputEditText,
        switchEnable: SwitchCompat,
        switchOnlyVerification: SwitchCompat,
        tvStatus: TextView,
        tvPermissions: TextView
    ) {
        numberInput.setText(configManager.webhookUrl)
        switchEnable.isChecked = configManager.isEnabled
        switchOnlyVerification.isChecked = configManager.onlyVerificationCodes
        
        val webhookUrl = configManager.webhookUrl
        val enabled = configManager.isEnabled
        val onlyVerification = configManager.onlyVerificationCodes
        
        if (!enabled) {
            tvStatus.text = "状态：已禁用"
            tvStatus.setTextColor(Color.RED)
        } else if (webhookUrl.isEmpty()) {
            tvStatus.text = "状态：未配置Webhook地址"
            tvStatus.setTextColor(Color.RED)
        } else {
            tvStatus.text = "状态：已启用"
            tvStatus.setTextColor(Color.GREEN)
        }
        
        val permissions = listOf(
            Manifest.permission.RECEIVE_SMS to "接收短信",
            Manifest.permission.READ_SMS to "读取短信"
        )
        
        val permissionStatus = permissions.map { (permission, name) ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            "$name: ${if (granted) "已授权" else "未授权"}"
        }.joinToString("\n")
        
        tvPermissions.text = "权限状态：\n$permissionStatus"
    }

    private fun requestAllPermissions() {
        val missingPermissions = PermissionManager.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.zip(grantResults.toTypedArray())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }
            
            if (deniedPermissions.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("权限请求")
                    .setMessage("需要短信权限才能正常转发短信。请在设置中授予权限后重试。")
                    .setPositiveButton("确定") { _, _ -> }
                    .setNegativeButton("前往设置") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .show()
            } else {
                Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
