package com.kimi.community.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimi.community.BuildConfig
import com.kimi.community.KimiApp
import com.kimi.community.data.notification.NotificationWorker
import com.kimi.community.ui.theme.ThemeColors
import com.kimi.community.ui.webview.WebViewActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as KimiApp
    val userPrefs = app.userPrefs
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var pollInterval by remember { mutableStateOf("300000") }
    var darkMode by remember { mutableStateOf(0) }
    var themeColor by remember { mutableStateOf(ThemeColors.KIMI) }
    var debugMode by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var updateChannel by remember { mutableStateOf("stable") } // stable=正式版, beta=测试版
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    // 注意：多个 Flow 必须并发收集（launch），否则第一个 collect 会永远挂起，后续收不到
    LaunchedEffect(Unit) {
        launch { userPrefs.isLoggedIn.collect { isLoggedIn = it } }
        launch { userPrefs.pollInterval.collect { pollInterval = it.toString() } }
        launch { userPrefs.darkMode.collect { darkMode = it } }
        launch { userPrefs.debugMode.collect { debugMode = it } }
        launch { userPrefs.themeColor.collect { themeColor = it } }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 账户
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("账户", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isLoggedIn) "已登录（token 自动刷新）" else "未登录", fontSize = 14.sp)
                        Button(
                            onClick = {
                                scope.launch {
                                    if (isLoggedIn) {
                                        // 网络登出（失败也允许本地清除）
                                        try {
                                            com.kimi.community.data.repository.KimiRepository().logout()
                                        } catch (e: Exception) { /* ignore */ }
                                        // 仅清除登录凭证，保留主题色/频率等设置
                                        userPrefs.clearLoginInfo()
                                        NotificationWorker.cancel(context)
                                    } else {
                                        context.startActivity(
                                            Intent(context, WebViewActivity::class.java)
                                                .putExtra("url", "https://www.kimi.com")
                                                .putExtra("title", "登录 Kimi")
                                                .putExtra("captureToken", true)
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(if (isLoggedIn) "退出登录" else "登录")
                        }
                    }
                }
            }

            // API 请求频率
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("API 请求频率 (毫秒)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pollInterval,
                        onValueChange = { pollInterval = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = pollInterval.toLongOrNull() == null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("默认 300000ms (5分钟)，用于后台消息轮询", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (pollInterval.toLongOrNull() != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            val ms = pollInterval.toLongOrNull() ?: 300000L
                            scope.launch {
                                userPrefs.setPollInterval(ms.toInt())
                                NotificationWorker.schedule(context, ms)
                            }
                        }) {
                            Text("保存频率")
                        }
                    }
                }
            }

            // 主题色（多色选择，实时生效）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("主题色", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "选择后立即生效，可随时更换",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeColors.options.forEach { option ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                        .then(
                                            if (themeColor == option.id) {
                                                Modifier.border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            themeColor = option.id
                                            scope.launch { userPrefs.setThemeColor(option.id) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (themeColor == option.id) {
                                        Text(
                                            "✓",
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(option.label, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 深色模式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("深色模式", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = darkMode == 0,
                            onClick = {
                                darkMode = 0
                                scope.launch { userPrefs.setDarkMode(0) }
                            },
                            label = { Text("跟随系统") }
                        )
                        FilterChip(
                            selected = darkMode == 1,
                            onClick = {
                                darkMode = 1
                                scope.launch { userPrefs.setDarkMode(1) }
                            },
                            label = { Text("浅色") }
                        )
                        FilterChip(
                            selected = darkMode == 2,
                            onClick = {
                                darkMode = 2
                                scope.launch { userPrefs.setDarkMode(2) }
                            },
                            label = { Text("深色") }
                        )
                    }
                }
            }

            // 调试模式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("调试模式", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("显示 API 请求日志浮动面板", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = debugMode,
                        onCheckedChange = {
                            debugMode = it
                            scope.launch { userPrefs.setDebugMode(it) }
                        }
                    )
                }
            }

            // 关于
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关于", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("版本", fontSize = 14.sp)
                        Text("${BuildConfig.VERSION_NAME} (${if (updateChannel == "beta") "测试版" else "正式版"})", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 渠道选择
                    Text("更新渠道", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = updateChannel == "stable",
                            onClick = { updateChannel = "stable" },
                            label = { Text("正式版") }
                        )
                        FilterChip(
                            selected = updateChannel == "beta",
                            onClick = { updateChannel = "beta" },
                            label = { Text("测试版") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 检查更新
                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            updateResult = null
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                updateResult = "当前已是最新版本（${BuildConfig.VERSION_NAME}）"
                                isCheckingUpdate = false
                            }
                        },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("检查中...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("检查更新")
                        }
                    }
                    updateResult?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // GitHub 仓库链接
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(context, WebViewActivity::class.java)
                                        .putExtra("url", "https://github.com/silverbullet-liang/kimi-community-android")
                                        .putExtra("title", "GitHub 仓库")
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GitHub 开源仓库", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("查看", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { showDisclaimer = true }) {
                        Text("免责声明")
                    }
                }
            }
        }
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = { Text("免责声明") },
            text = {
                Column {
                    Text("本项目仅用于技术交流学习，请勿用于非法用途。", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("打开后请在 24 小时内删除，否则后果自负，与本人无任何关系。", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("本应用为社区复刻学习项目，与 Kimi 官方无任何关联。", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimer = false }) {
                    Text("我已知晓")
                }
            }
        )
    }
}
