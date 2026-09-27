package com.kimi.community.data.api

import android.content.Context
import com.google.gson.Gson
import com.kimi.community.KimiApp
import com.kimi.community.data.model.RefreshTokenRequest
import com.kimi.community.data.prefs.UserPreferences
import com.kimi.community.ui.debug.DebugLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Token 刷新器：401/403 触发时换新并重放；双令牌轮换。
 * 文档实测 RefreshToken 端点（/api/...）在 Web 生产网关返回 404，
 * 因此刷新失败时静默降级——保留旧 token，由 UI 层引导重新登录。
 */
object TokenRefresher {
    private val mutex = Mutex()
    private var lastRefreshAt = 0L
    private var isRefreshing = false
    private var webRefreshInProgress = false

    /**
     * 尝试用 refreshToken 换取新令牌。
     * 优先调用 RefreshToken API；若 API 不可用（404），则启动透明 WebView Activity
     * 后台加载 kimi.com 并从 localStorage 获取新 token。
     * @return true 表示刷新成功（accessToken 已更新）
     */
    suspend fun refresh(force: Boolean = false): Boolean {
        if (!force && System.currentTimeMillis() - lastRefreshAt < 120_000L) return true
        return mutex.withLock {
            if (isRefreshing) return false
            val app = KimiApp.instance ?: return false
            val userPrefs = app.userPrefs
            isRefreshing = true
            try {
                // 方案一：RefreshToken API
                val refreshToken = userPrefs.refreshToken.first()
                if (!refreshToken.isNullOrEmpty()) {
                    try {
                        val api = KimiApiClient.createAuth(KimiApiService::class.java)
                        val resp = api.refreshToken(RefreshTokenRequest(refreshToken))
                        if (resp.accessToken != null) {
                            userPrefs.setAccessToken(resp.accessToken)
                            if (!resp.refreshToken.isNullOrEmpty()) {
                                userPrefs.setRefreshToken(resp.refreshToken)
                            }
                            lastRefreshAt = System.currentTimeMillis()
                            DebugLogStore.log("TOKEN", "API刷新成功 (${resp.accessToken.take(8)}...)")
                            return@withLock true
                        }
                    } catch (e: ApiException) {
                        DebugLogStore.log("TOKEN", "API刷新业务错误 code=${e.code}，尝试WebView fallback")
                    } catch (e: Exception) {
                        DebugLogStore.log("TOKEN", "API刷新异常: ${e.message}，尝试WebView fallback")
                    }
                }

                // 方案二：透明 WebView Activity 后台获取 token
                return refreshViaWebView(app, userPrefs)
            } finally {
                isRefreshing = false
            }
        }
    }

    /**
     * 启动透明 WebView Activity 加载 kimi.com，从 localStorage 获取新 token。
     * 轮询 DataStore 等待 token 更新，最多等待 15 秒。
     */
    private suspend fun refreshViaWebView(app: KimiApp, userPrefs: com.kimi.community.data.prefs.UserPreferences): Boolean {
        if (webRefreshInProgress) {
            DebugLogStore.log("TOKEN", "WebView刷新已在进行中，跳过")
            return false
        }
        webRefreshInProgress = true
        return try {
            val oldToken = userPrefs.accessToken.first()
            // 在主线程启动透明 Activity
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    com.kimi.community.ui.auth.TokenRefreshActivity.start(app)
                    DebugLogStore.log("TOKEN", "已启动透明WebView刷新Activity")
                } catch (e: Exception) {
                    DebugLogStore.log("TOKEN", "启动WebView Activity失败: ${e.message}")
                }
            }
            // 轮询等待新 token（最多15秒）
            repeat(30) {
                kotlinx.coroutines.delay(500)
                val newToken = userPrefs.accessToken.first()
                if (!newToken.isNullOrEmpty() && newToken != oldToken) {
                    lastRefreshAt = System.currentTimeMillis()
                    DebugLogStore.log("TOKEN", "WebView刷新成功 (${newToken.take(8)}...)")
                    return true
                }
            }
            DebugLogStore.log("TOKEN", "WebView刷新超时（15秒未获取到新token）")
            false
        } finally {
            webRefreshInProgress = false
        }
    }

    /** 从登录响应写入双令牌 */
    suspend fun saveTokens(accessToken: String?, refreshToken: String?) {
        val app = KimiApp.instance ?: return
        if (!accessToken.isNullOrEmpty()) app.userPrefs.setAccessToken(accessToken)
        if (!refreshToken.isNullOrEmpty()) app.userPrefs.setRefreshToken(refreshToken)
        if (!accessToken.isNullOrEmpty()) lastRefreshAt = System.currentTimeMillis()
    }
}

