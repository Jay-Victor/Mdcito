package com.mdcito.app.data.font

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import com.mdcito.app.R

data class BuiltinFontConfig(
    val name: String,
    val fontFamily: String,
    val downloadUrl: String,
    val fallbackUrls: List<String> = emptyList(),
    val fileName: String,
    val description: String,
)

data class InstalledFont(
    val name: String,
    val fontFamily: String,
    val filePath: String,
    val isBuiltin: Boolean = false,
)

@Singleton
class FontService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        val builtinFonts = listOf(
            BuiltinFontConfig(
                name = "思源黑体",
                fontFamily = "noto_sans_sc",
                downloadUrl = "https://cdn.jsdelivr.net/gh/notofonts/noto-cjk@main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf",
                fallbackUrls = listOf(
                    "https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf",
                    "https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf",
                ),
                fileName = "NotoSansSC-Regular.otf",
                description = "Google 出品的中文字体，适合正文阅读",
            ),
            BuiltinFontConfig(
                name = "JetBrains Mono",
                fontFamily = "jetbrains_mono",
                downloadUrl = "https://cdn.jsdelivr.net/gh/JetBrains/JetBrainsMono@master/fonts/ttf/JetBrainsMono-Regular.ttf",
                fallbackUrls = listOf(
                    "https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Regular.ttf",
                    "https://raw.githubusercontent.com/JetBrains/JetBrainsMono/master/fonts/ttf/JetBrainsMono-Regular.ttf",
                ),
                fileName = "JetBrainsMono-Regular.ttf",
                description = "JetBrains 出品的编程字体，适合代码块",
            ),
            BuiltinFontConfig(
                name = "霞鹜文楷",
                fontFamily = "lxgw_wenkai",
                downloadUrl = "https://cdn.jsdelivr.net/gh/lxgw/LxgwWenKai@v1.501/LXGWWenKai-Regular.ttf",
                fallbackUrls = listOf(
                    "https://github.com/lxgw/LxgwWenKai/releases/download/v1.501/LXGWWenKai-Regular.ttf",
                    "https://raw.githubusercontent.com/lxgw/LxgwWenKai/v1.501/LXGWWenKai-Regular.ttf",
                ),
                fileName = "LXGWWenKai-Regular.ttf",
                description = "开源中文字体，优雅的楷体风格",
            ),
        )

        val systemFonts = listOf(
            "system" to "系统默认",
            "sans_serif" to "无衬线体",
            "serif" to "衬线体",
            "monospace" to "等宽字体",
        )
    }

    private val fontsDir: File
        get() = File(context.filesDir, "custom_fonts").also { it.mkdirs() }

    private val _installedFonts = MutableStateFlow<List<InstalledFont>>(emptyList())
    val installedFonts: StateFlow<List<InstalledFont>> = _installedFonts.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    init {
        loadInstalledFonts()
    }

    private fun loadInstalledFonts() {
        val fonts = mutableListOf<InstalledFont>()
        val prefs = context.getSharedPreferences("custom_fonts", Context.MODE_PRIVATE)
        val fontEntries = prefs.getStringSet("installed_fonts", emptySet()) ?: emptySet()

        for (entry in fontEntries) {
            val parts = entry.split("|", limit = 4)
            if (parts.size >= 3) {
                val name = parts[0]
                val fontFamily = parts[1]
                val filePath = parts[2]
                val isBuiltin = parts.size > 3 && parts[3] == "builtin"
                val file = File(filePath)
                if (file.exists()) {
                    fonts.add(InstalledFont(name, fontFamily, filePath, isBuiltin))
                }
            }
        }
        _installedFonts.value = fonts
    }

    fun isFontDownloaded(fontFamily: String): Boolean {
        return _installedFonts.value.any { it.fontFamily == fontFamily }
    }

    fun getInstalledFont(fontFamily: String): InstalledFont? {
        return _installedFonts.value.find { it.fontFamily == fontFamily }
    }

    suspend fun downloadFont(config: BuiltinFontConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val urls = listOf(config.downloadUrl) + config.fallbackUrls
        var lastException: Exception? = null

        for (urlStr in urls) {
            try {
                _downloadProgress.value = _downloadProgress.value + (config.fontFamily to 0f)
                val result = downloadFromUrl(urlStr, config.fileName, config.fontFamily)
                if (result.isSuccess) {
                    saveFontInfo(config.name, config.fontFamily, result.getOrNull()!!, isBuiltin = true)
                    loadInstalledFonts()
                    _downloadProgress.value = _downloadProgress.value - config.fontFamily
                    return@withContext Result.success(Unit)
                }
                lastException = result.exceptionOrNull() as? Exception
                _downloadProgress.value = _downloadProgress.value - config.fontFamily
            } catch (e: Exception) {
                lastException = e
                _downloadProgress.value = _downloadProgress.value - config.fontFamily
            }
        }

        val file = File(fontsDir, config.fileName)
        if (file.exists()) file.delete()
        Result.failure(lastException ?: Exception(context.getString(R.string.all_sources_failed)))
    }

    private fun downloadFromUrl(urlStr: String, fileName: String, fontFamily: String): Result<String> {
        return try {
            var url = URL(urlStr)
            var connection: HttpURLConnection
            var redirectCount = 0

            while (true) {
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "*/*")
                connection.setRequestProperty("User-Agent", "Mdcito/1.0")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrEmpty() || redirectCount >= 5) {
                        return Result.failure(Exception(context.getString(R.string.redirect_too_many)))
                    }
                    url = URL(url, location)
                    redirectCount++
                    continue
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return Result.failure(Exception("HTTP $responseCode"))
                }
                break
            }

            val totalSize = connection.contentLength.toLong()
            val file = File(fontsDir, fileName)
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(32768)
            var downloaded = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (totalSize > 0) {
                    _downloadProgress.value = _downloadProgress.value + (fontFamily to (downloaded.toFloat() / totalSize).coerceIn(0f, 1f))
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (file.length() == 0L) {
                file.delete()
                return Result.failure(Exception(context.getString(R.string.download_empty)))
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installFontFromUri(uri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isOtf = fileName.lowercase().endsWith(".otf")
            val fontName = fileName.removeSuffix(".ttf").removeSuffix(".otf").removeSuffix(".TTF").removeSuffix(".OTF")
                .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "")
                .ifEmpty { "custom_${System.currentTimeMillis()}" }

            val fontFamily = "custom_$fontName"
            val extension = if (isOtf) ".otf" else ".ttf"
            val destFile = File(fontsDir, "${fontFamily}$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception(context.getString(R.string.font_error_cannot_read)))

            saveFontInfo(fontName, fontFamily, destFile.absolutePath, isBuiltin = false)
            loadInstalledFonts()

            Result.success(fontName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeFont(fontFamily: String) {
        val font = _installedFonts.value.find { it.fontFamily == fontFamily } ?: return
        File(font.filePath).delete()

        val prefs = context.getSharedPreferences("custom_fonts", Context.MODE_PRIVATE)
        val entries = prefs.getStringSet("installed_fonts", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.removeAll { it.contains("|$fontFamily|") }
        prefs.edit().putStringSet("installed_fonts", entries).apply()

        loadInstalledFonts()
    }

    private fun saveFontInfo(name: String, fontFamily: String, filePath: String, isBuiltin: Boolean) {
        val prefs = context.getSharedPreferences("custom_fonts", Context.MODE_PRIVATE)
        val entries = prefs.getStringSet("installed_fonts", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.add("$name|$fontFamily|$filePath|${if (isBuiltin) "builtin" else "custom"}")
        prefs.edit().putStringSet("installed_fonts", entries).apply()
    }

    fun getComposeFontFamily(fontKey: String): FontFamily {
        return when (fontKey) {
            "system" -> FontFamily.Default
            "sans_serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            else -> {
                val installed = _installedFonts.value.find { it.fontFamily == fontKey }
                if (installed != null && File(installed.filePath).exists()) {
                    FontFamily(Font(file = File(installed.filePath), weight = FontWeight.Normal))
                } else {
                    FontFamily.Default
                }
            }
        }
    }

    fun getBuiltinFontDisplayName(config: BuiltinFontConfig): String {
        return when (config.fontFamily) {
            "noto_sans_sc" -> context.getString(R.string.font_name_noto_sans_sc)
            "lxgw_wenkai" -> context.getString(R.string.font_name_lxgw_wenkai)
            else -> config.name
        }
    }

    fun getBuiltinFontDescription(config: BuiltinFontConfig): String {
        return when (config.fontFamily) {
            "noto_sans_sc" -> context.getString(R.string.font_desc_noto_sans_sc)
            "jetbrains_mono" -> context.getString(R.string.font_desc_jetbrains_mono)
            "lxgw_wenkai" -> context.getString(R.string.font_desc_lxgw_wenkai)
            else -> config.description
        }
    }

    fun getSystemFontDisplayName(fontKey: String): String {
        return when (fontKey) {
            "system" -> context.getString(R.string.system_default)
            "sans_serif" -> context.getString(R.string.sans_serif)
            "serif" -> context.getString(R.string.serif)
            "monospace" -> context.getString(R.string.monospace)
            else -> fontKey
        }
    }

    fun getCodeFontCss(fontKey: String): String? {
        return when (fontKey) {
            "system", "monospace" -> null
            "sans_serif" -> ".markdown-body code, .markdown-body pre code { font-family: sans-serif !important; }"
            "serif" -> ".markdown-body code, .markdown-body pre code { font-family: serif !important; }"
            else -> {
                val installed = _installedFonts.value.find { it.fontFamily == fontKey }
                if (installed != null && File(installed.filePath).exists()) {
                    val format = if (installed.filePath.lowercase().endsWith(".otf")) "opentype" else "truetype"
                    """
                    @font-face {
                        font-family: 'CustomCodeFont';
                        src: url('file://${installed.filePath}') format('$format');
                    }
                    .markdown-body code, .markdown-body pre code { font-family: 'CustomCodeFont', monospace !important; }
                    """.trimIndent()
                } else null
            }
        }
    }
}
