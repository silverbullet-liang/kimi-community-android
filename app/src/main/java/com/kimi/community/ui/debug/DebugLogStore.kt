package com.kimi.community.ui.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局调试日志存储。OkHttp 拦截器将请求/响应日志写入此单例，
 * 调试面板从 StateFlow 实时读取展示。
 */
object DebugLogStore {

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val title: String,
        val body: String
    )

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        if (!enabled) clear()
    }

    fun log(title: String, body: String = "") {
        if (!_enabled.value) return
        val entry = LogEntry(title = title, body = body)
        _logs.value = (listOf(entry) + _logs.value).take(200)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
