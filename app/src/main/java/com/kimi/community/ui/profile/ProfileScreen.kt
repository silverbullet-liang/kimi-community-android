package com.kimi.community.ui.profile

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimi.community.ui.components.MomentCard
import com.kimi.community.ui.detail.MomentDetailActivity
import com.kimi.community.ui.webview.WebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel()
    val userBase by viewModel.userBase.collectAsState()
    val moments by viewModel.moments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMe()
    }

    if (showLogin) {
        context.startActivity(
            Intent(context, WebViewActivity::class.java)
                .putExtra("url", "https://www.kimi.com")
                .putExtra("title", "登录 Kimi")
                .putExtra("captureToken", true)
        )
        showLogin = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("个人主页", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
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
                        Text("未登录或加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showLogin = true }) {
                            Text("去登录")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadMe() }) {
                            Text("重试")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        UserProfileCard(
                            userBase = userBase,
                            userStat = viewModel.userStat.collectAsState().value
                        )
                    }

                    if (moments.isNotEmpty()) {
                        item {
                            Text("我的作品", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                onAuthorClick = { }
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
}
