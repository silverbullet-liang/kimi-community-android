package com.kimi.community.ui.webview

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kimi.community.KimiApp
import com.kimi.community.ui.theme.KimiCommunityTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * token 捕获全局状态：JS bridge 写入，Compose 层观察。
 * 分别保存 access_token 与 refresh_token。
 */
object TokenCaptureStore {
    private val _token = MutableStateFlow<String?>(null)
    private val _refreshToken = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    fun setToken(token: String) {
        if (!token.isNullOrBlank()) _token.value = token
    }

    fun setRefreshToken(token: String) {
        if (!token.isNullOrBlank()) _refreshToken.value = token
    }

    fun reset() {
        _token.value = null
        _refreshToken.value = null
    }
}

class WebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: "https://www.kimi.com"
        val title = intent.getStringExtra("title") ?: "网页"
        val captureToken = intent.getBooleanExtra("captureToken", false)
        TokenCaptureStore.reset()
        setContent {
            KimiCommunityTheme {
                WebViewScreen(url = url, title = title, captureToken = captureToken)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    captureToken: Boolean
) {
    val context = LocalContext.current
    val userPrefs = (context.applicationContext as KimiApp).userPrefs
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf(title) }
    var isLoading by remember { mutableStateOf(true) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val capturedToken by TokenCaptureStore.token.collectAsState()
    val capturedRefreshToken by TokenCaptureStore.refreshToken.collectAsState()
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.clipData != null -> {
                    val clip = data.clipData
                    Array(clip!!.itemCount) { i -> clip.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                cameraImageUri != null -> arrayOf(cameraImageUri!!)
                else -> null
            }
        } else null
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            (context as? Activity)?.finish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle, fontWeight = FontWeight.Medium, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, webView?.url ?: url)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "分享"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                }
            )
        },
        // 登录凭证捕获模式：底部显示手动保存栏（不再自动保存/自动关闭）
        bottomBar = {
            if (captureToken && capturedToken != null) {
                Surface(tonalElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "已检测到登录凭证",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Token: ${maskToken(capturedToken)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { (context as? Activity)?.finish() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("暂不保存")
                            }
                            Button(
                                onClick = {
                                    val token = capturedToken
                                    val rToken = capturedRefreshToken
                                    (context as? Activity)?.let { act ->
                                        scope.launch(Dispatchers.IO) {
                                            userPrefs.setAccessToken(token)
                                            if (!rToken.isNullOrBlank()) {
                                                userPrefs.setRefreshToken(rToken)
                                            }
                                            // 15 分钟有效期记录
                                            userPrefs.setTokenExpiresAt(System.currentTimeMillis() + 900_000L)
                                            withContext(Dispatchers.Main) { act.finish() }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("保存登录态")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webView = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            javaScriptCanOpenWindowsAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = true
                            setGeolocationEnabled(true)
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }
                        addJavascriptInterface(ClipboardInterface(ctx), "AndroidClipboard")
                        addJavascriptInterface(TokenCallback(), "AndroidTokenCallback")
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                currentTitle = view?.title ?: title
                                if (captureToken) {
                                    try {
                                        view?.evaluateJavascript(getTokenCaptureScript(), null)
                                    } catch (e: Exception) {
                                        // JS 注入失败不影响页面
                                    }
                                }
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                request?.url?.let { uri ->
                                    if (uri.scheme == "http" || uri.scheme == "https") {
                                        view?.loadUrl(uri.toString())
                                        return true
                                    }
                                }
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.let { req ->
                                    // 只 grant WebView 明确支持的资源，避免 IllegalArgumentException 闪退
                                    val supported = req.resources.filter { res ->
                                        res == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                                            res == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                            res == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                                    }
                                    if (supported.isEmpty()) {
                                        req.deny()
                                        return
                                    }
                                    val needs = mutableListOf<String>()
                                    if (supported.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                        needs.add(Manifest.permission.CAMERA)
                                    }
                                    if (supported.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        needs.add(Manifest.permission.RECORD_AUDIO)
                                    }
                                    if (needs.isEmpty()) {
                                        req.grant(supported.toTypedArray())
                                    } else {
                                        // 先授予 WebView 可用的资源，再请求系统权限
                                        req.grant(supported.toTypedArray())
                                        try {
                                            permissionLauncher.launch(needs.toTypedArray())
                                        } catch (e: Exception) {
                                            // launcher 异常不阻断页面
                                        }
                                    }
                                }
                            }
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    callback?.invoke(origin, true, false)
                                } else {
                                    try {
                                        permissionLauncher.launch(arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ))
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    callback?.invoke(origin, false, false)
                                }
                            }
                            override fun onShowFileChooser(
                                webView: WebView?,
                                callback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                filePathCallback = callback
                                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }
                                try {
                                    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                    if (picturesDir != null) {
                                        val imageFile = File(picturesDir, "IMG_$timeStamp.jpg")
                                        cameraImageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
                                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                                    }
                                    val chooserIntent = Intent.createChooser(intent, "选择文件")
                                    if (cameraImageUri != null) {
                                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
                                    }
                                    filePickerLauncher.launch(chooserIntent)
                                } catch (e: Exception) {
                                    // 文件选择器异常时回退为纯文件选择
                                    try {
                                        filePickerLauncher.launch(intent)
                                    } catch (e2: Exception) {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = null
                                    }
                                }
                                return true
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { webViewRef ->
                    webViewRef.stopLoading()
                    webViewRef.loadUrl("about:blank")
                    webViewRef.destroy()
                }
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            // 注意：不再自动保存 token / 自动关闭。
            // 捕获到 token 后仅在底部栏显示预览，由用户手动点击"保存登录态"才持久化。
        }
    }
}

class ClipboardInterface(private val context: Context) {
    @JavascriptInterface
    fun writeText(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("kimi", text))
        } catch (e: Exception) {
            // ignore
        }
    }
    @JavascriptInterface
    fun readText(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

/** 油猴脚本回调：捕获 access_token / refresh_token 后写入全局状态（JS bridge 线程） */
class TokenCallback {
    @JavascriptInterface
    fun onTokenCaptured(token: String) {
        if (!token.isNullOrBlank()) {
            TokenCaptureStore.setToken(token)
        }
    }
    @JavascriptInterface
    fun onRefreshTokenCaptured(token: String) {
        if (!token.isNullOrBlank()) {
            TokenCaptureStore.setRefreshToken(token)
        }
    }
}

/** Token 脱敏显示：前 8 位 + ... + 后 4 位 */
private fun maskToken(token: String?): String {
    if (token.isNullOrBlank()) return ""
    if (token.length <= 16) return token
    return "${token.take(8)}...${token.takeLast(4)}"
}

/**
 * token 捕获脚本：从 localStorage 实时获取（用户要求 token 从 LocalStorage 获取）。
 * - 页面加载完成后立即扫描一次
 * - 每 800ms 轮询 localStorage（token 可能在登录完成后才写入）
 * - 监听 storage 事件
 * - 拦截 fetch/XHR 的 Authorization 头兜底
 *
 * 识别规则（收紧，避免误判）：
 * - 必须是字符串且长度 >= 30
 * - 优先匹配 JWT 格式（三段 base64url，含两个点）
 * - 排除 JSON（以 { 开头）、URL（以 http 开头）、含空格/引号/花括号的值
 * - 捕获后仅上报，由用户手动点击"保存登录态"才持久化（不再自动保存）
 */
private fun getTokenCaptureScript(): String {
    return """
        (function() {
            if (window.__kimiTokenMonitor) return;
            window.__kimiTokenMonitor = true;
            var reported = {};
            function looksLikeToken(v) {
                if (typeof v !== 'string') return false;
                if (v.length < 30) return false;
                // 排除明显非 token 的值
                if (v.indexOf(' ') !== -1) return false;
                if (v.indexOf('"') !== -1 || v.indexOf("'") !== -1) return false;
                if (v.indexOf('{') !== -1 || v.indexOf('}') !== -1) return false;
                if (v.indexOf('[') !== -1 || v.indexOf(']') !== -1) return false;
                if (v.indexOf('<') !== -1 || v.indexOf('>') !== -1) return false;
                if (v.indexOf('http://') === 0 || v.indexOf('https://') === 0) return false;
                if (v.indexOf('data:') === 0) return false;
                // JWT 格式优先（三段 base64url）
                var jwtMatch = v.match(/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/);
                if (jwtMatch) return true;
                // 非 JWT：长度 >= 50 且只含 base64url 字符 + 常见 token 字符
                if (v.length >= 50) {
                    var tokenChars = /^[A-Za-z0-9_\-\.]+$/;
                    if (tokenChars.test(v)) return true;
                }
                return false;
            }
            function isRefreshKey(k) {
                return k.toLowerCase().indexOf('refresh') !== -1;
            }
            function report(key, value) {
                if (!looksLikeToken(value)) return;
                if (reported[key] === value) return;
                reported[key] = value;
                try {
                    if (isRefreshKey(key)) {
                        if (window.AndroidTokenCallback) window.AndroidTokenCallback.onRefreshTokenCaptured(value);
                    } else {
                        if (window.AndroidTokenCallback) window.AndroidTokenCallback.onTokenCaptured(value);
                    }
                } catch(e) {}
            }
            function scan() {
                try {
                    var keys = Object.keys(localStorage);
                    for (var i = 0; i < keys.length; i++) {
                        var key = keys[i];
                        var lower = key.toLowerCase();
                        if (lower.indexOf('token') !== -1 || lower.indexOf('auth') !== -1 || lower.indexOf('access') !== -1) {
                            var value = localStorage.getItem(key);
                            report(key, value);
                        }
                    }
                } catch(e) {}
            }
            scan();
            try {
                window.addEventListener('storage', function(e) {
                    if (e.key && e.newValue) report(e.key, e.newValue);
                });
            } catch(e) {}
            setInterval(scan, 800);
            // 兜底：拦截 fetch 的 Authorization 头
            try {
                var originalFetch = window.fetch;
                window.fetch = function() {
                    try {
                        var h = arguments[1] && arguments[1].headers;
                        if (h) {
                            var auth = h['Authorization'] || h['authorization'] || (h.get && h.get('Authorization'));
                            if (auth && auth.indexOf('Bearer') !== -1) {
                                report('fetch_auth', auth.replace('Bearer ', ''));
                            }
                        }
                    } catch(e) {}
                    return originalFetch.apply(this, arguments);
                };
            } catch(e) {}
            // 兜底：拦截 XHR Authorization 头
            try {
                var origSet = XMLHttpRequest.prototype.setRequestHeader;
                XMLHttpRequest.prototype.setRequestHeader = function(k, v) {
                    try {
                        if ((k === 'Authorization' || k === 'authorization') && v.indexOf('Bearer') !== -1) {
                            report('xhr_auth', v.replace('Bearer ', ''));
                        }
                    } catch(e) {}
                    return origSet.apply(this, arguments);
                };
            } catch(e) {}
        })();
    """.trimIndent()
}
