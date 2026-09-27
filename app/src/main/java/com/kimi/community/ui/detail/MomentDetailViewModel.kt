package com.kimi.community.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.data.model.Comment
import com.kimi.community.data.model.CommentVoteAction
import com.kimi.community.data.model.Moment
import com.kimi.community.data.model.MomentVoteAction
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MomentDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()

    private val _moment = MutableStateFlow<Moment?>(null)
    val moment: StateFlow<Moment?> = _moment.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _similarMoments = MutableStateFlow<List<Moment>>(emptyList())
    val similarMoments: StateFlow<List<Moment>> = _similarMoments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment: StateFlow<Boolean> = _isSendingComment.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var pageToken: String? = null
    private var isLastPage = false

    fun load(momentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                loadComments(momentId)
                loadSimilar(momentId)
                // 通过 GetFeed 拉取动态详情（feedType=MOMENT + feedId）
                try {
                    val feedResp = repository.getFeed(momentId)
                    feedResp.feed?.moment?.let { setMoment(it) }
                    if (feedResp.similarMoments.isNullOrEmpty().not()) {
                        _similarMoments.value = feedResp.similarMoments ?: emptyList()
                    }
                } catch (e: Exception) {
                    // 详情拉取失败保留空白，仅展示评论
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setMoment(m: Moment?) {
        _moment.value = m
    }

    private suspend fun loadComments(momentId: String) {
        try {
            val response = repository.listComments(momentId, pageToken = null, pageSize = 20)
            _comments.value = response.comments ?: emptyList()
            pageToken = response.nextPageToken
            isLastPage = response.isLastPage ?: (response.nextPageToken == null)
        } catch (e: Exception) {
            _error.value = e.message ?: "评论加载失败"
        }
    }

    private suspend fun loadSimilar(momentId: String) {
        try {
            val response = repository.listSimilarMoments(momentId)
            _similarMoments.value = response.items ?: emptyList()
        } catch (e: Exception) {
            // 推荐加载失败不影响主流程
        }
    }

    fun loadMoreComments(momentId: String) {
        if (isLastPage || _isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val response = repository.listComments(momentId, pageToken = pageToken, pageSize = 20)
                _comments.value = _comments.value + (response.comments ?: emptyList())
                pageToken = response.nextPageToken
                isLastPage = response.isLastPage ?: (response.nextPageToken == null)
            } catch (e: Exception) {
                // 静默
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleLikeMoment(moment: Moment?) {
        val m = moment ?: return
        val momentId = m.id ?: return
        val current = m.interactionStatus?.isLiked ?: false
        val newStatus = !current
        _moment.value = m.copy(
            interactionStatus = m.interactionStatus?.copy(isLiked = newStatus),
            stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (newStatus) 1 else -1)
        )
        viewModelScope.launch {
            try {
                repository.voteMoment(momentId, if (newStatus) MomentVoteAction.LIKE else MomentVoteAction.CANCEL_LIKE)
            } catch (e: Exception) {
                _moment.value = m
            }
        }
    }

    fun toggleFavoriteMoment(moment: Moment?) {
        val m = moment ?: return
        val momentId = m.id ?: return
        val current = m.interactionStatus?.isCollected ?: false
        val newStatus = !current
        _moment.value = m.copy(interactionStatus = m.interactionStatus?.copy(isCollected = newStatus))
        viewModelScope.launch {
            try {
                if (newStatus) repository.createFavorite(momentId)
                else repository.deleteFavorite(momentId)
            } catch (e: Exception) {
                _moment.value = m
            }
        }
    }

    fun sendComment(momentId: String, content: String, targetCommentId: String? = null, onSuccess: () -> Unit) {
        if (content.isBlank() || _isSendingComment.value) return
        viewModelScope.launch {
            _isSendingComment.value = true
            try {
                val response = repository.createComment(momentId = momentId, text = content, targetCommentId = targetCommentId)
                response.comment?.let { newComment ->
                    _comments.value = listOf(newComment) + _comments.value
                }
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "评论发送失败"
            } finally {
                _isSendingComment.value = false
            }
        }
    }

    fun toggleLikeComment(comment: Comment) {
        val commentId = comment.id ?: return
        val current = comment.interactionStatus?.isLiked ?: false
        val newStatus = !current
        _comments.value = _comments.value.map {
            if (it.id == commentId) {
                it.copy(
                    interactionStatus = it.interactionStatus?.copy(isLiked = newStatus),
                    stat = it.stat?.copy(likeNum = (it.stat?.likeNum ?: 0) + if (newStatus) 1 else -1)
                )
            } else it
        }
        viewModelScope.launch {
            try {
                repository.voteComment(commentId, if (newStatus) CommentVoteAction.LIKE else CommentVoteAction.CANCEL_LIKE)
            } catch (e: Exception) {
                _comments.value = _comments.value.map {
                    if (it.id == commentId) {
                        it.copy(
                            interactionStatus = it.interactionStatus?.copy(isLiked = current),
                            stat = it.stat?.copy(likeNum = (it.stat?.likeNum ?: 0) + if (current) 1 else -1)
                        )
                    } else it
                }
            }
        }
    }

    fun loadSubComments(comment: Comment) {
        val rootCommentId = comment.id ?: return
        viewModelScope.launch {
            try {
                val response = repository.listSubComments(rootCommentId)
                val subs = response.subComments ?: emptyList()
                _comments.value = _comments.value.map {
                    if (it.id == rootCommentId) it.copy(subComments = subs) else it
                }
            } catch (e: Exception) {
                // 静默
            }
        }
    }
}
