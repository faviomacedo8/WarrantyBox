package com.warrantybox.app.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityManager(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val biometricEnabled: Boolean
        get() = preferences.getBoolean(KEY_BIOMETRIC, false)

    val hasPin: Boolean
        get() = preferences.contains(KEY_PIN_HASH) && preferences.contains(KEY_PIN_SALT)

    val lockEnabled: Boolean
        get() = biometricEnabled || hasPin

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setPin(pin: String) {
        require(pin.matches(Regex("\\d{4,6}"))) { "O PIN deve ter entre 4 e 6 algarismos" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        preferences.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = preferences.getString(KEY_PIN_SALT, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        } ?: return false
        val expected = preferences.getString(KEY_PIN_HASH, null) ?: return false
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            hash(pin, salt).toByteArray(Charsets.UTF_8)
        )
    }

    fun clearPin() {
        preferences.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    companion object {
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
    }
}
