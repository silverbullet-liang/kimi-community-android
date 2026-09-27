package com.kimi.community.ui.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.data.model.Comment
import com.kimi.community.data.model.Moment
import com.kimi.community.data.model.Notification
import com.kimi.community.data.model.UserMiniInfo
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MessagesUiState {
    data object Loading : MessagesUiState
    data class Success(val notifications: List<Notification>) : MessagesUiState
    data class Error(val message: String) : MessagesUiState
}

class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()
    private val pageSize = 20

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _moments = MutableStateFlow<List<Moment>>(emptyList())
    val moments: StateFlow<List<Moment>> = _moments.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _users = MutableStateFlow<List<UserMiniInfo>>(emptyList())
    val users: StateFlow<List<UserMiniInfo>> = _users.asStateFlow()

    /** userId → UserMiniInfo 快速查找映射 */
    val userMap: Map<String, UserMiniInfo>
        get() = _users.value.associateBy { it.userId ?: "" }

    /** momentId → Moment 快速查找映射 */
    val momentMap: Map<String, Moment>
        get() = _moments.value.associateBy { it.id ?: "" }

    /** commentId → Comment 快速查找映射 */
    val commentMap: Map<String, Comment>
        get() = _comments.value.associateBy { it.id ?: "" }

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var lastId: String? = null // 分页游标 idGt
    private var hasMore = true

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = MessagesUiState.Loading
            try {
                val response = repository.listNotification(idGt = null, limit = pageSize)
                val items = response.notifications ?: emptyList()
                _notifications.value = items
                _moments.value = response.moments ?: emptyList()
                _comments.value = response.comments ?: emptyList()
                _users.value = response.users ?: emptyList()
                lastId = items.lastOrNull()?.id
                hasMore = items.size >= pageSize
                _uiState.value = MessagesUiState.Success(_notifications.value)
            } catch (e: Exception) {
                if (_notifications.value.isEmpty()) {
                    _uiState.value = MessagesUiState.Error(e.message ?: "加载失败")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
        if (lastId == null || !hasMore) return
        viewModelScope.launch {
            try {
                val response = repository.listNotification(idGt = lastId, limit = pageSize)
                val items = response.notifications ?: emptyList()
                _notifications.value = _notifications.value + items
                _moments.value = _moments.value + (response.moments ?: emptyList())
                _comments.value = _comments.value + (response.comments ?: emptyList())
                _users.value = _users.value + (response.users ?: emptyList())
                lastId = items.lastOrNull()?.id
                hasMore = items.size >= pageSize
            } catch (e: Exception) {
                // 静默
            }
        }
    }

    fun markAllRead() {
        val maxId = _notifications.value
            .mapNotNull { it.id }
            .maxOrNull() ?: return
        viewModelScope.launch {
            try {
                repository.markRead(maxId)
            } catch (e: Exception) {
                // 静默
            }
        }
    }
}
