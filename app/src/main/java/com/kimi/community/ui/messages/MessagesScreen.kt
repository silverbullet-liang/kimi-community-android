package com.kimi.community.ui.messages

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kimi.community.data.model.Notification
import com.kimi.community.data.model.NotificationType
import com.kimi.community.data.model.UserMiniInfo
import com.kimi.community.ui.components.formatTimeString
import com.kimi.community.ui.detail.MomentDetailActivity
import com.kimi.community.ui.profile.UserProfileActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MessagesViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    val userMap = viewModel.userMap
    val momentMap = viewModel.momentMap

    // 分页加载：接近底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("消息", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text("全部已读")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is MessagesUiState.Loading -> {
                    Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MessagesUiState.Error -> {
                    Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                is MessagesUiState.Success -> {
                    if (notifications.isEmpty()) {
                        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("暂无消息", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notifications, key = { it.id ?: "" }) { notification ->
                                // 通过 fromUsers (userId列表) 在 userMap 中查找用户信息
                                val fromUserIds = notification.fromUsers ?: emptyList()
                                val firstUser = fromUserIds.firstOrNull()?.let { userMap[it] }
                                val extraCount = (fromUserIds.size - 1).coerceAtLeast(0)
                                val momentTitle = momentMap[notification.momentId]?.content?.title

                                NotificationCard(
                                    notification = notification,
                                    fromUser = firstUser,
                                    extraUserCount = extraCount,
                                    momentTitle = momentTitle,
                                    onClick = {
                                        notification.momentId?.let { momentId ->
                                            context.startActivity(
                                                Intent(context, MomentDetailActivity::class.java)
                                                    .putExtra("momentId", momentId)
                                            )
                                        }
                                    },
                                    onUserClick = { userId ->
                                        userId?.let {
                                            context.startActivity(
                                                Intent(context, UserProfileActivity::class.java)
                                                    .putExtra("userId", it)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    fromUser: UserMiniInfo?,
    extraUserCount: Int,
    momentTitle: String?,
    onClick: () -> Unit,
    onUserClick: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val typeText = getNotificationTypeText(notification.notificationType)
    val displayName = buildString {
        append(fromUser?.name ?: "用户")
        if (extraUserCount > 0) append(" 等$extraUserCount 人")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // 头像（fromUsers 第一位，通过 userMap 查找）
            AsyncImage(
                model = fromUser?.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onUserClick(fromUser?.userId) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onUserClick(fromUser?.userId) }
                    )
                    Text(
                        text = formatTimeString(notification.happenedTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 消息类型
                Text(
                    text = typeText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 内容折叠
                Text(
                    text = "$typeText：$displayName",
                    fontSize = 14.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = !expanded }
                )

                // 作品名称方格（右下角 1:1，最多3字）
                momentTitle?.let { title ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title.take(3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 回复/评论类型底部按钮
                if (notification.notificationType == NotificationType.REPLY_COMMENT ||
                    notification.notificationType == NotificationType.COMMENT_MOMENT ||
                    notification.notificationType == NotificationType.UP_COMMENT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { /* 点赞评论 */ }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("点赞", fontSize = 13.sp)
                        }
                        TextButton(onClick = { /* 回复 */ }) {
                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("回复", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun getNotificationTypeText(type: String?): String {
    return when (type) {
        NotificationType.FOLLOW -> "关注了你"
        NotificationType.REPLY_COMMENT -> "回复了你的评论"
        NotificationType.COMMENT_MOMENT -> "评论了你的动态"
        NotificationType.UP_COMMENT -> "点赞了你的评论"
        NotificationType.UP_MOMENT -> "点赞了你的动态"
        NotificationType.FAVORITE -> "收藏了你的动态"
        NotificationType.ACTIVITY -> "活动通知"
        NotificationType.OPERATIONAL -> "运营通知"
        NotificationType.MY_KIMI -> "Kimi 通知"
        NotificationType.TASK -> "任务通知"
        NotificationType.SKILL -> "技能通知"
        else -> "通知"
    }
}
