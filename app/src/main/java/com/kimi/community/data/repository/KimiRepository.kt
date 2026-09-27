package com.kimi.community.data.repository

import com.kimi.community.data.api.ApiException
import com.kimi.community.data.api.KimiApiClient
import com.kimi.community.data.api.KimiApiService
import com.kimi.community.data.model.*
import com.kimi.community.ui.debug.DebugLogStore

/**
 * Kimi 社区数据仓库。
 * 错误处理由 ApiErrorInterceptor 统一完成（401/403 刷新重放、429/503/504 退避重试、其余抛 ApiException）。
 * 成功响应直接是业务对象（无 envelope 包装），Repository 层无需解包。
 */
class KimiRepository {

    private val api: KimiApiService = KimiApiClient.create(KimiApiService::class.java)

    // ===== Feed =====
    suspend fun listFeeds(category: String = FeedCategory.RECOMMEND, pageToken: String? = null, pageSize: Int = 20): ListFeedsResponse {
        return api.listFeeds(ListFeedsRequest(category = category, pageToken = pageToken, pageSize = pageSize))
    }

    /** 用户主页作品流（category=USER_PUBLISH + userId） */
    suspend fun listUserFeeds(userId: String, pageToken: String? = null, pageSize: Int = 20): ListFeedsResponse {
        return api.listFeeds(
            ListFeedsRequest(category = FeedCategory.USER_PUBLISH, userId = userId, pageToken = pageToken, pageSize = pageSize)
        )
    }

    suspend fun getFeed(feedId: String, feedType: String = FeedType.MOMENT): GetFeedResponse {
        return api.getFeed(GetFeedRequest(feedType = feedType, feedId = feedId))
    }

    // ===== Moment =====
    suspend fun voteMoment(momentId: String, voteAction: String): VoteMomentResponse {
        return api.voteMoment(VoteMomentRequest(momentId = momentId, voteAction = voteAction))
    }

    suspend fun listSimilarMoments(momentId: String, pageToken: String? = null): ListSimilarMomentsResponse {
        return api.listSimilarMoments(ListSimilarMomentsRequest(momentId = momentId, pageToken = pageToken))
    }

    // ===== Favorite =====
    suspend fun createFavorite(momentId: String): CreateFavoriteResponse {
        return api.createFavorite(CreateFavoriteRequest(momentId = momentId))
    }

    suspend fun deleteFavorite(momentId: String): DeleteFavoriteResponse {
        return api.deleteFavorite(DeleteFavoriteRequest(momentId = momentId))
    }

    // ===== Comment =====
    suspend fun listComments(momentId: String, pageToken: String? = null, pageSize: Int = 20): ListCommentsResponse {
        return api.listComments(ListCommentsRequest(momentId = momentId, pageToken = pageToken, pageSize = pageSize))
    }

    suspend fun createComment(momentId: String, text: String, targetCommentId: String? = null): CreateCommentResponse {
        return api.createComment(
            CreateCommentRequest(momentId = momentId, targetCommentId = targetCommentId, content = CommentContent(text = text))
        )
    }

    suspend fun deleteComment(commentId: String): DeleteCommentResponse {
        return api.deleteComment(DeleteCommentRequest(commentId = commentId))
    }

    suspend fun voteComment(commentId: String, voteAction: String): VoteCommentResponse {
        return api.voteComment(VoteCommentRequest(commentId = commentId, voteAction = voteAction))
    }

    suspend fun listSubComments(rootCommentId: String, pageToken: String? = null, pageSize: Int = 20): ListSubCommentsResponse {
        return api.listSubComments(ListSubCommentsRequest(rootCommentId = rootCommentId, pageToken = pageToken, pageSize = pageSize))
    }

    // ===== Notification =====
    suspend fun listNotification(idGt: String? = null, limit: Int = 20): ListNotificationResponse {
        return api.listNotification(ListNotificationRequest(idGt = idGt, limit = limit))
    }

    suspend fun markRead(idLte: String): MarkReadResponse {
        return api.markRead(MarkReadRequest(idLte = idLte))
    }

    // ===== User =====
    suspend fun getMe(): GetMeResponse {
        return api.getMe(GetMeRequest())
    }

    suspend fun getUserProfile(userId: String): GetUserProfileResponse {
        return api.getUserProfile(GetUserProfileRequest(userId = userId))
    }

    // ===== Follow =====
    suspend fun modifyFollow(userId: String, action: String): ModifyFollowResponse {
        return api.modifyFollow(ModifyFollowRequest(userId = userId, action = action))
    }

    // ===== Block（拉黑用户）=====
    suspend fun modifyBlock(userId: String, action: String): ModifyBlockResponse {
        return api.modifyBlock(ModifyBlockRequest(userId = userId, action = action))
    }

    // ===== Mute（屏蔽内容）=====
    suspend fun mute(objectType: String, objectId: String, reason: String? = null): MuteResponse {
        return api.mute(MuteRequest(objectType = objectType, objectId = objectId, reason = reason))
    }

    // ===== Complaint（举报）=====
    suspend fun createComplaint(objectType: String, objectId: String, reason: String? = null): CreateComplaintResponse {
        return api.createComplaint(CreateComplaintRequest(objectType = objectType, objectId = objectId, reason = reason))
    }

    // ===== Search =====
    suspend fun searchMoments(query: String, pageToken: String? = null, pageSize: Int = 20): SearchMomentsResponse {
        return api.searchMoments(SearchMomentsRequest(query = query, pageToken = pageToken, pageSize = pageSize))
    }

    suspend fun searchUsers(query: String, pageToken: String? = null, pageSize: Int = 20): SearchUsersResponse {
        return api.searchUsers(SearchUsersRequest(query = query, pageToken = pageToken, pageSize = pageSize))
    }

    // ===== Auth =====
    suspend fun logout(): Boolean {
        return try {
            KimiApiClient.createAuth(KimiApiService::class.java).logout(LogoutRequest())
            true
        } catch (e: ApiException) {
            // 404 等业务错误也视为成功（本地清除登录态即可）
            DebugLogStore.log("AUTH", "Logout 业务错误 code=${e.code}（忽略，本地清除）")
            true
        } catch (e: Exception) {
            DebugLogStore.log("AUTH", "Logout 接口异常（忽略，本地清除）: ${e.message}")
            true // 网络失败也允许本地登出
        }
    }
}
