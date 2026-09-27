package com.kimi.community

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kimi.community.data.api.KimiApiClient
import com.kimi.community.data.notification.NotificationWorker
import com.kimi.community.data.prefs.UserPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class KimiApp : Application() {

    lateinit var userPrefs: UserPreferences
        private set

    private var defaultUncaughtHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        installCrashHandler()
        userPrefs = UserPreferences(this)
        KimiApiClient.init(this)
        createNotificationChannels()
        // 调度后台通知轮询（Worker 内部校验登录态与频率）
        try {
            NotificationWorker.schedule(this)
        } catch (e: Exception) {
            // WorkManager 初始化失败不阻断启动
        }
    }

    /** 全局崩溃捕获：把堆栈写入外部文件，方便定位"打开就闪退"等问题 */
    private fun installCrashHandler() {
        defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashInfo = buildString {
                    appendLine("=== Kimi 社区崩溃日志 ===")
                    appendLine("时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine("线程: ${thread.name} (id=${thread.id})")
                    appendLine("App版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("设备: ${Build.MODEL} (${Build.MANUFACTURER})")
                    appendLine()
                    appendLine("=== 异常堆栈 ===")
                    appendLine(sw.toString())
                    var cause = throwable.cause
                    while (cause != null) {
                        appendLine()
                        appendLine("=== Caused by: ${cause.javaClass.name}: ${cause.message} ===")
                        val csw = StringWriter()
                        cause.printStackTrace(PrintWriter(csw))
                        appendLine(csw.toString())
                        cause = cause.cause
                    }
                }
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = File(dir, "kimi_crash_${System.currentTimeMillis()}.txt")
                file.writeText(crashInfo)
            } catch (e: Exception) {
                // 崩溃处理器自身不能再崩溃
            }
            defaultUncaughtHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_COMMUNITY,
                "社区消息",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kimi 社区消息通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_COMMUNITY = "community_notifications"
        lateinit var instance: KimiApp
            private set
    }
}
