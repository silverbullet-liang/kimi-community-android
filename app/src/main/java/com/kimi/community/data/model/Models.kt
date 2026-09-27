package com.kimi.community.data.model

import com.google.gson.annotations.SerializedName

// ===== 通用类型（严格按 Kimi_API_zh.md 附录 A）=====
data class ImageUrl(
    @SerializedName("url") val url: String? = null,
    @SerializedName("fileId") val fileId: String? = null,
    @SerializedName("originUrl") val originUrl: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null
)

/** AvatarImage — url:string */
data class AvatarImage(
    @SerializedName("url") val url: String? = null
)

/** UserBase — userId, name, bio, avatarImage */
data class UserBase(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarImage") val avatarImage: AvatarImage? = null
)

/** UserStat — followingNum, followerNum, momentLikesNum, momentNum */
data class UserStat(
    @SerializedName("followingNum") val followingNum: Int? = null,
    @SerializedName("followerNum") val followerNum: Int? = null,
    @SerializedName("momentLikesNum") val momentLikesNum: Int? = null,
    @SerializedName("momentNum") val momentNum: Int? = null
)

/** InteractionStatus (moment.type.v1) — isLiked, isDisliked, isCollected, isMuted */
data class InteractionStatus(
    @SerializedName("isLiked") val isLiked: Boolean? = null,
    @SerializedName("isDisliked") val isDisliked: Boolean? = null,
    @SerializedName("isCollected") val isCollected: Boolean? = null,
    @SerializedName("isMuted") val isMuted: Boolean? = null
)

/** MomentStat (moment.type.v1) — likeNum, dislikeNum, commentNum, shareNum, collectNum, readNum */
data class MomentStat(
    @SerializedName("likeNum") val likeNum: Int? = null,
    @SerializedName("dislikeNum") val dislikeNum: Int? = null,
    @SerializedName("commentNum") val commentNum: Int? = null,
    @SerializedName("shareNum") val shareNum: Int? = null,
    @SerializedName("collectNum") val collectNum: Int? = null,
    @SerializedName("readNum") val readNum: Int? = null
)

/** PageStat — commentNum, likeNum */
data class PageStat(
    @SerializedName("commentNum") val commentNum: Int? = null,
    @SerializedName("likeNum") val likeNum: Int? = null
)

/** MomentMedia — videoUrl, images */
data class MomentMedia(
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("images") val images: List<ImageUrl>? = null
)

/** MomentExternalLink — url, coverImage, title, description */
data class MomentExternalLink(
    @SerializedName("url") val url: String? = null,
    @SerializedName("coverImage") val coverImage: ImageUrl? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null
)

/** MomentChatInfo — rawMessage, isDeleted, share, segments */
data class MomentChatInfo(
    @SerializedName("rawMessage") val rawMessage: String? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean? = null,
    @SerializedName("share") val share: Any? = null,
    @SerializedName("segments") val segments: List<Any>? = null
)

/** Link — id, type, displayText, text, offset, length, url */
data class Link(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("displayText") val displayText: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("offset") val offset: Int? = null,
    @SerializedName("length") val length: Int? = null,
    @SerializedName("url") val url: String? = null
)

/** MentionedUser — userId, name */
data class MentionedUser(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String? = null
)

/** MomentContent — text, title, media, source, externalLink, chatInfo, chatShareId, type, excerpt, visibility, textType, artifactShareId, mentions, links */
data class MomentContent(
    @SerializedName("text") val text: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("media") val media: MomentMedia? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("externalLink") val externalLink: MomentExternalLink? = null,
    @SerializedName("chatInfo") val chatInfo: MomentChatInfo? = null,
    @SerializedName("chatShareId") val chatShareId: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("excerpt") val excerpt: String? = null,
    @SerializedName("visibility") val visibility: String? = null,
    @SerializedName("textType") val textType: String? = null,
    @SerializedName("artifactShareId") val artifactShareId: String? = null,
    @SerializedName("mentions") val mentions: List<MentionedUser>? = null,
    @SerializedName("links") val links: List<Link>? = null
)

/** HtmlFile — cdnUrl, coverImage, coverImageDominantColor, remixPrompt, fileId */
data class HtmlFile(
    @SerializedName("cdnUrl") val cdnUrl: String? = null,
    @SerializedName("coverImage") val coverImage: ImageUrl? = null,
    @SerializedName("coverImageDominantColor") val coverImageDominantColor: String? = null,
    @SerializedName("remixPrompt") val remixPrompt: String? = null,
    @SerializedName("fileId") val fileId: String? = null
)

