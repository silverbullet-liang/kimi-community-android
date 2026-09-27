package com.kimi.community

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kimi.community.ui.debug.DebugLogStore
import com.kimi.community.ui.debug.DebugOverlay
import com.kimi.community.ui.home.HomeScreen
import com.kimi.community.ui.messages.MessagesScreen
import com.kimi.community.ui.profile.ProfileScreen
import com.kimi.community.ui.settings.SettingsScreen
import com.kimi.community.ui.theme.KimiCommunityTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 通知权限请求
        val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val app = application as KimiApp
            val userPrefs = app.userPrefs

            var darkMode by remember { mutableStateOf(0) }
            var themeColor by remember { mutableStateOf(com.kimi.community.ui.theme.ThemeColors.KIMI) }
            var debugMode by remember { mutableStateOf(false) }

            // 注意：多个 Flow 必须并发收集（launch），否则第一个 collect 会永远挂起，后续收不到
            LaunchedEffect(Unit) {
                launch { userPrefs.darkMode.collect { darkMode = it } }
                launch { userPrefs.themeColor.collect { themeColor = it } }
                launch { userPrefs.debugMode.collect { debugMode = it } }
            }

            // 同步调试模式到日志存储
            LaunchedEffect(debugMode) {
                DebugLogStore.setEnabled(debugMode)
            }

            KimiCommunityTheme(darkMode = darkMode, themeColor = themeColor) {
                MainScreen()
                if (debugMode) {
                    DebugOverlay(onClose = { debugMode = false })
                }
            }
        }
    }
}

private enum class TabItem(val title: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Home),
    MESSAGES("消息", Icons.Default.Notifications),
    PROFILE("个人", Icons.Default.Person),
    SETTINGS("设置", Icons.Default.Settings)
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(TabItem.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TabItem.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            TabItem.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
            TabItem.MESSAGES -> MessagesScreen(modifier = Modifier.padding(innerPadding))
            TabItem.PROFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            TabItem.SETTINGS -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
