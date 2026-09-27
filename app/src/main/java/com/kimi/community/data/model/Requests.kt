package com.kimi.community.data.model

import com.google.gson.annotations.SerializedName

// ===== 枚举常量（严格按 Kimi_API_zh.md 附录 B）=====
object FeedCategory {
    const val RECOMMEND = "FEED_CATEGORY_RECOMMEND" // 热门/推荐
    const val FOLLOW = "FEED_CATEGORY_FOLLOW"
    const val HASHTAG = "FEED_CATEGORY_HASHTAG"
    const val USER_FAVORITE = "FEED_CATEGORY_USER_FAVORITE"
    const val USER_LIKES = "FEED_CATEGORY_USER_LIKES"
    const val USER_PUBLISH = "FEED_CATEGORY_USER_PUBLISH"
}

object FeedType {
    const val UNSPECIFIED = "FEED_TYPE_UNSPECIFIED"
    const val INTEREST_CARD = "FEED_TYPE_INTEREST_CARD"
    const val MOMENT = "FEED_TYPE_MOMENT"
}

object MomentVoteAction {
    const val LIKE = "MOMENT_VOTE_ACTION_LIKE"
    const val CANCEL_LIKE = "MOMENT_VOTE_ACTION_CANCEL_LIKE"
    const val DISLIKE = "MOMENT_VOTE_ACTION_DISLIKE"
    const val CANCEL_DISLIKE = "MOMENT_VOTE_ACTION_CANCEL_DISLIKE"
}

object CommentVoteAction {
    const val LIKE = "COMMENT_VOTE_ACTION_LIKE"
    const val CANCEL_LIKE = "COMMENT_VOTE_ACTION_CANCEL_LIKE"
    const val DISLIKE = "COMMENT_VOTE_ACTION_DISLIKE"
    const val CANCEL_DISLIKE = "COMMENT_VOTE_ACTION_CANCEL_DISLIKE"
}

object FollowAction {
    const val FOLLOW = "FOLLOW_ACTION_FOLLOW"
    const val UNFOLLOW = "FOLLOW_ACTION_UNFOLLOW"
}

object NotificationType {
    const val UNSPECIFIED = "NOTIFICATION_TYPE_UNSPECIFIED"
    const val FAVORITE = "NOTIFICATION_TYPE_FAVORITE"
    const val FOLLOW = "NOTIFICATION_TYPE_FOLLOW"
    const val REPLY_COMMENT = "NOTIFICATION_TYPE_REPLY_COMMENT"
    const val UP_COMMENT = "NOTIFICATION_TYPE_UP_COMMENT"
    const val UP_MOMENT = "NOTIFICATION_TYPE_UP_MOMENT"
    const val COMMENT_MOMENT = "NOTIFICATION_TYPE_COMMENT_MOMENT"
    const val MY_KIMI = "NOTIFICATION_TYPE_MY_KIMI"
    const val TASK = "NOTIFICATION_TYPE_TASK"
    const val SKILL = "NOTIFICATION_TYPE_SKILL"
    const val ACTIVITY = "NOTIFICATION_TYPE_ACTIVITY"
    const val OPERATIONAL = "NOTIFICATION_TYPE_OPERATIONAL"
}

object UserProfileView {
    const val BASIC = "USER_PROFILE_VIEW_BASIC"
    const val FULL = "USER_PROFILE_VIEW_FULL"
    const val LITE = "USER_PROFILE_VIEW_LITE"
}

// ===== Feed Requests/Responses =====
/** ListFeedsRequest — sessionId, pageSize, pageToken, category, userId, pinnedMomentId, hashtagId */
data class ListFeedsRequest(
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null,
    @SerializedName("category") val category: String = FeedCategory.RECOMMEND,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("pinnedMomentId") val pinnedMomentId: String? = null,
    @SerializedName("hashtagId") val hashtagId: String? = null
)

/** ListFeedsResponse — nextPageToken, isLastPage, feeds */
data class ListFeedsResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("isLastPage") val isLastPage: Boolean? = null,
    @SerializedName("feeds") val feeds: List<Feed>? = null
)

/** GetFeedRequest — feedType, feedId */
data class GetFeedRequest(
    @SerializedName("feedType") val feedType: String = FeedType.MOMENT,
    @SerializedName("feedId") val feedId: String
)

