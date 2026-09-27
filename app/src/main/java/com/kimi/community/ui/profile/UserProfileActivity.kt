package com.kimi.community.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kimi.community.data.model.UserBase
import com.kimi.community.data.model.UserStat
import com.kimi.community.ui.components.MomentCard
import com.kimi.community.ui.detail.MomentDetailActivity
import com.kimi.community.ui.theme.KimiCommunityTheme
import kotlinx.coroutines.launch

class UserProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        setContent {
            KimiCommunityTheme {
                UserProfileScreen(userId = userId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: ProfileViewModel = viewModel()
    val userBase by viewModel.userBase.collectAsState()
    val userStat by viewModel.userStat.collectAsState()
    val moments by viewModel.moments.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val isSelf by viewModel.isSelf.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 用户作品分页加载
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreMoments(userId)
    }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("用户主页", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isSelf) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "解除屏蔽" else "屏蔽用户") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleBlock(userId) { success, msg ->
                                            if (msg != null) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("举报用户") },
                                    onClick = {
                                        showMenu = false
                                        showReportDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadUser(userId) }) {
                            Text("重试")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        UserProfileCard(
                            userBase = userBase,
                            userStat = userStat,
                            showFollowButton = !isSelf,
                            isFollowing = isFollowing,
                            onFollowClick = { viewModel.toggleFollow(userId) }
                        )
                    }

                    if (moments.isNotEmpty()) {
                        item {
                            Text("TA 的作品", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        items(moments, key = { it.id ?: "" }) { moment ->
                            MomentCard(
                                moment = moment,
                                onClick = {
                                    context.startActivity(
                                        Intent(context, MomentDetailActivity::class.java)
                                            .putExtra("momentId", moment.id ?: "")
                                    )
                                },
                                onAuthorClick = { },
                                onLike = { viewModel.toggleLike(moment) },
                                onFavorite = { viewModel.toggleFavorite(moment) }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("暂无作品", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 举报对话框
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("举报用户") },
            text = {
                Column {
                    Text("请选择或输入举报原因：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("例如：垃圾广告、人身攻击、违规内容等") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            viewModel.reportUser(userId, reportReason) { success, msg ->
                                if (msg != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                            showReportDialog = false
                            reportReason = ""
                        }
                    },
                    enabled = reportReason.isNotBlank()
                ) {
                    Text("提交")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    reportReason = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun UserProfileCard(
    userBase: UserBase?,
    userStat: UserStat?,
    showFollowButton: Boolean = false,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = userBase?.avatarImage?.url,
                contentDescription = "头像",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userBase?.name ?: "未知用户",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            userBase?.bio?.let { bio ->
                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bio,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(count = userStat?.followingNum ?: 0, label = "关注")
                StatColumn(count = userStat?.followerNum ?: 0, label = "粉丝")
                StatColumn(count = userStat?.momentNum ?: 0, label = "作品")
            }

            if (showFollowButton) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isFollowing) {
                        ButtonDefaults.outlinedButtonColors()
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (isFollowing) "已关注" else "关注")
                }
            }
        }
    }
}

@Composable
private fun StatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
