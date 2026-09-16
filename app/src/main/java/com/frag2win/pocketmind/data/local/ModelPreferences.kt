package com.frag2win.pocketmind.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "model_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getSelectedVariant(): String? {
        return prefs.getString(KEY_SELECTED_VARIANT, null)
    }

    fun setSelectedVariant(variant: GemmaVariant) {
        prefs.edit().putString(KEY_SELECTED_VARIANT, variant.name).apply()
    }

    fun isAutoSelectEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SELECT, true)
    }

    fun setAutoSelectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SELECT, enabled).apply()
    }

    fun getHfToken(): String? {
        return prefs.getString(KEY_HF_TOKEN, null)
    }

    fun setHfToken(token: String) {
        prefs.edit().putString(KEY_HF_TOKEN, token).apply()
    }

    fun getTavilyApiKey(): String? {
        return prefs.getString(KEY_TAVILY_API_KEY, null)
    }

    fun setTavilyApiKey(key: String) {
        prefs.edit().putString(KEY_TAVILY_API_KEY, key).apply()
    }

    fun getDisplayName(): String {
        return prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User"
    }

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
    }

    fun getUserName(): String = getDisplayName()

    fun setUserName(name: String) = setDisplayName(name)

    companion object {
        private const val KEY_SELECTED_VARIANT = "selected_variant"
        private const val KEY_AUTO_SELECT = "auto_select"
        private const val KEY_HF_TOKEN = "hf_token"
        private const val KEY_TAVILY_API_KEY = "tavily_api_key"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
