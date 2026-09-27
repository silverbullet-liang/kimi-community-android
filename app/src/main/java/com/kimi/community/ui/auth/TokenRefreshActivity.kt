package com.kimi.community.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.kimi.community.KimiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 透明 Activity：后台启动 WebView 加载 kimi.com，从 localStorage 获取新 access_token。
 * 当 RefreshToken API 不可用（404）时，作为 fallback 方案。
 * 用户无感知（透明背景 + 无 UI），获取到 token 后自动 finish。
 */
class TokenRefreshActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var finished = false

    companion object {
        private const val TIMEOUT_MS = 20000L
        private const val KIMI_URL = "https://www.kimi.com"

        /** 启动 token 刷新 Activity（如果当前已在运行则不重复启动） */
        fun start(context: Context) {
            val intent = Intent(context, TokenRefreshActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 透明背景，无内容视图
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 页面加载完成后，注入 JS 从 localStorage 读取 token
                    view?.postDelayed({
                        extractToken(view)
                    }, 1500)
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(TokenJsInterface { token ->
                if (token.isNotEmpty() && !finished) {
                    scope.launch {
                        (application as KimiApp).userPrefs.setAccessToken(token)
                    }
                    finishRefresh()
                }
            }, "AndroidBridge")
        }
        // 不把 WebView 添加到视图层级（后台运行）
        webView.loadUrl(KIMI_URL)

        // 超时保护
        scope.launch {
            delay(TIMEOUT_MS)
            finishRefresh()
        }
    }

    /** 注入 JS 读取 localStorage 中的 access_token */
    private fun extractToken(view: WebView?) {
        val js = """
            (function() {
                try {
                    var keys = Object.keys(localStorage);
                    for (var i = 0; i < keys.length; i++) {
                        var key = keys[i];
                        if (key.indexOf('access_token') >= 0 || key.indexOf('token') >= 0) {
                            var val = localStorage.getItem(key);
                            if (val && val.length > 20 && val.indexOf('{') < 0) {
                                AndroidBridge.onToken(val);
                                return;
                            }
                        }
                    }
                    // 尝试从 cookie 或其他存储读取
                    AndroidBridge.onToken('');
                } catch(e) {
                    AndroidBridge.onToken('');
                }
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun finishRefresh() {
        if (finished) return
        finished = true
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {
            // ignore
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webView.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }

    /** JS 桥接接口 */
    class TokenJsInterface(private val callback: (String) -> Unit) {
        @JavascriptInterface
        fun onToken(token: String) {
            callback(token)
        }
    }
}