/** ChatShareCard — htmlFile, codeArtifact, imageList, okcArtifact, notice, noticeIconUrl（Moment 顶层字段） */
data class ChatShareCard(
    @SerializedName("htmlFile") val htmlFile: HtmlFile? = null,
    @SerializedName("codeArtifact") val codeArtifact: Any? = null,
    @SerializedName("imageList") val imageList: List<ImageUrl>? = null,
    @SerializedName("okcArtifact") val okcArtifact: Any? = null,
    @SerializedName("notice") val notice: String? = null,
    @SerializedName("noticeIconUrl") val noticeIconUrl: String? = null
)

/** MomentAuthor — userBase, userStat, type, relationStatus */
data class MomentAuthor(
    @SerializedName("userBase") val userBase: UserBase? = null,
    @SerializedName("userStat") val userStat: UserStat? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("relationStatus") val relationStatus: UserRelationStatus? = null
)

/** UserRelationStatus — followStatus, blockStatus */
data class UserRelationStatus(
    @SerializedName("followStatus") val followStatus: String? = null,
    @SerializedName("blockStatus") val blockStatus: String? = null
)

// ===== Moment =====
/**
 * Moment — id, content, stat, createTime, updateTime, publishTime, author,
 * interactionStatus, commentPermission, askKimiQuestion, refMomentId, hotComment,
 * chatShareCard（顶层）, coverImage, hashtags, askKimiQuestions, commentPresets
 */
data class Moment(
    @SerializedName("id") val id: String? = null,
    @SerializedName("content") val content: MomentContent? = null,
    @SerializedName("stat") val stat: MomentStat? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("publishTime") val publishTime: String? = null,
    @SerializedName("author") val author: MomentAuthor? = null,
    @SerializedName("interactionStatus") val interactionStatus: InteractionStatus? = null,
    @SerializedName("commentPermission") val commentPermission: String? = null,
    @SerializedName("askKimiQuestion") val askKimiQuestion: AskKimiQuestion? = null,
    @SerializedName("refMomentId") val refMomentId: String? = null,
    @SerializedName("hotComment") val hotComment: Comment? = null,
    @SerializedName("chatShareCard") val chatShareCard: ChatShareCard? = null,
    @SerializedName("coverImage") val coverImage: ImageUrl? = null,
    @SerializedName("hashtags") val hashtags: List<HashtagItem>? = null,
    @SerializedName("askKimiQuestions") val askKimiQuestions: List<AskKimiQuestion>? = null,
    @SerializedName("commentPresets") val commentPresets: List<CommentPreset>? = null
)

/** AskKimiQuestion — question, id */
data class AskKimiQuestion(
    @SerializedName("question") val question: String? = null,
    @SerializedName("id") val id: String? = null
)

/** HashtagItem — id, name, createTime, updateTime, description, icon, status, type, momentCount */
data class HashtagItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("icon") val icon: HashtagIcon? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("momentCount") val momentCount: String? = null
)

/** HashtagIcon — fileId, url, width, height */
data class HashtagIcon(
    @SerializedName("fileId") val fileId: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

/** CommentPreset (moment.type.v1) — id, content, mentions */
data class CommentPreset(
    @SerializedName("id") val id: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("mentions") val mentions: List<MentionedUser>? = null
)

// ===== Feed =====
/** Feed — feedType, feedId, moment, interestCard, recEventLog */
data class Feed(
    @SerializedName("feedType") val feedType: String? = null,
    @SerializedName("feedId") val feedId: String? = null,
    @SerializedName("moment") val moment: Moment? = null,
    @SerializedName("interestCard") val interestCard: InterestCard? = null,
    @SerializedName("recEventLog") val recEventLog: RecEventLog? = null
)

/** RecEventLog — via, trackVia */
data class RecEventLog(
    @SerializedName("via") val via: String? = null,
    @SerializedName("trackVia") val trackVia: String? = null
)

/** InterestCard — title, content, buttonText, interests */
data class InterestCard(
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("buttonText") val buttonText: String? = null,
    @SerializedName("interests") val interests: List<Interest>? = null
)

/** Interest — name, imageUrl, subInterests */
data class Interest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("subInterests") val subInterests: List<SubInterest>? = null
)

/** SubInterest — name, domainTag */
data class SubInterest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("domainTag") val domainTag: String? = null
)

// ===== Comment =====
/**
 * Comment — createTime, id, author, momentId, repliedComment, content, type, stat,
 * ipLocation, status, interactionStatus, subCommentPageToken, subCommentNum,
 * subCommentIsLastPage, rootComment, aiGenerated, subComments, mentions, links
 */
