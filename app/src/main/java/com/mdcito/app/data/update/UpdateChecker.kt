package com.mdcito.app.data.update

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(
    private val gson: Gson
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val GITHUB_OWNER = "Jay-Victor"
        private const val GITHUB_REPO = "Mdcito"
        private const val GITEE_OWNER = "Jay-Victor"
        private const val GITEE_REPO = "Mdcito"

        // GitHub 镜像加速地址
        private val GITHUB_MIRRORS = listOf(
            MirrorUrl("ghproxy.com", "https://ghproxy.com/"),
            MirrorUrl("ghp.ci", "https://ghp.ci/"),
            MirrorUrl("gh.api.99988866.xyz", "https://gh.api.99988866.xyz/")
        )
    }

    /**
     * 并行检查两个平台的更新，同时返回双方结果
     * 使用 Kotlin async/await 并行发起网络请求，总耗时 = max(Gitee耗时, GitHub耗时)
     */
    suspend fun checkAllSources(currentVersion: String): DualCheckResult = coroutineScope {
        val giteeDeferred = async(Dispatchers.IO) { runCatching { checkGiteeRelease(currentVersion) } }
        val githubDeferred = async(Dispatchers.IO) { runCatching { checkGitHubRelease(currentVersion) } }
        DualCheckResult(
            gitee = giteeDeferred.await().getOrNull(),
            github = githubDeferred.await().getOrNull()
        )
    }

    /**
     * 从指定平台检查更新
     */
    suspend fun checkFromSource(source: UpdateSource, currentVersion: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            when (source) {
                UpdateSource.GITHUB -> runCatching { checkGitHubRelease(currentVersion) }.getOrNull()
                UpdateSource.GITEE -> runCatching { checkGiteeRelease(currentVersion) }.getOrNull()
            }
        }
    }

    private fun checkGitHubRelease(currentVersion: String): UpdateInfo? {
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        val response = executeRequest(url)
        if (response == null) {
            Timber.tag("UpdateChecker").w("GitHub API 请求失败")
            return null
        }

        val release = try {
            gson.fromJson(response, GitHubRelease::class.java)
        } catch (e: Exception) {
            Timber.tag("UpdateChecker").e(e, "解析 GitHub Release 失败")
            return null
        }

        if (release.draft || release.prerelease) return null

        val remoteVersion = release.tagName.trimStart('v')
        if (!VersionComparator.isNewer(remoteVersion, currentVersion)) return null

        // 查找 APK asset
        val apkAsset = release.assets.find {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null

        // URL 验证：确保包含有效的 HTTP/HTTPS 协议前缀
        val downloadUrl = apkAsset.browserDownloadUrl
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            Timber.tag("UpdateChecker").w("GitHub 下载 URL 格式无效: $downloadUrl")
            return null
        }

        // 生成镜像 URL（确保每个镜像 URL 都有完整的协议前缀）
        val mirrorUrls = GITHUB_MIRRORS.mapNotNull { mirror ->
            val mirrorFullUrl = mirror.url + downloadUrl
            // 验证镜像 URL 格式
            if (mirrorFullUrl.startsWith("http://") || mirrorFullUrl.startsWith("https://")) {
                MirrorUrl(mirror.name, mirrorFullUrl)
            } else {
                Timber.tag("UpdateChecker").w("镜像 URL 格式无效: ${mirror.name} - $mirrorFullUrl")
                null
            }
        }

        return UpdateInfo(
            versionName = remoteVersion,
            versionCode = parseVersionCode(remoteVersion),
            releaseNotes = release.body,
            publishedAt = release.publishedAt,
            downloadUrl = downloadUrl,
            downloadSize = apkAsset.size,
            fileName = apkAsset.name,
            source = UpdateSource.GITHUB,
            mirrorUrls = mirrorUrls
        )
    }

    private fun checkGiteeRelease(currentVersion: String): UpdateInfo? {
        val url = "https://gitee.com/api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases/latest"
        val response = executeRequest(url)
        if (response == null) {
            Timber.tag("UpdateChecker").w("Gitee API 请求失败")
            return null
        }

        val release = try {
            gson.fromJson(response, GiteeRelease::class.java)
        } catch (e: Exception) {
            Timber.tag("UpdateChecker").e(e, "解析 Gitee Release 失败")
            return null
        }

        if (release.draft || release.prerelease) return null

        val remoteVersion = release.tagName.trimStart('v')
        if (!VersionComparator.isNewer(remoteVersion, currentVersion)) return null

        // 查找 APK asset
        val apkAsset = release.assets.find {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null

        // 使用 browser_download_url 作为下载链接（这是直接的下载 URL）
        // 如果 browser_download_url 为空，则构造标准的下载 URL
        val downloadUrl = apkAsset.browserDownloadUrl.ifBlank {
            // 构造标准 Gitee 下载链接: https://gitee.com/{owner}/{repo}/releases/download/{tag}/{filename}
            "https://gitee.com/$GITEE_OWNER/$GITEE_REPO/releases/download/${release.tagName}/${apkAsset.name}"
        }

        // URL 验证：确保包含有效的 HTTP/HTTPS 协议前缀
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            Timber.tag("UpdateChecker").w("Gitee 下载 URL 格式无效: $downloadUrl")
            return null
        }

        return UpdateInfo(
            versionName = remoteVersion,
            versionCode = parseVersionCode(remoteVersion),
            releaseNotes = release.body,
            publishedAt = release.publishedAt.ifBlank { release.createdAt },
            downloadUrl = downloadUrl,
            downloadSize = apkAsset.size,
            fileName = apkAsset.name,
            source = UpdateSource.GITEE,
            mirrorUrls = emptyList()
        )
    }

    private fun executeRequest(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            val call = httpClient.newCall(request)
            val response = call.execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                Timber.tag("UpdateChecker").w("HTTP ${response.code}: $url")
                null
            }
        } catch (e: Exception) {
            Timber.tag("UpdateChecker").e(e, "网络请求异常: $url")
            null
        }
    }

    /**
     * 获取所有版本的更新日志（用于更新日志页面）
     * 优先从 GitHub 获取，失败时切换到 Gitee
     */
    suspend fun fetchAllReleases(): List<ChangelogEntry> = withContext(Dispatchers.IO) {
        // 优先 GitHub
        val githubEntries = runCatching { fetchGitHubAllReleases() }.getOrNull()
        if (!githubEntries.isNullOrEmpty()) return@withContext githubEntries

        // 回退到 Gitee
        val giteeEntries = runCatching { fetchGiteeAllReleases() }.getOrNull()
        if (!giteeEntries.isNullOrEmpty()) return@withContext giteeEntries

        emptyList()
    }

    private fun fetchGitHubAllReleases(): List<ChangelogEntry> {
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=30"
        val response = executeRequest(url) ?: return emptyList()

        val releases = try {
            val type = object : TypeToken<List<GitHubRelease>>() {}.type
            gson.fromJson<List<GitHubRelease>>(response, type)
        } catch (e: Exception) {
            Timber.tag("UpdateChecker").e(e, "解析 GitHub Releases 列表失败")
            return emptyList()
        }

        return releases
            .filter { !it.draft && !it.prerelease }
            .map { release ->
                val version = release.tagName.trimStart('v')
                val date = formatReleaseDate(release.publishedAt)
                ChangelogEntry(
                    versionName = version,
                    releaseDate = date,
                    title = "Mdcito V$version",
                    changes = parseReleaseNotes(release.body),
                    releaseUrl = release.htmlUrl,
                )
            }
            .sortedWith(
                compareByDescending<ChangelogEntry> { parseVersionCode(it.versionName) }
                    .thenByDescending { it.releaseDate }
            )
            .mapIndexed { index, entry ->
                entry.copy(isLatest = index == 0)
            }
    }

    private fun fetchGiteeAllReleases(): List<ChangelogEntry> {
        val url = "https://gitee.com/api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases?per_page=30"
        val response = executeRequest(url) ?: return emptyList()

        val releases = try {
            val type = object : TypeToken<List<GiteeRelease>>() {}.type
            gson.fromJson<List<GiteeRelease>>(response, type)
        } catch (e: Exception) {
            Timber.tag("UpdateChecker").e(e, "解析 Gitee Releases 列表失败")
            return emptyList()
        }

        return releases
            .filter { !it.draft && !it.prerelease }
            .map { release ->
                val version = release.tagName.trimStart('v')
                val date = formatReleaseDate(release.publishedAt.ifBlank { release.createdAt })
                ChangelogEntry(
                    versionName = version,
                    releaseDate = date,
                    title = "Mdcito V$version",
                    changes = parseReleaseNotes(release.body),
                    releaseUrl = release.htmlUrl,
                )
            }
            .sortedWith(
                compareByDescending<ChangelogEntry> { parseVersionCode(it.versionName) }
                    .thenByDescending { it.releaseDate }
            )
            .mapIndexed { index, entry ->
                entry.copy(isLatest = index == 0)
            }
    }

    /**
     * 解析 Release Notes 为变更列表
     * 过滤掉 Markdown 标题行，保留列表项和普通文本
     */
    private fun parseReleaseNotes(body: String): List<String> {
        if (body.isBlank()) return emptyList()
        return body.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("#") } // 过滤 Markdown 标题行
            .map { line ->
                // 移除 Markdown 列表标记
                line.removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("+ ")
                    .removePrefix("• ")
                    .trim()
            }
            .filter { it.isNotBlank() }
    }

    /**
     * 格式化发布日期为 "年-月-日" 格式
     */
    private fun formatReleaseDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return try {
            // GitHub: 2023-10-25T08:30:00Z
            // Gitee: 2023-10-25T08:30:00+08:00
            val datePart = dateStr.substringBefore("T")
            // 验证格式
            val parts = datePart.split("-")
            if (parts.size == 3) {
                "${parts[0]}-${parts[1]}-${parts[2]}"
            } else {
                datePart
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseVersionCode(version: String): Int {
        val parts = version.trimStart('v').substringBefore('-').split('.')
        var code = 0
        for ((i, part) in parts.withIndex()) {
            val num = part.toIntOrNull() ?: 0
            code = code * 100 + num
            if (i >= 2) break
        }
        return code
    }
}
