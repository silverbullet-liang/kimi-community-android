package com.kimi.community.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.KimiApp
import com.kimi.community.data.api.ApiException
import com.kimi.community.data.model.Feed
import com.kimi.community.data.model.FeedCategory
import com.kimi.community.data.model.Moment
import com.kimi.community.data.model.MomentVoteAction
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val feeds: List<Feed>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()

    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()

    private val _category = MutableStateFlow(FeedCategory.RECOMMEND)
    val category: StateFlow<String> = _category.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var pageToken: String? = null
    private var isLastPage = false

    init {
        refresh()
    }

    fun switchCategory(category: String) {
        if (_category.value == category) return
        _category.value = category
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = HomeUiState.Loading
            try {
                val response = repository.listFeeds(
                    category = _category.value,
                    pageToken = null,
                    pageSize = 20
                )
                _feeds.value = response.feeds ?: emptyList()
                pageToken = response.nextPageToken
                isLastPage = response.isLastPage ?: (response.nextPageToken == null)
                _uiState.value = HomeUiState.Success(_feeds.value)
            } catch (e: ApiException) {
                if (_feeds.value.isEmpty()) {
                    _uiState.value = HomeUiState.Error(e.friendlyMessage)
                }
            } catch (e: Exception) {
                if (_feeds.value.isEmpty()) {
                    _uiState.value = HomeUiState.Error(e.message ?: "加载失败")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || isLastPage) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val response = repository.listFeeds(
                    category = _category.value,
                    pageToken = pageToken,
                    pageSize = 20
                )
                val newFeeds = response.feeds ?: emptyList()
                _feeds.value = _feeds.value + newFeeds
                pageToken = response.nextPageToken
                isLastPage = response.isLastPage ?: (response.nextPageToken == null)
            } catch (e: Exception) {
                // 静默失败，保留已加载数据
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleLike(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isLiked ?: false
        val newStatus = !current
        // 乐观更新
        _feeds.value = _feeds.value.map { feed ->
            val m = feed.moment
            if (m?.id == momentId) {
                feed.copy(
                    moment = m.copy(
                        interactionStatus = m.interactionStatus?.copy(isLiked = newStatus),
                        stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (newStatus) 1 else -1)
                    )
                )
            } else feed
        }
        viewModelScope.launch {
            try {
                repository.voteMoment(
                    momentId,
                    if (newStatus) MomentVoteAction.LIKE else MomentVoteAction.CANCEL_LIKE
                )
            } catch (e: Exception) {
                // 失败回滚
                _feeds.value = _feeds.value.map { feed ->
                    val m = feed.moment
                    if (m?.id == momentId) {
                        feed.copy(
                            moment = m.copy(
                                interactionStatus = m.interactionStatus?.copy(isLiked = current),
                                stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (current) 1 else -1)
                            )
                        )
                    } else feed
                }
            }
        }
    }

    fun toggleFavorite(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isCollected ?: false
        val newStatus = !current
        _feeds.value = _feeds.value.map { feed ->
            val m = feed.moment
            if (m?.id == momentId) {
                feed.copy(
                    moment = m.copy(
                        interactionStatus = m.interactionStatus?.copy(isCollected = newStatus)
                    )
                )
            } else feed
        }
        viewModelScope.launch {
            try {
                if (newStatus) {
                    repository.createFavorite(momentId)
                } else {
                    repository.deleteFavorite(momentId)
                }
            } catch (e: Exception) {
                // 失败回滚
                _feeds.value = _feeds.value.map { feed ->
                    val m = feed.moment
                    if (m?.id == momentId) {
                        feed.copy(
                            moment = m.copy(
                                interactionStatus = m.interactionStatus?.copy(isCollected = current)
                            )
                        )
                    } else feed
                }
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return runBlocking { (getApplication<KimiApp>()).userPrefs.accessToken.first()?.isNotEmpty() == true }
    }
}
