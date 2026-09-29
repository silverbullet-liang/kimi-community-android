package com.kimi.community.data.api

import com.kimi.community.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Kimi 社区 API 接口（严格按 Kimi_API_zh.md）。
 * 成功响应直接是业务对象（无 envelope 包装）；错误由 ApiErrorInterceptor 解析并抛 ApiException。
 * 社区业务接口走 /apiv2 前缀；鉴权接口（RefreshToken/Logout）走 /api 前缀。
 */
interface KimiApiService {

    // ===== FeedService（apiv2）=====
    @POST("apiv2/moment.v1.FeedService/ListFeeds")
    suspend fun listFeeds(@Body request: ListFeedsRequest): ListFeedsResponse

    @POST("apiv2/moment.v1.FeedService/GetFeed")
    suspend fun getFeed(@Body request: GetFeedRequest): GetFeedResponse

    @POST("apiv2/moment.v1.FeedService/ListFollowUnread")
    suspend fun listFollowUnread(): ListFollowUnreadResponse

    @POST("apiv2/moment.v1.FeedService/MarkReadForFollow")
    suspend fun markReadForFollow(@Body request: MarkReadForFollowRequest): MarkReadForFollowResponse

    // ===== MomentService =====
    @POST("apiv2/moment.v1.MomentService/VoteMoment")
    suspend fun voteMoment(@Body request: VoteMomentRequest): VoteMomentResponse

    @POST("apiv2/moment.v1.MomentService/ListSimilarMoments")
    suspend fun listSimilarMoments(@Body request: ListSimilarMomentsRequest): ListSimilarMomentsResponse

    @POST("apiv2/moment.v1.MomentService/GetChatShareCard")
    suspend fun getChatShareCard(@Body request: GetChatShareCardRequest): GetChatShareCardResponse

    @POST("apiv2/moment.v1.MomentService/CreateMoment")
    suspend fun createMoment(@Body request: CreateMomentRequest): CreateMomentResponse

    @POST("apiv2/moment.v1.MomentService/DeleteMoment")
    suspend fun deleteMoment(@Body request: DeleteMomentRequest): DeleteMomentResponse

    @POST("apiv2/moment.v1.MomentService/ListMomentAskKimiQuestions")
    suspend fun listMomentAskKimiQuestions(@Body request: ListMomentAskKimiQuestionsRequest): ListMomentAskKimiQuestionsResponse

    // ===== UploadService =====
    @POST("apiv2/moment.v1.UploadService/GeneratePresignedURL")
    suspend fun generatePresignedURL(@Body request: GeneratePresignedURLRequest): GeneratePresignedURLResponse

    @POST("apiv2/moment.v1.UploadService/CreateFile")
    suspend fun createFile(@Body request: CreateFileRequest): CreateFileResponse

    // ===== HashtagService =====
    @POST("apiv2/moment.v1.HashtagService/ListHashtags")
    suspend fun listHashtags(@Body request: ListHashtagsRequest): ListHashtagsResponse

    @POST("apiv2/moment.v1.HashtagService/GetHashtag")
    suspend fun getHashtag(@Body request: GetHashtagRequest): GetHashtagResponse

    @POST("apiv2/moment.v1.HashtagService/RecommendHashtags")
    suspend fun recommendHashtags(): RecommendHashtagsResponse

    // ===== FavoriteService =====
    @POST("apiv2/moment.v1.FavoriteService/CreateFavorite")
    suspend fun createFavorite(@Body request: CreateFavoriteRequest): CreateFavoriteResponse

    @POST("apiv2/moment.v1.FavoriteService/DeleteFavorite")
    suspend fun deleteFavorite(@Body request: DeleteFavoriteRequest): DeleteFavoriteResponse

    // ===== CommentService =====
    @POST("apiv2/moment.v1.CommentService/ListComments")
    suspend fun listComments(@Body request: ListCommentsRequest): ListCommentsResponse

    @POST("apiv2/moment.v1.CommentService/CreateComment")
    suspend fun createComment(@Body request: CreateCommentRequest): CreateCommentResponse

    @POST("apiv2/moment.v1.CommentService/DeleteComment")
    suspend fun deleteComment(@Body request: DeleteCommentRequest): DeleteCommentResponse

