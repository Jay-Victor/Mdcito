package com.mdcito.app.data.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

object LanguageHelper {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LOCALE = "selected_locale"

    data class LanguageOption(
        val tag: String,
        val displayName: String,
    )

    val supportedLanguages = listOf(
        LanguageOption("", "跟随系统"),
        LanguageOption("zh-CN", "简体中文"),
        LanguageOption("zh-TW", "繁體中文"),
        LanguageOption("en", "English"),
        LanguageOption("ja", "日本語"),
        LanguageOption("ko", "한국어"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("fr", "Français"),
    )

    @Volatile
    private var currentLocaleTag: String? = null

    var onLocaleChanged: (() -> Unit)? = null

    /**
     * CompositionLocal 用于在 Compose 树中传播语言版本号。
     * 当语言切换时，版本号递增，读取此值的 Composable 会被强制重组，
     * 从而使 stringResource() 重新读取更新后的资源。
     */
    val LocalLocaleVersion = compositionLocalOf { 0 }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentLocaleTag = prefs.getString(KEY_LOCALE, null)
    }

    fun getCurrentLanguageTag(): String = currentLocaleTag ?: ""

    fun wrapContext(context: Context): Context {
        val tag = currentLocaleTag
        if (tag.isNullOrEmpty()) return context

        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun setLanguage(activity: Activity, languageTag: String) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOCALE, languageTag.ifEmpty { null }).apply()

        currentLocaleTag = languageTag.ifEmpty { null }

        onLocaleChanged?.invoke()

        // 通过重建 Activity 使新 Locale 生效，替代废弃的 updateConfiguration()
        activity.recreate()
    }
}