/** GetFeedResponse — feed, similarMoments */
data class GetFeedResponse(
    @SerializedName("feed") val feed: Feed? = null,
    @SerializedName("similarMoments") val similarMoments: List<Moment>? = null
)

// ===== Moment Requests/Responses =====
/** VoteMomentRequest — momentId, voteAction(MOMENT_VOTE_ACTION_*) */
data class VoteMomentRequest(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("voteAction") val voteAction: String
)

/** VoteMomentResponse — 空消息 */
data class VoteMomentResponse(val ignored: Boolean = true)

/** ListSimilarMomentsRequest — momentId, pageSize, pageToken */
data class ListSimilarMomentsRequest(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("pageSize") val pageSize: Int = 10,
    @SerializedName("pageToken") val pageToken: String? = null
)

/** ListSimilarMomentsResponse — nextPageToken, items */
data class ListSimilarMomentsResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<Moment>? = null
)

/** GetChatShareCardRequest — chatShareId */
data class GetChatShareCardRequest(
    @SerializedName("chatShareId") val chatShareId: String
)

/** GetChatShareCardResponse — chatShareCard */
data class GetChatShareCardResponse(
    @SerializedName("chatShareCard") val chatShareCard: ChatShareCard? = null
)

// ===== Favorite Requests/Responses =====
/** CreateFavoriteRequest — momentId */
data class CreateFavoriteRequest(
    @SerializedName("momentId") val momentId: String
)

/** CreateFavoriteResponse — 空消息 */
data class CreateFavoriteResponse(val ignored: Boolean = true)

/** DeleteFavoriteRequest — momentId */
data class DeleteFavoriteRequest(
    @SerializedName("momentId") val momentId: String
)

/** DeleteFavoriteResponse — 空消息 */
data class DeleteFavoriteResponse(val ignored: Boolean = true)

// ===== Comment Requests/Responses =====
/** ListCommentsRequest — momentId, pageSize, pageToken, rootCommentId, pinnedCommentId */
data class ListCommentsRequest(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null,
    @SerializedName("rootCommentId") val rootCommentId: String? = null,
    @SerializedName("pinnedCommentId") val pinnedCommentId: String? = null
)

/** ListCommentsResponse — nextPageToken, pageStat, isLastPage, pinnedCommentId, comments */
data class ListCommentsResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("pageStat") val pageStat: PageStat? = null,
    @SerializedName("isLastPage") val isLastPage: Boolean? = null,
    @SerializedName("pinnedCommentId") val pinnedCommentId: String? = null,
    @SerializedName("comments") val comments: List<Comment>? = null
)

/** CreateCommentRequest — momentId, targetCommentId, content(CommentContent{text}), mentions, links */
data class CreateCommentRequest(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("targetCommentId") val targetCommentId: String? = null,
    @SerializedName("content") val content: CommentContent,
    @SerializedName("mentions") val mentions: List<MentionedUser>? = null,
    @SerializedName("links") val links: List<Link>? = null
)

/** CreateCommentResponse — comment */
data class CreateCommentResponse(
    @SerializedName("comment") val comment: Comment? = null
)

/** DeleteCommentRequest — commentId */
data class DeleteCommentRequest(
    @SerializedName("commentId") val commentId: String
)

/** DeleteCommentResponse — 空消息 */
data class DeleteCommentResponse(val ignored: Boolean = true)

/** VoteCommentRequest — commentId, voteAction(COMMENT_VOTE_ACTION_*) */
data class VoteCommentRequest(
    @SerializedName("commentId") val commentId: String,
    @SerializedName("voteAction") val voteAction: String
)

/** VoteCommentResponse — 空消息 */
data class VoteCommentResponse(val ignored: Boolean = true)

/** ListSubCommentsRequest — rootCommentId, pageSize, pageToken */
data class ListSubCommentsRequest(
    @SerializedName("rootCommentId") val rootCommentId: String,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null
)

/** ListSubCommentsResponse — nextPageToken, subComments */
data class ListSubCommentsResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("subComments") val subComments: List<Comment>? = null
)

// ===== Notification Requests/Responses =====
/** ListNotificationRequest — idGt, idLt, limit */
data class ListNotificationRequest(
    @SerializedName("idGt") val idGt: String? = null,
    @SerializedName("idLt") val idLt: String? = null,
    @SerializedName("limit") val limit: Int = 20
)

