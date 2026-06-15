package com.mdcito.app.data.update

import com.google.gson.annotations.SerializedName

/**
 * 更新源平台枚举
 */
enum class UpdateSource(val displayName: String, val repoUrl: String) {
    GITHUB("GitHub", "https://github.com/Jay-Victor/Mdcito"),
    GITEE("Gitee", "https://gitee.com/Jay-Victor/Mdcito")
}

/**
 * GitHub Release API 响应模型
 * API 文档: https://docs.github.com/en/rest/releases/releases
 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList()
)

data class GitHubAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("browser_download_url") val browserDownloadUrl: String = ""
)

/**
 * Gitee Release API 响应模型
 * API 文档: https://gitee.com/api/v5/swagger#/getV5ReposOwnerRepoReleasesLatest
 */
data class GiteeRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("assets") val assets: List<GiteeAsset> = emptyList()
)

data class GiteeAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("url") val url: String = ""
)

/**
 * 统一更新信息模型
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val downloadSize: Long,
    val fileName: String,
    val source: UpdateSource,
    val mirrorUrls: List<MirrorUrl> = emptyList()
)

/**
 * 双平台并行检测结果
 */
data class DualCheckResult(
    val gitee: UpdateInfo?,
    val github: UpdateInfo?
)

/**
 * 镜像下载地址
 */
data class MirrorUrl(
    val name: String,
    val url: String
)

/**
 * 下载进度状态
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
        val remainingTimeSecs: Long
    ) : DownloadState()
    data class Paused(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * 语义化版本比较工具
 */
object VersionComparator {
    /**
     * 比较两个语义化版本号
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
     */
    fun compare(v1: String, v2: String): Int {
        val p1 = parseVersion(v1)
        val p2 = parseVersion(v2)

        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    /**
     * 判断远程版本是否比本地版本更新
     */
    fun isNewer(remote: String, local: String): Boolean = compare(remote, local) > 0

    private fun parseVersion(version: String): List<Int> {
        val cleanVersion = version.trimStart('v').substringBefore('-')
        return cleanVersion.split('.').mapNotNull { it.toIntOrNull() }
    }
}
