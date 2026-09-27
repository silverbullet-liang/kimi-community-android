package com.kimi.community.ui.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kimi.community.data.model.Comment
import com.kimi.community.data.model.Moment
import com.kimi.community.ui.components.formatTimeString
import com.kimi.community.ui.theme.KimiCommunityTheme
import com.kimi.community.ui.webview.WebViewActivity
import java.util.regex.Pattern

class MomentDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val momentId = intent.getStringExtra("momentId") ?: ""
        setContent {
            KimiCommunityTheme {
                MomentDetailScreen(momentId = momentId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentDetailScreen(momentId: String) {
    val context = LocalContext.current
    val viewModel: MomentDetailViewModel = viewModel()
    val moment by viewModel.moment.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val similarMoments by viewModel.similarMoments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isSending by viewModel.isSendingComment.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var replyToId by remember { mutableStateOf<String?>(null) }
    var replyToName by remember { mutableStateOf<String?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 评论分页加载：接近底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreComments(momentId)
    }

    LaunchedEffect(momentId) {
        viewModel.load(momentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("评论 (${comments.size})", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            // 底部输入栏
            Surface(tonalElevation = 3.dp) {
                Column {
                    replyToName?.let { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("回复 @$name", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    replyToId = null
                                    replyToName = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "取消回复", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 点赞作品
                        val isLiked = moment?.interactionStatus?.isLiked ?: false
                        IconButton(onClick = { viewModel.toggleLikeMoment(moment) }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "点赞作品",
                                tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 收藏作品
                        val isCollected = moment?.interactionStatus?.isCollected ?: false
                        IconButton(onClick = { viewModel.toggleFavoriteMoment(moment) }) {
                            Icon(
                                imageVector = if (isCollected) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "收藏作品",
                                tint = if (isCollected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 评论输入框
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("说点什么...") },
                            singleLine = true,
                            enabled = !isSending
                        )

                        // @Kimi 快捷
                        TextButton(onClick = { commentText += "@Kimi " }) {
                            Text("@Kimi")
                        }

                        // 发送
                        IconButton(
                            onClick = {
                                viewModel.sendComment(momentId, commentText.trim(), targetCommentId = replyToId) {
                                    commentText = ""
                                    replyToId = null
                                    replyToName = null
                                }
                            },
                            enabled = commentText.isNotBlank() && !isSending
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 作品预览 WebView (1:1) + 全屏按钮
            item {
                MomentDetailPreview(moment = moment)
            }

            // 作品信息
            item {
                moment?.let { m ->
                    Column {
                        m.content?.title?.let { title ->
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        m.content?.excerpt?.let { excerpt ->
                            Text(excerpt, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (m.content?.title.isNullOrBlank() && m.content?.excerpt.isNullOrBlank()) {
                            m.content?.text?.let { t ->
                                if (t.isNotBlank()) {
                                    Text(t, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = m.author?.userBase?.avatarImage?.url,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(m.author?.userBase?.name ?: "", fontSize = 13.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("点赞 ${m.stat?.likeNum ?: 0} · 评论 ${m.stat?.commentNum ?: 0}", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 评论列表
            item {
                Text("评论", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (comments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无评论，快来抢沙发吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(comments, key = { it.id ?: "" }) { comment ->
                    CommentItem(
                        comment = comment,
                        onToggleLike = { viewModel.toggleLikeComment(comment) },
                        onReply = {
                            replyToId = comment.id
                            replyToName = comment.author?.name
                        },
                        onUserClick = { userId ->
                            userId?.let {
                                context.startActivity(
                                    Intent(context, com.kimi.community.ui.profile.UserProfileActivity::class.java)
                                        .putExtra("userId", it)
                                )
                            }
                        },
                        onReplySub = { subComment ->
                            replyToId = subComment.id
                            replyToName = subComment.author?.name
                        },
                        onOpenLink = { url ->
                            context.startActivity(
                                Intent(context, WebViewActivity::class.java)
                                    .putExtra("url", url)
                                    .putExtra("title", "链接")
                            )
                        }
                    )
                }
            }

            // 评论加载更多指示器
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            // 推荐作品（两列网格）
            if (similarMoments.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("推荐作品", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Column {
                        similarMoments.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { m ->
                                    SimilarMomentItem(
                                        moment = m,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            context.startActivity(
                                                Intent(context, MomentDetailActivity::class.java)
                                                    .putExtra("momentId", m.id ?: "")
                                            )
                                        }
                                    )
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 作品预览：按内容类型自适应展示
 * - 有 CDN 链接（htmlFile.cdnUrl）→ 内嵌 WebView
 * - 有 chatShareId → 内嵌会话享链接
 * - 有图片列表（imageList / media.images）→ 单图展示或多图横滑（HorizontalPager）
 * - 都没有 → 占位符
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MomentDetailPreview(moment: Moment?) {
    val context = LocalContext.current
    val cdnUrl = moment?.chatShareCard?.htmlFile?.cdnUrl
    val chatShareId = moment?.content?.chatShareId
    val shareUrl = if (chatShareId != null) "https://www.kimi.com/chat/share/$chatShareId" else null
    val images = moment?.chatShareCard?.imageList ?: moment?.content?.media?.images

    // 决定全屏跳转的 URL
    val fullscreenUrl = cdnUrl ?: shareUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when {
            // 1. CDN 链接 → 内嵌 WebView
            cdnUrl != null -> {
                com.kimi.community.ui.components.MomentPreview(
                    url = cdnUrl,
                    title = moment?.content?.title ?: "作品预览",
                    onClick = { }
                )
            }
            // 2. 会话享链接 → 内嵌 WebView
            shareUrl != null -> {
                com.kimi.community.ui.components.MomentPreview(
                    url = shareUrl,
                    title = moment?.content?.title ?: "会话分享",
                    onClick = { }
                )
            }
            // 3. 图片列表 → 单图或多图横滑
            !images.isNullOrEmpty() -> {
                if (images.size == 1) {
                    // 单张图片
                    AsyncImage(
                        model = images[0].url ?: images[0].originUrl,
                        contentDescription = "作品图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 多张图片 → HorizontalPager 横滑
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { images.size })
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = images[page].url ?: images[page].originUrl,
                                contentDescription = "作品图片 ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // 页码指示器
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            images.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (pagerState.currentPage == index) Color.White
                                            else Color.White.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
            // 4. 占位符
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无预览", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 全屏按钮（仅当有可全屏的内容时显示）
        if (fullscreenUrl != null) {
            FloatingActionButton(
                onClick = {
                    context.startActivity(
                        Intent(context, WebViewActivity::class.java)
                            .putExtra("url", fullscreenUrl)
                            .putExtra("title", moment?.content?.title ?: "作品预览")
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "全屏", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onToggleLike: () -> Unit,
    onReply: () -> Unit,
    onUserClick: (String?) -> Unit,
    onReplySub: (Comment) -> Unit,
    onOpenLink: (String) -> Unit
) {
    var showAllSubs by remember { mutableStateOf(false) }
    val subs = comment.subComments ?: emptyList()
    val displayedSubs = if (showAllSubs) subs else subs.take(2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = comment.author?.avatarImage?.url,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onUserClick(comment.author?.userId) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        comment.author?.name ?: "",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onUserClick(comment.author?.userId) }
                    )
                    Text(formatTimeString(comment.createTime), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                comment.repliedComment?.user?.name?.let { replyTo ->
                    Text("回复 @$replyTo", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 内容（支持链接点击 → 内置查看器）
                CommentContent(
                    content = comment.content?.text ?: "",
                    onOpenLink = onOpenLink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLiked = comment.interactionStatus?.isLiked ?: false
                    TextButton(onClick = onToggleLike) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text((comment.stat?.likeNum ?: 0).toString(), fontSize = 12.sp)
                    }
                    TextButton(onClick = onReply) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("回复", fontSize = 12.sp)
                    }
                }

                // 楼中楼（缩进，可交互）
                if (subs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        displayedSubs.forEach { sub ->
                            SubCommentItem(
                                subComment = sub,
                                onUserClick = onUserClick,
                                onReply = { onReplySub(sub) },
                                onOpenLink = onOpenLink
                            )
                        }
                        if (subs.size > 2 && !showAllSubs) {
                            Text(
                                "查看全部 ${subs.size} 条回复",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable { showAllSubs = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 楼中楼子评论：可点击用户、可回复 */
@Composable
fun SubCommentItem(
    subComment: Comment,
    onUserClick: (String?) -> Unit,
    onReply: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = subComment.author?.avatarImage?.url,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUserClick(subComment.author?.userId) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    subComment.author?.name ?: "用户",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onUserClick(subComment.author?.userId) }
                )
                Text(formatTimeString(subComment.createTime), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            subComment.repliedComment?.user?.name?.let { replyTo ->
                Text("回复 @$replyTo", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            CommentContent(
                content = subComment.content?.text ?: "",
                onOpenLink = onOpenLink
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLiked = subComment.interactionStatus?.isLiked ?: false
                TextButton(onClick = { /* 子评论点赞 */ }) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text((subComment.stat?.likeNum ?: 0).toString(), fontSize = 11.sp)
                }
                TextButton(onClick = onReply) {
                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("回复", fontSize = 11.sp)
                }
            }
        }
    }
}

/** 评论内容：检测 URL 并将其变为可点击链接，点击打开内置 WebView 查看器 */
@Composable
fun CommentContent(content: String, onOpenLink: (String) -> Unit) {
    val linkPattern = Pattern.compile("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+)")
    val matcher = linkPattern.matcher(content)
    val urls = mutableListOf<String>()
    while (matcher.find()) {
        urls.add(matcher.group())
    }

    val annotated = buildAnnotatedString {
        append(content)
        urls.forEach { url ->
            val index = this.toString().indexOf(url)
            if (index >= 0) {
                addStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    index,
                    index + url.length
                )
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        onClick = { offset ->
            val url = urls.firstOrNull { u ->
                val index = content.indexOf(u)
                offset in index..(index + u.length)
            }
            if (url != null) {
                onOpenLink(url)
            }
        }
    )
}

@Composable
fun SimilarMomentItem(moment: Moment, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Text(
                moment.content?.title ?: "",
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
