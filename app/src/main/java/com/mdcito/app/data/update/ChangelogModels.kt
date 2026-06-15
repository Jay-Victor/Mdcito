package com.mdcito.app.data.update

/**
 * 更新日志条目模型
 */
data class ChangelogEntry(
    val versionName: String,
    val releaseDate: String,
    val title: String,
    val changes: List<String>,
    val isLatest: Boolean = false,
    val releaseUrl: String = "",
)