    @POST("apiv2/moment.v1.CommentService/VoteComment")
    suspend fun voteComment(@Body request: VoteCommentRequest): VoteCommentResponse

    @POST("apiv2/moment.v1.CommentService/ListSubComments")
    suspend fun listSubComments(@Body request: ListSubCommentsRequest): ListSubCommentsResponse

    // ===== NotificationService =====
    @POST("apiv2/moment.v1.NotificationService/ListNotification")
    suspend fun listNotification(@Body request: ListNotificationRequest): ListNotificationResponse

    @POST("apiv2/moment.v1.NotificationService/MarkRead")
    suspend fun markRead(@Body request: MarkReadRequest): MarkReadResponse

    @POST("apiv2/moment.v1.NotificationService/GetReminderStatus")
    suspend fun getReminderStatus(): GetReminderStatusResponse

    @POST("apiv2/moment.v1.NotificationService/ListReminders")
    suspend fun listReminders(@Body request: ListRemindersRequest): ListRemindersResponse

    @POST("apiv2/moment.v1.NotificationService/MarkReadForReminder")
    suspend fun markReadForReminder(@Body request: MarkReadForReminderRequest): MarkReadForReminderResponse

    // ===== UserService =====
    @POST("apiv2/moment.v1.UserService/GetMe")
    suspend fun getMe(@Body request: GetMeRequest): GetMeResponse

    @POST("apiv2/moment.v1.UserService/GetUserProfile")
    suspend fun getUserProfile(@Body request: GetUserProfileRequest): GetUserProfileResponse

    // ===== FollowService =====
    @POST("apiv2/moment.v1.FollowService/ModifyFollow")
    suspend fun modifyFollow(@Body request: ModifyFollowRequest): ModifyFollowResponse

    @POST("apiv2/moment.v1.FollowService/ModifyBlock")
    suspend fun modifyBlock(@Body request: ModifyBlockRequest): ModifyBlockResponse

    @POST("apiv2/moment.v1.FollowService/ListFollows")
    suspend fun listFollows(@Body request: ListFollowsRequest): ListFollowsResponse

    // ===== MuteService =====
    @POST("apiv2/moment.v1.MuteService/Mute")
    suspend fun mute(@Body request: MuteRequest): MuteResponse

    @POST("apiv2/moment.v1.MuteService/UnMute")
    suspend fun unMute(@Body request: UnMuteRequest): UnMuteResponse

    @POST("apiv2/moment.v1.MuteService/ListMuteReasons")
    suspend fun listMuteReasons(): ListMuteReasonsResponse

    @POST("apiv2/moment.v1.MuteService/AppendMuteReasons")
    suspend fun appendMuteReasons(@Body request: AppendMuteReasonsRequest): AppendMuteReasonsResponse

    // ===== ComplaintService（举报）=====
    @POST("apiv2/moment.v1.ComplaintService/CreateComplaint")
    suspend fun createComplaint(@Body request: CreateComplaintRequest): CreateComplaintResponse

    // ===== SearchService =====
    @POST("apiv2/moment.v1.SearchService/SearchMoments")
    suspend fun searchMoments(@Body request: SearchMomentsRequest): SearchMomentsResponse

    @POST("apiv2/moment.v1.SearchService/SearchUsers")
    suspend fun searchUsers(@Body request: SearchUsersRequest): SearchUsersResponse

    @POST("apiv2/moment.v1.SearchService/SearchHashtags")
    suspend fun searchHashtags(@Body request: SearchHashtagsRequest): SearchHashtagsResponse

    @POST("apiv2/moment.v1.SearchService/SuggestSearchQuery")
    suspend fun suggestSearchQuery(@Body request: SuggestSearchQueryRequest): SuggestSearchQueryResponse

    @POST("apiv2/moment.v1.SearchService/ListHotHashtagsInSearch")
    suspend fun listHotHashtagsInSearch(): ListHotHashtagsInSearchResponse

    // ===== ConfigService =====
    @POST("apiv2/moment.v1.ConfigService/GetConfig")
    suspend fun getConfig(): GetConfigResponse

    // ===== AuthService（/api 前缀）=====
    @POST("api/account.gateway.v1.AuthService/RefreshToken")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    @POST("api/account.gateway.v1.AuthService/Logout")
    suspend fun logout(@Body request: LogoutRequest): LogoutResponse
}
