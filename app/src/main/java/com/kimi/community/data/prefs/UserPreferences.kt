package com.kimi.community.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kimi_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        private val KEY_POLL_INTERVAL = intPreferencesKey("poll_interval")
        private val KEY_DARK_MODE = intPreferencesKey("dark_mode") // 0=system,1=light,2=dark
        private val KEY_CUSTOM_THEME = booleanPreferencesKey("custom_theme") // true=品牌色, false=壁纸
        private val KEY_DEBUG_MODE = booleanPreferencesKey("debug_mode")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_THEME_COLOR = stringPreferencesKey("theme_color") // kimi/blue/green/purple/orange/teal/dynamic
        private val KEY_TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val userAvatar: Flow<String?> = context.dataStore.data.map { it[KEY_USER_AVATAR] }
    val pollInterval: Flow<Int> = context.dataStore.data.map { it[KEY_POLL_INTERVAL] ?: 300000 }
    val darkMode: Flow<Int> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: 0 }
    val customTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_CUSTOM_THEME] ?: true }
    val debugMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DEBUG_MODE] ?: false }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[KEY_DEVICE_ID] }
    val sessionId: Flow<String?> = context.dataStore.data.map { it[KEY_SESSION_ID] }
    val themeColor: Flow<String> = context.dataStore.data.map { it[KEY_THEME_COLOR] ?: "kimi" }
    val tokenExpiresAt: Flow<Long> = context.dataStore.data.map { it[KEY_TOKEN_EXPIRES_AT] ?: 0L }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map {
        val token = it[KEY_ACCESS_TOKEN]
        // 收紧判定：非空 + 长度 >= 30 + 不含空格/引号/花括号（避免误捕获值被判定为已登录）
        !token.isNullOrBlank() &&
            token.length >= 30 &&
            !token.contains(" ") &&
            !token.contains("\"") &&
            !token.contains("{") &&
            !token.contains("}")
    }

    suspend fun setAccessToken(token: String?) {
        context.dataStore.edit { it[KEY_ACCESS_TOKEN] = token ?: "" }
    }

    suspend fun setRefreshToken(token: String?) {
        context.dataStore.edit { it[KEY_REFRESH_TOKEN] = token ?: "" }
    }

    suspend fun setUserInfo(id: String?, name: String?, avatar: String?) {
        context.dataStore.edit {
            it[KEY_USER_ID] = id ?: ""
            it[KEY_USER_NAME] = name ?: ""
            it[KEY_USER_AVATAR] = avatar ?: ""
        }
    }

    suspend fun setPollInterval(interval: Int) {
        context.dataStore.edit { it[KEY_POLL_INTERVAL] = interval }
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    suspend fun setCustomTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUSTOM_THEME] = enabled }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEBUG_MODE] = enabled }
    }

    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
    }

    suspend fun setSessionId(id: String) {
        context.dataStore.edit { it[KEY_SESSION_ID] = id }
    }

    suspend fun setThemeColor(color: String) {
        context.dataStore.edit { it[KEY_THEME_COLOR] = color }
    }

    suspend fun setTokenExpiresAt(time: Long) {
        context.dataStore.edit { it[KEY_TOKEN_EXPIRES_AT] = time }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    /** 仅清除登录凭证，保留主题色/频率/深色模式/调试模式等应用设置 */
    suspend fun clearLoginInfo() {
        context.dataStore.edit {
            it.remove(KEY_ACCESS_TOKEN)
            it.remove(KEY_REFRESH_TOKEN)
            it.remove(KEY_USER_ID)
            it.remove(KEY_USER_NAME)
            it.remove(KEY_USER_AVATAR)
            it.remove(KEY_TOKEN_EXPIRES_AT)
        }
    }

    suspend fun getAccessTokenSync(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }.let { flow ->
            var result: String? = null
            flow.collect { result = it; return@collect }
            result
        }
    }
}
