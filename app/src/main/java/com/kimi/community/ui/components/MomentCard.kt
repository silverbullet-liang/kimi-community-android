package com.kimi.community.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.kimi.community.data.model.Moment
import com.kimi.community.ui.webview.WebViewActivity

@Composable
fun MomentCard(
    moment: Moment,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onLike: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLiked = moment.interactionStatus?.isLiked ?: false
    val likeCount = (moment.stat?.likeNum ?: 0).coerceAtLeast(0)
    val isFavorited = moment.interactionStatus?.isCollected ?: false
    // chatShareCard 是 Moment 顶层字段（Kimi_API_zh.md 附录 A）
    val cdnUrl = moment.chatShareCard?.htmlFile?.cdnUrl
    val chatShareId = moment.content?.chatShareId
    val shareUrl = if (chatShareId != null) "https://www.kimi.com/chat/share/$chatShareId" else null
    val previewUrl = cdnUrl ?: shareUrl
    val cardImages = moment.chatShareCard?.imageList ?: moment.content?.media?.images
    val title = moment.content?.title ?: moment.chatShareCard?.notice ?: "作品"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 作者信息（可点击 → 用户主页）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAuthorClick)
            ) {
                AsyncImage(
                    model = moment.author?.userBase?.avatarImage?.url,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moment.author?.userBase?.name ?: "未知用户",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatTimeString(moment.publishTime ?: moment.createTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 作品标题
            moment.content?.title?.let { t ->
                Text(
                    text = t,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 作品简介
            moment.content?.excerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 作品正文（无标题/摘要时展示）
            if (moment.content?.title.isNullOrBlank() && moment.content?.excerpt.isNullOrBlank()) {
                moment.content?.text?.let { text ->
                    if (text.isNotBlank()) {
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // 作品预览（WebView 或图片，1:1 正方形）
            when {
                previewUrl != null -> {
                    MomentPreview(
                        url = previewUrl,
                        title = title,
                        onClick = onClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                !cardImages.isNullOrEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (cardImages.size == 1) {
                            AsyncImage(
                                model = cardImages[0].url ?: cardImages[0].originUrl,
                                contentDescription = "作品图片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // 多图展示第一张 + 角标
                            AsyncImage(
                                model = cardImages[0].url ?: cardImages[0].originUrl,
                                contentDescription = "作品图片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${cardImages.size}图", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 点赞
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onLike?.invoke() }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(likeCount.toString(), fontSize = 13.sp)
                }

                // 评论
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "评论",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text((moment.stat?.commentNum ?: 0).toString(), fontSize = 13.sp)
                }

                // 收藏
                IconButton(onClick = { onFavorite?.invoke() }) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 全屏
                IconButton(onClick = {
                    val intent = Intent(context, WebViewActivity::class.java)
                    intent.putExtra("url", previewUrl ?: "")
                    intent.putExtra("title", title)
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "全屏",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 作品预览 WebView（1:1 正方形）：
 * 复用单个 WebView 实例，页面离开组合时及时释放，避免内存泄漏。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MomentPreview(
    url: String,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            onRelease = { webViewRef ->
                webViewRef.stopLoading()
                webViewRef.loadUrl("about:blank")
                webViewRef.destroy()
            }
        )

        // 加载占位提示
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "预览",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("点击预览", fontSize = 14.sp)
            }
        }
    }
}

/**
 * 格式化时间：兼容秒/毫秒字符串与数字时间戳。
 * Kimi_API_zh.md 中 createTime/publishTime 为 string。
 */
fun formatTimeString(time: String?): String {
    if (time.isNullOrBlank()) return ""
    val ts = time.toLongOrNull() ?: return ""
    return formatTime(if (ts > 10_000_000_000L) ts / 1000 else ts)
}

fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 2592000 -> "${diff / 86400}天前"
        else -> "${diff / 2592000}月前"
    }
}
