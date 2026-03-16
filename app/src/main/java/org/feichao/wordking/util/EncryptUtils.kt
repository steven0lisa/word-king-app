package org.feichao.wordking.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 加密存储工具
 * 对应需求文档中的 encryptSave() 和 decryptGet() 函数
 */
object EncryptUtils {

    private const val ENCRYPTED_PREFS_NAME = "word_king_encrypted_prefs"
    private const val MASTER_KEY_ALIAS = "word_king_master_key"

    private var encryptedPrefs: SharedPreferences? = null

    /**
     * 初始化加密SharedPreferences
     */
    fun init(context: Context) {
        if (encryptedPrefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * 加密存储
     */
    fun encryptSave(key: String, value: String) {
        encryptedPrefs?.edit()?.putString(key, value)?.apply()
    }

    /**
     * 解密获取
     */
    fun decryptGet(key: String): String {
        return encryptedPrefs?.getString(key, "") ?: ""
    }

    /**
     * 移除指定键
     */
    fun remove(key: String) {
        encryptedPrefs?.edit()?.remove(key)?.apply()
    }

    /**
     * 清除所有加密数据
     */
    fun clear() {
        encryptedPrefs?.edit()?.clear()?.apply()
    }

    /**
     * 打码显示敏感信息
     * 对应需求文档中的 maskSensitive() 函数
     */
    fun maskSensitive(value: String, type: String): String {
        if (value.isEmpty()) return ""

        return when (type) {
            "SSH_KEY" -> {
                if (value.length <= 8) value
                else "${value.take(4)}****${value.takeLast(4)}"
            }
            "AI_API_KEY" -> {
                if (value.length <= 12) value
                else "${value.take(6)}****${value.takeLast(6)}"
            }
            else -> value
        }
    }
}
