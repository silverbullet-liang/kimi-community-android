package com.kimi.community.data.api

import com.google.gson.annotations.SerializedName

/**
 * Connect-RPC 错误响应（仅在 code != 0 时返回）。
 * 成功响应直接是业务对象（如 ListFeedsResponse），无 envelope 包装。
 * 错误格式：{ "code": int, "message": string, "details": [...] }
 */
data class ErrorResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: List<Any>? = null
)

/** 错误码总表（严格按 Kimi_API_zh.md 第 6 节） */
object ErrorCodes {
    const val OK = 0
    const val INVALID_ARGUMENT = 400
    const val UNAUTHENTICATED = 401
    const val PERMISSION_DENIED = 403
    const val NOT_FOUND = 404
    const val ALREADY_EXISTS = 409
    const val FAILED_PRECONDITION = 412
    const val RESOURCE_EXHAUSTED = 429
    const val CANCELLED = 499
    const val INTERNAL = 500
    const val UNAVAILABLE = 503
    const val DEADLINE_EXCEEDED = 504

    /** 需要触发 token 刷新或重新登录的错误码 */
    fun isAuthError(code: Int): Boolean = code == UNAUTHENTICATED || code == PERMISSION_DENIED

    /** 需要退避重试的错误码 */
    fun isRetryable(code: Int): Boolean =
        code == RESOURCE_EXHAUSTED || code == UNAVAILABLE || code == DEADLINE_EXCEEDED

    fun describe(code: Int): String = when (code) {
        OK -> "成功"
        INVALID_ARGUMENT -> "请求参数非法"
        UNAUTHENTICATED -> "登录已过期，请重新登录"
        PERMISSION_DENIED -> "无权限执行该操作"
        NOT_FOUND -> "内容不存在或已删除"
        ALREADY_EXISTS -> "操作冲突（重复执行）"
        FAILED_PRECONDITION -> "前置条件不满足"
        RESOURCE_EXHAUSTED -> "操作过于频繁，请稍后再试"
        CANCELLED -> "请求已取消"
        INTERNAL -> "服务器内部错误"
        UNAVAILABLE -> "服务暂不可用"
        DEADLINE_EXCEEDED -> "请求超时"
        else -> "未知错误（$code）"
    }
}

/** 业务异常：携带错误码，供 UI 层展示并自动应对 */
class ApiException(
    val code: Int,
    val apiMessage: String? = null,
    val details: List<Any>? = null,
    cause: Throwable? = null
) : Exception(buildMessage(code, apiMessage), cause) {

    val friendlyMessage: String
        get() = apiMessage?.takeIf { it.isNotBlank() && it != "OK" } ?: ErrorCodes.describe(code)

    companion object {
        private fun buildMessage(code: Int, apiMessage: String?): String {
            val desc = ErrorCodes.describe(code)
            return if (!apiMessage.isNullOrBlank() && apiMessage != "OK") {
                "$desc：$apiMessage"
            } else {
                desc
            }
        }
    }
}
