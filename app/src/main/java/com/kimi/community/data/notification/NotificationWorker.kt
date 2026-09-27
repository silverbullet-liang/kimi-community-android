package com.kimi.community.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.kimi.community.KimiApp
import com.kimi.community.MainActivity
import com.kimi.community.R
import com.kimi.community.data.api.TokenRefresher
import com.kimi.community.data.model.Notification
import com.kimi.community.data.model.NotificationType
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 后台轮询 Worker：
 * 1. 先尝试刷新 accessToken（15 分钟有效期，后台保持登录态实时刷新）；
 * 2. 调用 ListNotification，发现新通知后推送系统通知。
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KimiApp
        val prefs = app.userPrefs
        val isLoggedIn = prefs.accessToken.first()?.isNotEmpty() == true
        if (!isLoggedIn) return Result.success()

        return try {
            // 后台实时刷新 token（避免 15 分钟过期）
            TokenRefresher.refresh()
            val repository = KimiRepository()
            val response = repository.listNotification(idGt = null, limit = 10)
            val notifications = response.notifications ?: emptyList()
            if (notifications.isNotEmpty()) {
                showNotification(notifications.first())
            }
            Result.success()
        } catch (e: Exception) {
            // 网络失败，退避重试
            Result.retry()
        }
    }

    private fun showNotification(notification: Notification) {
        val context = applicationContext
        val typeText = getNotificationTypeText(notification.notificationType)
        val userName = notification.fromUsers?.firstOrNull()
        val title = "${userName ?: "用户"} $typeText"
        val text = "点击查看"

        val channelId = "kimi_notifications"
        createChannel(channelId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_moment_id", notification.momentId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                notification.id?.hashCode() ?: System.currentTimeMillis().toInt(),
                builder.build()
            )
        } catch (e: SecurityException) {
            // 通知权限被拒，忽略
        }
    }

    private fun createChannel(channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "社区消息",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Kimi 社区关注、回复、评论通知"
        }
        manager.createNotificationChannel(channel)
    }

    private fun getNotificationTypeText(type: String?): String {
        return when (type) {
            NotificationType.FOLLOW -> "关注了你"
            NotificationType.REPLY_COMMENT -> "回复了你的评论"
            NotificationType.COMMENT_MOMENT -> "评论了你的动态"
            NotificationType.UP_COMMENT -> "点赞了你的评论"
            NotificationType.UP_MOMENT -> "点赞了你的动态"
            NotificationType.FAVORITE -> "收藏了你的动态"
            else -> "有新消息"
        }
    }

    companion object {
        private const val WORK_NAME = "kimi_notification_polling"

        /** 根据当前设置的频率调度（或更新）周期轮询任务（最短 15 分钟） */
        fun schedule(context: Context, intervalMs: Long = 300000L) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                intervalMs.coerceAtLeast(15 * 60 * 1000L), // WorkManager 最短周期 15 分钟
                TimeUnit.MILLISECONDS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
