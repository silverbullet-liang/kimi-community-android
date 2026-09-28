package com.kimi.community.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.data.model.FollowAction
import com.kimi.community.data.model.Moment
import com.kimi.community.data.model.MomentVoteAction
import com.kimi.community.data.model.UserBase
import com.kimi.community.data.model.UserStat
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val userBase: UserBase?) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()

    private val _userBase = MutableStateFlow<UserBase?>(null)
    val userBase: StateFlow<UserBase?> = _userBase.asStateFlow()

    private val _userStat = MutableStateFlow<UserStat?>(null)
    val userStat: StateFlow<UserStat?> = _userStat.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _moments = MutableStateFlow<List<Moment>>(emptyList())
    val moments: StateFlow<List<Moment>> = _moments.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _isSelf = MutableStateFlow(true)
    val isSelf: StateFlow<Boolean> = _isSelf.asStateFlow()

    private var pageToken: String? = null
    private var isLastPage = false
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    fun loadMe() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            _isSelf.value = true
            try {
                val response = repository.getMe()
                _userBase.value = response.userBase
                _userStat.value = response.userStat
                _isFollowing.value = false
                _uiState.value = ProfileUiState.Success(response.userBase)
                response.userBase?.userId?.let { loadUserMoments(it) }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            _isSelf.value = false
            try {
                val response = repository.getUserProfile(userId)
                _userBase.value = response.userBase
                _userStat.value = response.userStat
                _isFollowing.value = false
                _uiState.value = ProfileUiState.Success(response.userBase)
                loadUserMoments(userId)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    private suspend fun loadUserMoments(userId: String) {
        try {
            pageToken = null
            isLastPage = false
            val response = repository.listUserFeeds(userId, pageToken = null, pageSize = 20)
            _moments.value = response.feeds?.mapNotNull { it.moment } ?: emptyList()
            pageToken = response.nextPageToken
            isLastPage = response.isLastPage ?: (response.nextPageToken == null)
        } catch (e: Exception) {
            // 作品加载失败不影响用户信息
        }
    }

    fun loadMoreMoments(userId: String) {
        if (isLastPage || _isLoadingMore.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val response = repository.listUserFeeds(userId, pageToken = pageToken, pageSize = 20)
                val newMoments = response.feeds?.mapNotNull { it.moment } ?: emptyList()
                _moments.value = _moments.value + newMoments
                pageToken = response.nextPageToken
                isLastPage = response.isLastPage ?: (response.nextPageToken == null)
            } catch (e: Exception) {
                // 静默
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleFollow(userId: String) {
        val current = _isFollowing.value
        _isFollowing.value = !current
        viewModelScope.launch {
            try {
                repository.modifyFollow(userId, if (!current) FollowAction.FOLLOW else FollowAction.UNFOLLOW)
            } catch (e: Exception) {
                _isFollowing.value = current
            }
        }
    }

    // ===== 屏蔽用户 =====
    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    fun toggleBlock(userId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val current = _isBlocked.value
        _isBlocked.value = !current
        viewModelScope.launch {
            try {
                val action = if (!current) "BLOCK_ACTION_BLOCK" else "BLOCK_ACTION_UNBLOCK"
                repository.modifyBlock(userId, action)
                onResult(true, if (!current) "已屏蔽该用户" else "已解除屏蔽")
            } catch (e: Exception) {
                _isBlocked.value = current
                onResult(false, e.message ?: "操作失败")
            }
        }
    }

    // ===== 举报用户 =====
    fun reportUser(userId: String, reason: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.createComplaint("COMPLAINT_OBJECT_TYPE_USER", userId, reason)
                onResult(true, "举报已提交，感谢反馈")
            } catch (e: Exception) {
                onResult(false, e.message ?: "举报失败")
            }
        }
    }

    // ===== 点赞动态 =====
    fun toggleLike(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isLiked ?: false
        val newStatus = !current
        _moments.value = _moments.value.map { m ->
            if (m.id == momentId) {
                m.copy(
                    interactionStatus = m.interactionStatus?.copy(isLiked = newStatus),
                    stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (newStatus) 1 else -1)
                )
            } else m
        }
        viewModelScope.launch {
            try {
                repository.voteMoment(momentId, if (newStatus) MomentVoteAction.LIKE else MomentVoteAction.CANCEL_LIKE)
            } catch (e: Exception) {
                _moments.value = _moments.value.map { m ->
                    if (m.id == momentId) {
                        m.copy(
                            interactionStatus = m.interactionStatus?.copy(isLiked = current),
                            stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (current) 1 else -1)
                        )
                    } else m
                }
            }
        }
    }

    // ===== 收藏动态 =====
    fun toggleFavorite(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isCollected ?: false
        val newStatus = !current
        _moments.value = _moments.value.map { m ->
            if (m.id == momentId) {
                m.copy(interactionStatus = m.interactionStatus?.copy(isCollected = newStatus))
            } else m
        }
        viewModelScope.launch {
            try {
                if (newStatus) repository.createFavorite(momentId)
                else repository.deleteFavorite(momentId)
            } catch (e: Exception) {
                _moments.value = _moments.value.map { m ->
                    if (m.id == momentId) {
                        m.copy(interactionStatus = m.interactionStatus?.copy(isCollected = current))
                    } else m
                }
            }
        }
    }
}
