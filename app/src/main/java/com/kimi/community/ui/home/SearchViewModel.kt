package com.kimi.community.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.data.model.Moment
import com.kimi.community.data.model.MomentVoteAction
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val moments: List<Moment>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var pageToken: String? = null
    private var isLastPage = false
    private val results = mutableListOf<Moment>()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = SearchUiState.Loading
            results.clear()
            pageToken = null
            isLastPage = false
            try {
                val response = repository.searchMoments(q, pageToken = null, pageSize = 20)
                results.addAll((response.moments ?: emptyList()).mapNotNull { it.moment })
                pageToken = response.nextPageToken
                isLastPage = response.nextPageToken == null
                _uiState.value = SearchUiState.Success(results.toList())
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "搜索失败")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadMore() {
        if (_isSearching.value || isLastPage) return
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val response = repository.searchMoments(q, pageToken = pageToken, pageSize = 20)
                results.addAll((response.moments ?: emptyList()).mapNotNull { it.moment })
                pageToken = response.nextPageToken
                isLastPage = response.nextPageToken == null
                _uiState.value = SearchUiState.Success(results.toList())
            } catch (e: Exception) {
                // 静默
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun toggleLike(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isLiked ?: false
        val newStatus = !current
        updateMoment(momentId) { m ->
            m.copy(
                interactionStatus = m.interactionStatus?.copy(isLiked = newStatus),
                stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (newStatus) 1 else -1)
            )
        }
        viewModelScope.launch {
            try {
                repository.voteMoment(momentId, if (newStatus) MomentVoteAction.LIKE else MomentVoteAction.CANCEL_LIKE)
            } catch (e: Exception) {
                updateMoment(momentId) { m ->
                    m.copy(
                        interactionStatus = m.interactionStatus?.copy(isLiked = current),
                        stat = m.stat?.copy(likeNum = (m.stat?.likeNum ?: 0) + if (current) 1 else -1)
                    )
                }
            }
        }
    }

    fun toggleFavorite(moment: Moment) {
        val momentId = moment.id ?: return
        val current = moment.interactionStatus?.isCollected ?: false
        val newStatus = !current
        updateMoment(momentId) { m ->
            m.copy(interactionStatus = m.interactionStatus?.copy(isCollected = newStatus))
        }
        viewModelScope.launch {
            try {
                if (newStatus) repository.createFavorite(momentId)
                else repository.deleteFavorite(momentId)
            } catch (e: Exception) {
                updateMoment(momentId) { m ->
                    m.copy(interactionStatus = m.interactionStatus?.copy(isCollected = current))
                }
            }
        }
    }

    private fun updateMoment(momentId: String, transform: (Moment) -> Moment) {
        val idx = results.indexOfFirst { it.id == momentId }
        if (idx >= 0) {
            results[idx] = transform(results[idx])
            _uiState.value = SearchUiState.Success(results.toList())
        }
    }
}
