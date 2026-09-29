package com.kimi.community.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimi.community.ui.components.MomentCard
import com.kimi.community.ui.create.CreateMomentActivity
import com.kimi.community.ui.detail.MomentDetailActivity
import com.kimi.community.ui.profile.UserProfileActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    val feeds by viewModel.feeds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val category by viewModel.category.collectAsState()
    val listState = rememberLazyListState()

    // 分页加载：监听滚动位置，接近底部时自动加载更多
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

    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        SearchScreen(onBack = { showSearch = false })
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("社区", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, CreateMomentActivity::class.java))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "发布")
                    }
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab 切换（热门 / 我关注的）
            TabRow(selectedTabIndex = if (category == com.kimi.community.data.model.FeedCategory.FOLLOW) 1 else 0) {
                Tab(
                    selected = category == com.kimi.community.data.model.FeedCategory.RECOMMEND,
                    onClick = { viewModel.switchCategory(com.kimi.community.data.model.FeedCategory.RECOMMEND) },
                    text = { Text("热门") }
                )
                Tab(
                    selected = category == com.kimi.community.data.model.FeedCategory.FOLLOW,
                    onClick = { viewModel.switchCategory(com.kimi.community.data.model.FeedCategory.FOLLOW) },
                    text = { Text("我关注的") }
                )
            }

            // 信息流（下拉刷新 + 上拉加载）
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is HomeUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.refresh() }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        if (feeds.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂无数据，下拉刷新试试")
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(feeds, key = { it.feedId ?: it.moment?.id ?: "" }) { feed ->
                                    val moment = feed.moment
                                    if (moment != null) {
                                        MomentCard(
                                            moment = moment,
                                            onClick = {
                                                context.startActivity(
                                                    Intent(context, MomentDetailActivity::class.java)
                                                        .putExtra("momentId", moment.id ?: "")
                                                        .putExtra("feedId", feed.feedId ?: "")
                                                )
                                            },
                                            onAuthorClick = {
                                                moment.author?.userBase?.userId?.let { userId ->
                                                    context.startActivity(
                                                        Intent(context, UserProfileActivity::class.java)
                                                            .putExtra("userId", userId)
                                                    )
                                                }
                                            },
                                            onLike = { viewModel.toggleLike(moment) },
                                            onFavorite = { viewModel.toggleFavorite(moment) }
                                        )
                                    }
                                }

                                // 分页加载指示器
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
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel()
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val listState = rememberLazyListState()

    // 分页加载：监听滚动位置，接近底部时自动加载更多
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
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("搜索动态、用户、话题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.search() },
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        Text("搜索")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("输入关键词搜索动态")
                    }
                }
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.search() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                is SearchUiState.Success -> {
                    if (state.moments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无搜索结果")
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.moments, key = { it.id ?: "" }) { moment ->
                                MomentCard(
                                    moment = moment,
                                    onClick = {
                                        context.startActivity(
                                            Intent(context, MomentDetailActivity::class.java)
                                                .putExtra("momentId", moment.id ?: "")
                                        )
                                    },
                                    onAuthorClick = {
                                        moment.author?.userBase?.userId?.let { userId ->
                                            context.startActivity(
                                                Intent(context, UserProfileActivity::class.java)
                                                    .putExtra("userId", userId)
                                            )
                                        }
                                    },
                                    onLike = { viewModel.toggleLike(moment) },
                                    onFavorite = { viewModel.toggleFavorite(moment) }
                                )
                            }
                            if (isSearching) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