data class Comment(
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("author") val author: CommentAuthor? = null,
    @SerializedName("momentId") val momentId: String? = null,
    @SerializedName("repliedComment") val repliedComment: RepliedComment? = null,
    @SerializedName("content") val content: CommentContent? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("stat") val stat: CommentStat? = null,
    @SerializedName("ipLocation") val ipLocation: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("interactionStatus") val interactionStatus: CommentInteractionStatus? = null,
    @SerializedName("subCommentPageToken") val subCommentPageToken: String? = null,
    @SerializedName("subCommentNum") val subCommentNum: Int? = null,
    @SerializedName("subCommentIsLastPage") val subCommentIsLastPage: Boolean? = null,
    @SerializedName("rootComment") val rootComment: Comment? = null,
    @SerializedName("aiGenerated") val aiGenerated: Boolean? = null,
    @SerializedName("subComments") val subComments: List<Comment>? = null,
    @SerializedName("mentions") val mentions: List<MentionedUser>? = null,
    @SerializedName("links") val links: List<Link>? = null
)

/** CommentAuthor — userId, name, avatarImage */
data class CommentAuthor(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatarImage") val avatarImage: AvatarImage? = null
)

/** CommentContent — text */
data class CommentContent(
    @SerializedName("text") val text: String? = null
)

/** CommentStat — likeNum, replyNum */
data class CommentStat(
    @SerializedName("likeNum") val likeNum: Int? = null,
    @SerializedName("replyNum") val replyNum: Int? = null
)

/** CommentInteractionStatus — isLiked, isDisliked, isMuted */
data class CommentInteractionStatus(
    @SerializedName("isLiked") val isLiked: Boolean? = null,
    @SerializedName("isDisliked") val isDisliked: Boolean? = null,
    @SerializedName("isMuted") val isMuted: Int? = null
)

/** RepliedComment — id, user */
data class RepliedComment(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user") val user: CommentAuthor? = null
)

// ===== Notification =====
/**
 * Notification — id, notificationType, momentId, commentId, happenedTime, readTime, fromUsers
 * 注意：fromUsers 是用户 ID 字符串列表（repeated string），不是用户对象。
 * 完整用户信息在 ListNotificationResponse.users 数组里，通过 userId 关联查找。
 */
data class Notification(
    @SerializedName("id") val id: String? = null,
    @SerializedName("notificationType") val notificationType: String? = null,
    @SerializedName("momentId") val momentId: String? = null,
    @SerializedName("commentId") val commentId: String? = null,
    @SerializedName("happenedTime") val happenedTime: String? = null,
    @SerializedName("readTime") val readTime: String? = null,
    @SerializedName("fromUsers") val fromUsers: List<String>? = null
)

/** UserMiniInfo (moment.type.v1) — userId, name, bio, avatarUrl, isDeleted */
data class UserMiniInfo(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("isDeleted") val isDeleted: Int? = null
)

/** NotificationConfig — pollSec, pollInCommunitySec */
data class NotificationConfig(
    @SerializedName("pollSec") val pollSec: Int? = null,
    @SerializedName("pollInCommunitySec") val pollInCommunitySec: Int? = null
)

// ===== UserProfile =====
/** UserInfo — userBase, userStat, interactionStatus, isDeleted */
data class UserInfo(
    @SerializedName("userBase") val userBase: UserBase? = null,
    @SerializedName("userStat") val userStat: UserStat? = null,
    @SerializedName("interactionStatus") val interactionStatus: InteractionStatus? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean? = null
)

// ===== 搜索 =====
/** SearchMomentsResponseItem — moment, recEventLog, titleHighlights, excerptHighlights */
data class SearchMomentsResponseItem(
    @SerializedName("moment") val moment: Moment? = null,
    @SerializedName("recEventLog") val recEventLog: RecEventLog? = null,
    @SerializedName("titleHighlights") val titleHighlights: List<SearchResponseItemHighlight>? = null,
    @SerializedName("excerptHighlights") val excerptHighlights: List<SearchResponseItemHighlight>? = null
)

/** SearchUsersResponseItem — userInfo, nameHighlights, bioHighlights */
data class SearchUsersResponseItem(
    @SerializedName("userInfo") val userInfo: UserInfo? = null,
    @SerializedName("nameHighlights") val nameHighlights: List<SearchResponseItemHighlight>? = null,
    @SerializedName("bioHighlights") val bioHighlights: List<SearchResponseItemHighlight>? = null
)

/** SearchResponseItemHighlight — startFrom, endAt */
data class SearchResponseItemHighlight(
    @SerializedName("startFrom") val startFrom: Int? = null,
    @SerializedName("endAt") val endAt: Int? = null
)

// ===== Auth =====
/** RefreshTokenRequest — refreshToken */
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

/** RefreshTokenResponse — accessToken, refreshToken（双令牌轮换） */
data class RefreshTokenResponse(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null
)

/** LogoutRequest/Response — 空消息 */
data class LogoutRequest(val ignored: Boolean = true)

data class LogoutResponse(val ignored: Boolean = true)