object KimiApiClient {
    private const val BASE_URL = "https://www.kimi.com/"
    private const val CLIENT_VERSION = "3.1.0"
    private const val APP_CHANNEL = "community"
    private const val MAX_RETRY = 3

    lateinit var userPrefs: UserPreferences
        private set
    private var appContext: Context? = null
    private lateinit var retrofit: Retrofit
    private lateinit var authRetrofit: Retrofit

    fun init(context: Context) {
        appContext = context.applicationContext
        userPrefs = (context.applicationContext as KimiApp).userPrefs
        retrofit = buildRetrofit(baseUrl = BASE_URL, authBase = false)
        authRetrofit = buildRetrofit(baseUrl = BASE_URL, authBase = true)
    }

    private fun buildRetrofit(baseUrl: String, authBase: Boolean): Retrofit {
        val debugMode = runBlocking { userPrefs.debugMode.first() }
        val loggingInterceptor = if (debugMode) {
            HttpLoggingInterceptor { message -> DebugLogStore.log("API", message) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        } else {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(authBase))
            .addInterceptor(ApiErrorInterceptor(authBase))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)
    fun <T> createAuth(service: Class<T>): T = authRetrofit.create(service)

    /** 全局公共请求头注入（严格按 Kimi_API_zh.md 第 3 节） */
    private class HeaderInterceptor(private val authBase: Boolean) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-msh-platform", "android")
                .header("x-msh-version", CLIENT_VERSION)
                .header("x-msh-os-version", android.os.Build.VERSION.SDK_INT.toString())
                .header("x-msh-device-model", android.os.Build.MODEL ?: "unknown")
                .header("x-msh-android-version", android.os.Build.VERSION.RELEASE ?: "")
                .header("x-msh-app-channel", APP_CHANNEL)
                .header("R-Timezone", "Asia/Shanghai")
                .header("X-Language", "zh-CN")
                .header("x-user-region", "CN")

            val ctx = appContext
            if (ctx != null) {
                val deviceId = runBlocking {
                    userPrefs.deviceId.first() ?: generateDeviceId()
                }
                builder.header("x-msh-device-id", deviceId)
                val sessionId = runBlocking {
                    userPrefs.sessionId.first() ?: generateSessionId()
                }
                builder.header("x-msh-session-id", sessionId)
            }

            // 鉴权接口（RefreshToken/Logout）同样携带 Authorization（如有）
            val token = runBlocking { userPrefs.accessToken.first() }
            if (!token.isNullOrEmpty()) {
                builder.header("Authorization", "Bearer $token")
            }
            return chain.proceed(builder.build())
        }
    }

    /**
     * API 错误处理拦截器（严格按 Kimi_API_zh.md 第 6 节错误码总表）：
     * 1. 检测业务错误（HTTP 非 200，或 body 含 code != 0）
     * 2. 401/403 → 刷新 token 并重放原请求（鉴权接口自身除外，避免递归）
     * 3. 429/503/504 → 指数退避重试（最多 MAX_RETRY 次）
     * 4. 其他错误（code != 0）→ 抛 ApiException，由 Repository/UI 层展示
     * 5. 成功（code == 0 或无 code 字段）→ 放行，Gson 直接解析为业务对象
     */
    private class ApiErrorInterceptor(private val authBase: Boolean) : Interceptor {
        private val gson = Gson()

        override fun intercept(chain: Interceptor.Chain): Response {
            // 最外层兜底：拦截器任何异常都不应导致 App 崩溃，直接放行原始响应
            return try {
                interceptInternal(chain)
            } catch (e: Throwable) {
                DebugLogStore.log("API", "拦截器异常（放行）: ${e.message}")
                chain.proceed(chain.request())
            }
        }

        private fun interceptInternal(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var response = chain.proceed(request)
            var attempts = 0
            var authRefreshed = false

            while (attempts <= MAX_RETRY) {
                val error = parseError(response)

                // 401/403 刷新重放（仅首次，鉴权接口自身除外）
                if (!authBase && !authRefreshed && error != null && ErrorCodes.isAuthError(error.code)) {
                    authRefreshed = true
                    val refreshed = try {
                        runBlocking(Dispatchers.IO) { TokenRefresher.refresh() }
                    } catch (e: Throwable) {
                        DebugLogStore.log("API", "Token刷新异常: ${e.message}")
                        false
                    }
                    if (refreshed) {
                        response.close()
                        val newToken = try {
                            runBlocking { userPrefs.accessToken.first() }
                        } catch (e: Throwable) { null }
                        if (!newToken.isNullOrEmpty()) {
                            request = request.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                            response = chain.proceed(request)
                            continue // 重新检查错误
                        }
                    }
                    // 刷新失败：继续往下走，最终抛 ApiException(401) 引导登录
                }

                // 429/503/504 退避重试
                if (error != null && ErrorCodes.isRetryable(error.code) && attempts < MAX_RETRY) {
                    val waitMs = 500L * (1L shl attempts)
                    DebugLogStore.log("API", "错误码 ${error.code} 退避重试 ${attempts + 1}/$MAX_RETRY (${waitMs}ms)")
                    try {
                        Thread.sleep(waitMs)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    response.close()
                    response = chain.proceed(request)
                    attempts++
                    continue
                }

                // 其他错误（code != 0）→ 抛 ApiException
                if (error != null && error.code != 0) {
                    val code = error.code
                    val msg = error.message
                    response.close()
                    DebugLogStore.log("API", "业务错误 code=$code msg=$msg")
                    throw ApiException(code, msg)
                }

                // 成功，放行
                return response
            }
            return response
        }

        /**
         * 解析错误响应：
         * - HTTP 非 200 → 用 HTTP 状态码作为错误码
         * - HTTP 200 但 body 含 code != 0 → 业务错误
         * - 否则（成功响应，无 code 字段或 code=0）→ null
         * 使用 peekBody 读取副本（最多 256KB），不消耗原始 response body。
         */
        private fun parseError(response: Response): ErrorResponse? {
            return try {
                if (response.code != 200) {
                    return ErrorResponse(code = response.code, message = response.message)
                }
                if (response.body == null) return null
                // 最多读取 256KB，避免大响应 OOM；错误响应通常很小
                val bodyString = response.peekBody(256 * 1024L).string()
                if (bodyString.isBlank()) return null
                val error = gson.fromJson(bodyString, ErrorResponse::class.java)
                if (error != null && error.code != 0) error else null
            } catch (e: Throwable) {
                null
            }
        }
    }

    private fun generateDeviceId(): String {
        val id = "kimi-" + UUID.randomUUID().toString().replace("-", "")
        runBlocking { userPrefs.setDeviceId(id) }
        return id
    }

    private fun generateSessionId(): String {
        val id = "s-" + UUID.randomUUID().toString().replace("-", "")
        runBlocking { userPrefs.setSessionId(id) }
        return id
    }
}
