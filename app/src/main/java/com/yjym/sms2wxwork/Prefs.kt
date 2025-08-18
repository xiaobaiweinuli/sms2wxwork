package com.yjym.sms2wxwork

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREFS = "sms_forward_prefs"
    private const val KEY_NUMBER = "forward_number"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ONLY_VERIFICATION = "only_verification_codes"
    
    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    var Context.forwardNumber: String?
        get() = prefs(this).getString(KEY_NUMBER, null)?.trim()?.ifEmpty { null }
        set(value) { prefs(this).edit().putString(KEY_NUMBER, value?.trim()).apply() }
    
    var Context.enabled: Boolean
        get() = prefs(this).getBoolean(KEY_ENABLED, false)
        set(value) { prefs(this).edit().putBoolean(KEY_ENABLED, value).apply() }
        
    var Context.onlyVerificationCodes: Boolean
        get() = prefs(this).getBoolean(KEY_ONLY_VERIFICATION, false)
        set(value) { prefs(this).edit().putBoolean(KEY_ONLY_VERIFICATION, value).apply() }
}