/** ListNotificationResponse — notifications, moments, comments, users */
data class ListNotificationResponse(
    @SerializedName("notifications") val notifications: List<Notification>? = null,
    @SerializedName("moments") val moments: List<Moment>? = null,
    @SerializedName("comments") val comments: List<Comment>? = null,
    @SerializedName("users") val users: List<UserMiniInfo>? = null
)

/** MarkReadRequest — idLte */
data class MarkReadRequest(
    @SerializedName("idLte") val idLte: String
)

/** MarkReadResponse — 空消息 */
data class MarkReadResponse(val ignored: Boolean = true)

// ===== User Requests/Responses =====
/** GetMeRequest — view(USER_PROFILE_VIEW_*) */
data class GetMeRequest(
    @SerializedName("view") val view: String = UserProfileView.BASIC
)

/** GetMeResponse — userBase, userStat, userInfo */
data class GetMeResponse(
    @SerializedName("userBase") val userBase: UserBase? = null,
    @SerializedName("userStat") val userStat: UserStat? = null,
    @SerializedName("userInfo") val userInfo: UserInfo? = null
)

/** GetUserProfileRequest — userId */
data class GetUserProfileRequest(
    @SerializedName("userId") val userId: String
)

/** GetUserProfileResponse — userBase, userStat, interactionStatus, userInfo */
data class GetUserProfileResponse(
    @SerializedName("userBase") val userBase: UserBase? = null,
    @SerializedName("userStat") val userStat: UserStat? = null,
    @SerializedName("interactionStatus") val interactionStatus: InteractionStatus? = null,
    @SerializedName("userInfo") val userInfo: UserInfo? = null
)

// ===== Follow Requests/Responses =====
/** ModifyFollowRequest — userId, action(FOLLOW_ACTION_*) */
data class ModifyFollowRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("action") val action: String
)

/** ModifyFollowResponse — userId, followStatus */
data class ModifyFollowResponse(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("followStatus") val followStatus: String? = null
)

// ===== Search Requests/Responses =====
/** SearchMomentsRequest — query, pageSize, pageToken */
data class SearchMomentsRequest(
    @SerializedName("query") val query: String,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null
)

/** SearchMomentsResponse — nextPageToken, moments(items), relatedMoments */
data class SearchMomentsResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("moments") val moments: List<SearchMomentsResponseItem>? = null,
    @SerializedName("relatedMoments") val relatedMoments: List<SearchMomentsResponseItem>? = null
)

/** SearchUsersRequest — query, pageSize, pageToken */
data class SearchUsersRequest(
    @SerializedName("query") val query: String,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("pageToken") val pageToken: String? = null
)

/** SearchUsersResponse — nextPageToken, users(items), relatedUsers */
data class SearchUsersResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("users") val users: List<SearchUsersResponseItem>? = null,
    @SerializedName("relatedUsers") val relatedUsers: List<SearchUsersResponseItem>? = null
)

// ===== Block（拉黑）Requests/Responses =====
/** ModifyBlockRequest — userId, action(BLOCK_ACTION_BLOCK/UNBLOCK) */
data class ModifyBlockRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("action") val action: String
)

/** ModifyBlockResponse — userId, blockStatus */
data class ModifyBlockResponse(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("blockStatus") val blockStatus: String? = null
)

// ===== Mute（屏蔽内容）Requests/Responses =====
/** MuteRequest — objectType(MUTE_OBJECT_TYPE_*), objectId, reason */
data class MuteRequest(
    @SerializedName("objectType") val objectType: String,
    @SerializedName("objectId") val objectId: String,
    @SerializedName("reason") val reason: String? = null
)

/** MuteResponse — msg */
data class MuteResponse(
    @SerializedName("msg") val msg: String? = null
)

// ===== Complaint（举报）Requests/Responses =====
/** CreateComplaintRequest — objectType(COMPLAINT_OBJECT_TYPE_*), objectId, reason */
data class CreateComplaintRequest(
    @SerializedName("objectType") val objectType: String,
    @SerializedName("objectId") val objectId: String,
    @SerializedName("reason") val reason: String? = null
)

/** CreateComplaintResponse — 空消息 */
data class CreateComplaintResponse(val ignored: Boolean = true)
