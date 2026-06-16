package com.mdcito.app.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mdcito.app.data.model.ImageHostProfile
import com.mdcito.app.data.model.ImageProcessingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_COLOR_INDEX = intPreferencesKey("theme_color_index")
        val LIGHT_SCHEME = stringPreferencesKey("light_scheme")
        val DARK_SCHEME = stringPreferencesKey("dark_scheme")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val CARD_GLASS_INTENSITY = intPreferencesKey("card_glass_intensity")
        val CARD_TRANSPARENCY = intPreferencesKey("card_transparency")
        val NAV_BAR_TRANSPARENCY = intPreferencesKey("nav_bar_transparency")
        val BUTTON_STYLE = stringPreferencesKey("button_style")
        val FONT_SIZE = intPreferencesKey("font_size")
        val UI_FONT_SIZE = intPreferencesKey("ui_font_size")
        val UI_FONT = stringPreferencesKey("ui_font")
        val EDITOR_FONT = stringPreferencesKey("editor_font")
        val CODE_FONT = stringPreferencesKey("code_font")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
        val AUTO_SAVE_INTERVAL_UNIT = stringPreferencesKey("auto_save_interval_unit")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val HIGHLIGHT_CURRENT_LINE = booleanPreferencesKey("highlight_current_line")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val BRACKET_MATCHING = booleanPreferencesKey("bracket_matching")
        val SPELL_CHECK = booleanPreferencesKey("spell_check")
        val CODE_FOLDING = booleanPreferencesKey("code_folding")
        val TAB_SIZE = intPreferencesKey("tab_size")
        val LINE_WRAP = stringPreferencesKey("line_wrap")
        val VERSION_SNAPSHOT = booleanPreferencesKey("version_snapshot")
        val SAVE_ON_EXIT_PROMPT = booleanPreferencesKey("save_on_exit_prompt")
        val SYNC_SCROLL = booleanPreferencesKey("sync_scroll")
        val PREVIEW_THEME = stringPreferencesKey("preview_theme")
        val WORKSPACE_PATH = stringPreferencesKey("workspace_path")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val IMAGE_HOST_ENABLED = booleanPreferencesKey("image_host_enabled")
        val IMAGE_HOST_PROVIDER = stringPreferencesKey("image_host_provider")
        val IMAGE_HOST_GITHUB_REPO = stringPreferencesKey("image_host_github_repo")
        val IMAGE_HOST_GITHUB_BRANCH = stringPreferencesKey("image_host_github_branch")
        val IMAGE_HOST_GITHUB_DOMAIN = stringPreferencesKey("image_host_github_domain")
        val IMAGE_HOST_QINIU_AK = stringPreferencesKey("image_host_qiniu_ak")
        val IMAGE_HOST_QINIU_BUCKET = stringPreferencesKey("image_host_qiniu_bucket")
        val IMAGE_HOST_QINIU_DOMAIN = stringPreferencesKey("image_host_qiniu_domain")
        val IMAGE_HOST_ALIYUN_AK_ID = stringPreferencesKey("image_host_aliyun_ak_id")
        val IMAGE_HOST_ALIYUN_BUCKET = stringPreferencesKey("image_host_aliyun_bucket")
        val IMAGE_HOST_ALIYUN_REGION = stringPreferencesKey("image_host_aliyun_region")
        val IMAGE_HOST_ALIYUN_DOMAIN = stringPreferencesKey("image_host_aliyun_domain")
        val IMAGE_HOST_TENCENT_BUCKET = stringPreferencesKey("image_host_tencent_bucket")
        val IMAGE_HOST_TENCENT_REGION = stringPreferencesKey("image_host_tencent_region")
        val IMAGE_HOST_TENCENT_DOMAIN = stringPreferencesKey("image_host_tencent_domain")
        val IMAGE_HOST_CUSTOM_URL = stringPreferencesKey("image_host_custom_url")
        val IMAGE_HOST_CUSTOM_METHOD = stringPreferencesKey("image_host_custom_method")
        val IMAGE_HOST_CUSTOM_HEADERS = stringPreferencesKey("image_host_custom_headers")
        val IMAGE_HOST_CUSTOM_RESPONSE = stringPreferencesKey("image_host_custom_response")
        val IMAGE_COMPRESS = booleanPreferencesKey("image_compress")
        val IMAGE_COMPRESS_QUALITY = intPreferencesKey("image_compress_quality")
        val IMAGE_RESIZE = booleanPreferencesKey("image_resize")
        val IMAGE_RESIZE_MAX_WIDTH = intPreferencesKey("image_resize_max_width")
        val IMAGE_RESIZE_MAX_HEIGHT = intPreferencesKey("image_resize_max_height")
        val IMAGE_WATERMARK = booleanPreferencesKey("image_watermark")
        val IMAGE_WATERMARK_TEXT = stringPreferencesKey("image_watermark_text")
        val IMAGE_AUTO_RENAME = booleanPreferencesKey("image_auto_rename")
        val IMAGE_RENAME_RULE = stringPreferencesKey("image_rename_rule")
        val HARDWARE_ACCEL = booleanPreferencesKey("hardware_accel")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val LOG_LEVEL = stringPreferencesKey("log_level")
        val DEBUG_LOG_OUTPUT = booleanPreferencesKey("debug_log_output")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_BLUR = booleanPreferencesKey("background_blur")
        val BACKGROUND_BLUR_INTENSITY = intPreferencesKey("background_blur_intensity")
        val BACKGROUND_BRIGHTNESS = intPreferencesKey("background_brightness")
        val EDITOR_BACKGROUND_IMAGE_URI = stringPreferencesKey("editor_background_image_uri")
        val EDITOR_BACKGROUND_BLUR = booleanPreferencesKey("editor_background_blur")
        val EDITOR_BACKGROUND_BLUR_INTENSITY = intPreferencesKey("editor_background_blur_intensity")
        val EDITOR_BACKGROUND_BRIGHTNESS = intPreferencesKey("editor_background_brightness")
        val CARD_BG_COLOR = intPreferencesKey("card_bg_color")
        val NAV_BAR_COLOR = intPreferencesKey("nav_bar_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val MARKDOWN_DIALECT = stringPreferencesKey("markdown_dialect")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val NAV_BAR_GLASS_INTENSITY = intPreferencesKey("nav_bar_glass_intensity")
        // ── 每种风格独立的调节参数 ──
        val CARD_MINIMAL_TRANSPARENCY = intPreferencesKey("card_minimal_transparency")
        val CARD_FROSTED_INTENSITY = intPreferencesKey("card_frosted_intensity")
        val CARD_LIQUID_INTENSITY = intPreferencesKey("card_liquid_intensity")

        val NAV_BAR_MINIMAL_TRANSPARENCY = intPreferencesKey("nav_bar_minimal_transparency")
        val NAV_BAR_FROSTED_INTENSITY = intPreferencesKey("nav_bar_frosted_intensity")
        val NAV_BAR_LIQUID_INTENSITY = intPreferencesKey("nav_bar_liquid_intensity")
        val HIGHLIGHT_LINE_COLOR = stringPreferencesKey("highlight_line_color")
        val INDENT_TYPE = stringPreferencesKey("indent_type")
        val INDENT_SIZE = intPreferencesKey("indent_size")

        // ── 图床方案管理 ──
        val IMAGE_HOST_PROFILES = stringPreferencesKey("image_host_profiles")
        val IMAGE_HOST_ACTIVE_PROFILE_ID = stringPreferencesKey("image_host_active_profile_id")

        // ── 图片处理设置（独立于方案） ──
        val IMAGE_PROCESSING_SETTINGS = stringPreferencesKey("image_processing_settings")

        // ── 云同步设置 ──
        val CLOUD_SYNC_SERVICE_TYPE = stringPreferencesKey("cloud_sync_service_type")
        val CLOUD_SYNC_SERVER_URL = stringPreferencesKey("cloud_sync_server_url")
        val CLOUD_SYNC_PORT = intPreferencesKey("cloud_sync_port")
        val CLOUD_SYNC_USERNAME = stringPreferencesKey("cloud_sync_username")
        val CLOUD_SYNC_REMOTE_PATH = stringPreferencesKey("cloud_sync_remote_path")
        val CLOUD_SYNC_MODE = stringPreferencesKey("cloud_sync_mode")
        val CLOUD_SYNC_AUTO_SYNC_INTERVAL = intPreferencesKey("cloud_sync_auto_sync_interval")
        val CLOUD_SYNC_WIFI_ONLY = booleanPreferencesKey("cloud_sync_wifi_only")
        val CLOUD_SYNC_CHARGING_ONLY = booleanPreferencesKey("cloud_sync_charging_only")
        val CLOUD_SYNC_FILE_FILTER_RULES = stringPreferencesKey("cloud_sync_file_filter_rules")
        val CLOUD_SYNC_LAST_SYNC_TIME = longPreferencesKey("cloud_sync_last_sync_time")
        val CLOUD_SYNC_LAST_SYNC_STATUS = stringPreferencesKey("cloud_sync_last_sync_status")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val CLOUD_SYNC_TOKEN_EXPIRY_TIME = longPreferencesKey("cloud_sync_token_expiry_time")
        val CLOUD_SYNC_CONFLICT_RESOLUTION = stringPreferencesKey("cloud_sync_conflict_resolution")
        val CLOUD_SYNC_USE_CUSTOM_AUTH_ENDPOINT = booleanPreferencesKey("cloud_sync_use_custom_auth_endpoint")
        val CLOUD_SYNC_CUSTOM_AUTH_ENDPOINT = stringPreferencesKey("cloud_sync_custom_auth_endpoint")
        val CLOUD_SYNC_CUSTOM_TOKEN_ENDPOINT = stringPreferencesKey("cloud_sync_custom_token_endpoint")
        val CLOUD_SYNC_DELETIONS = booleanPreferencesKey("cloud_sync_deletions")
        val CLOUD_SYNC_LAST_SYNCED_FILES = stringPreferencesKey("cloud_sync_last_synced_files")
        val CLOUD_SYNC_HISTORY = stringPreferencesKey("cloud_sync_history")

        // ── 更新管理设置 ──
        val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        val UPDATE_SOURCE = stringPreferencesKey("update_source")
        val LAST_UPDATE_CHECK_TIME = longPreferencesKey("last_update_check_time")

        // ── 过渡动画设置 ──
        val SPLASH_ANIMATION_ENABLED = booleanPreferencesKey("splash_animation_enabled")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[Keys.THEME_MODE] ?: "SYSTEM" }
    val themeColorIndex: Flow<Int> = dataStore.data.map { it[Keys.THEME_COLOR_INDEX] ?: 0 }
    val lightScheme: Flow<String> = dataStore.data.map { it[Keys.LIGHT_SCHEME] ?: "warm" }
    val darkScheme: Flow<String> = dataStore.data.map { it[Keys.DARK_SCHEME] ?: "warm_dark" }
    val cardStyle: Flow<String> = dataStore.data.map { it[Keys.CARD_STYLE] ?: "minimal" }
    val cardGlassIntensity: Flow<Int> = dataStore.data.map { it[Keys.CARD_GLASS_INTENSITY] ?: 50 }
    val cardTransparency: Flow<Int> = dataStore.data.map { it[Keys.CARD_TRANSPARENCY] ?: 100 }
    val navBarTransparency: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_TRANSPARENCY] ?: 100 }
    val fontSize: Flow<Int> = dataStore.data.map { it[Keys.FONT_SIZE] ?: 14 }
    val reduceMotion: Flow<Boolean> = dataStore.data.map { it[Keys.REDUCE_MOTION] ?: false }
    val autoSave: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_SAVE] ?: false }
    val autoSaveInterval: Flow<Int> = dataStore.data.map { it[Keys.AUTO_SAVE_INTERVAL] ?: 5 }
    val autoSaveIntervalUnit: Flow<String> = dataStore.data.map { it[Keys.AUTO_SAVE_INTERVAL_UNIT] ?: "seconds" }
    val showLineNumbers: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_LINE_NUMBERS] ?: false }
    val highlightCurrentLine: Flow<Boolean> = dataStore.data.map { it[Keys.HIGHLIGHT_CURRENT_LINE] ?: true }
    val autoIndent: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_INDENT] ?: true }
    val bracketMatching: Flow<Boolean> = dataStore.data.map { it[Keys.BRACKET_MATCHING] ?: true }
    val spellCheck: Flow<Boolean> = dataStore.data.map { it[Keys.SPELL_CHECK] ?: false }
    val codeFolding: Flow<Boolean> = dataStore.data.map { it[Keys.CODE_FOLDING] ?: true }
    val tabSize: Flow<Int> = dataStore.data.map { it[Keys.TAB_SIZE] ?: 4 }
    val lineWrap: Flow<String> = dataStore.data.map { it[Keys.LINE_WRAP] ?: "soft" }
    val versionSnapshot: Flow<Boolean> = dataStore.data.map { it[Keys.VERSION_SNAPSHOT] ?: true }
    val saveOnExitPrompt: Flow<Boolean> = dataStore.data.map { it[Keys.SAVE_ON_EXIT_PROMPT] ?: true }
    val syncScroll: Flow<Boolean> = dataStore.data.map { it[Keys.SYNC_SCROLL] ?: true }
    val previewTheme: Flow<String> = dataStore.data.map { it[Keys.PREVIEW_THEME] ?: "follow" }
    val workspacePath: Flow<String?> = dataStore.data.map { it[Keys.WORKSPACE_PATH] }
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    val imageHostEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.IMAGE_HOST_ENABLED] ?: false }
    val imageHostProvider: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_PROVIDER] ?: "github" }
    val imageHostGithubRepo: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_GITHUB_REPO] ?: "" }
    val imageHostGithubBranch: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_GITHUB_BRANCH] ?: "main" }
    val imageHostGithubDomain: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_GITHUB_DOMAIN] ?: "" }
    val imageHostQiniuAk: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_QINIU_AK] ?: "" }
    val imageHostQiniuBucket: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_QINIU_BUCKET] ?: "" }
    val imageHostQiniuDomain: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_QINIU_DOMAIN] ?: "" }
    val imageHostAliyunAkId: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_ALIYUN_AK_ID] ?: "" }
    val imageHostAliyunBucket: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_ALIYUN_BUCKET] ?: "" }
    val imageHostAliyunRegion: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_ALIYUN_REGION] ?: "" }
    val imageHostAliyunDomain: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_ALIYUN_DOMAIN] ?: "" }
    val imageHostTencentBucket: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_TENCENT_BUCKET] ?: "" }
    val imageHostTencentRegion: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_TENCENT_REGION] ?: "" }
    val imageHostTencentDomain: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_TENCENT_DOMAIN] ?: "" }
    val imageHostCustomUrl: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_CUSTOM_URL] ?: "" }
    val imageHostCustomMethod: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_CUSTOM_METHOD] ?: "POST" }
    val imageHostCustomHeaders: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_CUSTOM_HEADERS] ?: "" }
    val imageHostCustomResponse: Flow<String> = dataStore.data.map { it[Keys.IMAGE_HOST_CUSTOM_RESPONSE] ?: "" }
    val imageCompress: Flow<Boolean> = dataStore.data.map { it[Keys.IMAGE_COMPRESS] ?: false }
    val imageCompressQuality: Flow<Int> = dataStore.data.map { it[Keys.IMAGE_COMPRESS_QUALITY] ?: 80 }
    val imageResize: Flow<Boolean> = dataStore.data.map { it[Keys.IMAGE_RESIZE] ?: false }
    val imageResizeMaxWidth: Flow<Int> = dataStore.data.map { it[Keys.IMAGE_RESIZE_MAX_WIDTH] ?: 1920 }
    val imageResizeMaxHeight: Flow<Int> = dataStore.data.map { it[Keys.IMAGE_RESIZE_MAX_HEIGHT] ?: 1080 }
    val imageWatermark: Flow<Boolean> = dataStore.data.map { it[Keys.IMAGE_WATERMARK] ?: false }
    val imageWatermarkText: Flow<String> = dataStore.data.map { it[Keys.IMAGE_WATERMARK_TEXT] ?: "" }
    val imageAutoRename: Flow<Boolean> = dataStore.data.map { it[Keys.IMAGE_AUTO_RENAME] ?: true }
    val imageRenameRule: Flow<String> = dataStore.data.map { it[Keys.IMAGE_RENAME_RULE] ?: "timestamp" }
    val hardwareAccel: Flow<Boolean> = dataStore.data.map { it[Keys.HARDWARE_ACCEL] ?: true }
    val debugMode: Flow<Boolean> = dataStore.data.map { it[Keys.DEBUG_MODE] ?: false }
    val logLevel: Flow<String> = dataStore.data.map { it[Keys.LOG_LEVEL] ?: "debug" }
    val debugLogOutput: Flow<Boolean> = dataStore.data.map { it[Keys.DEBUG_LOG_OUTPUT] ?: false }
    val buttonStyle: Flow<String> = dataStore.data.map { it[Keys.BUTTON_STYLE] ?: "default" }
    val uiFont: Flow<String> = dataStore.data.map { it[Keys.UI_FONT] ?: "system" }
    val uiFontSize: Flow<Int> = dataStore.data.map { it[Keys.UI_FONT_SIZE] ?: 14 }
    val editorFont: Flow<String> = dataStore.data.map { it[Keys.EDITOR_FONT] ?: "system" }
    val codeFont: Flow<String> = dataStore.data.map { it[Keys.CODE_FONT] ?: "jetbrains_mono" }
    val backgroundImageUri: Flow<String?> = dataStore.data.map { it[Keys.BACKGROUND_IMAGE_URI] }
    val backgroundBlur: Flow<Boolean> = dataStore.data.map { it[Keys.BACKGROUND_BLUR] ?: false }
    val backgroundBlurIntensity: Flow<Int> = dataStore.data.map { it[Keys.BACKGROUND_BLUR_INTENSITY] ?: 10 }
    val backgroundBrightness: Flow<Int> = dataStore.data.map { it[Keys.BACKGROUND_BRIGHTNESS] ?: 100 }
    val editorBackgroundImageUri: Flow<String?> = dataStore.data.map { it[Keys.EDITOR_BACKGROUND_IMAGE_URI] }
    val editorBackgroundBlur: Flow<Boolean> = dataStore.data.map { it[Keys.EDITOR_BACKGROUND_BLUR] ?: false }
    val editorBackgroundBlurIntensity: Flow<Int> = dataStore.data.map { it[Keys.EDITOR_BACKGROUND_BLUR_INTENSITY] ?: 10 }
    val editorBackgroundBrightness: Flow<Int> = dataStore.data.map { it[Keys.EDITOR_BACKGROUND_BRIGHTNESS] ?: 100 }
    val cardBgColor: Flow<Int> = dataStore.data.map { it[Keys.CARD_BG_COLOR] ?: -1 }
    val navBarColor: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_COLOR] ?: -1 }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val lineHeight: Flow<Float> = dataStore.data.map { it[Keys.LINE_HEIGHT] ?: 1.7f }
    val letterSpacing: Flow<Float> = dataStore.data.map { it[Keys.LETTER_SPACING] ?: 0.0f }
    val paragraphSpacing: Flow<Float> = dataStore.data.map { it[Keys.PARAGRAPH_SPACING] ?: 1.0f }
    val markdownDialect: Flow<String> = dataStore.data.map { it[Keys.MARKDOWN_DIALECT] ?: "gfm" }
    val navBarStyle: Flow<String> = dataStore.data.map { it[Keys.NAV_BAR_STYLE] ?: "minimal" }
    val navBarGlassIntensity: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_GLASS_INTENSITY] ?: 50 }

    // ── 每种风格独立的调节参数 ──
    val cardMinimalTransparency: Flow<Int> = dataStore.data.map { it[Keys.CARD_MINIMAL_TRANSPARENCY] ?: 0 }
    val cardFrostedIntensity: Flow<Int> = dataStore.data.map { it[Keys.CARD_FROSTED_INTENSITY] ?: 150 }
    val cardLiquidIntensity: Flow<Int> = dataStore.data.map { it[Keys.CARD_LIQUID_INTENSITY] ?: 50 }

    val navBarMinimalTransparency: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_MINIMAL_TRANSPARENCY] ?: 0 }
    val navBarFrostedIntensity: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_FROSTED_INTENSITY] ?: 150 }
    val navBarLiquidIntensity: Flow<Int> = dataStore.data.map { it[Keys.NAV_BAR_LIQUID_INTENSITY] ?: 50 }
    val highlightLineColor: Flow<String> = dataStore.data.map { it[Keys.HIGHLIGHT_LINE_COLOR] ?: "#4C6EF5" }
    val indentType: Flow<String> = dataStore.data.map { it[Keys.INDENT_TYPE] ?: "spaces" }
    val indentSize: Flow<Int> = dataStore.data.map { it[Keys.INDENT_SIZE] ?: 4 }

    // ── 图床方案管理 ──
    private val gson = Gson()

    val imageHostProfiles: Flow<List<ImageHostProfile>> = dataStore.data.map { prefs ->
        val json = prefs[Keys.IMAGE_HOST_PROFILES] ?: ""
        if (json.isBlank()) emptyList()
        else {
            try {
                val type = object : TypeToken<List<ImageHostProfile>>() {}.type
                gson.fromJson<List<ImageHostProfile>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val imageHostActiveProfileId: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.IMAGE_HOST_ACTIVE_PROFILE_ID] ?: ""
    }

    val imageProcessingSettings: Flow<ImageProcessingSettings> = dataStore.data.map { prefs ->
        val json = prefs[Keys.IMAGE_PROCESSING_SETTINGS] ?: ""
        if (json.isBlank()) ImageProcessingSettings()
        else {
            try {
                gson.fromJson(json, ImageProcessingSettings::class.java) ?: ImageProcessingSettings()
            } catch (_: Exception) {
                ImageProcessingSettings()
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setThemeColorIndex(index: Int) {
        dataStore.edit { it[Keys.THEME_COLOR_INDEX] = index }
    }

    suspend fun setLightScheme(scheme: String) {
        dataStore.edit { it[Keys.LIGHT_SCHEME] = scheme }
    }

    suspend fun setDarkScheme(scheme: String) {
        dataStore.edit { it[Keys.DARK_SCHEME] = scheme }
    }

    suspend fun setCardStyle(style: String) {
        dataStore.edit { it[Keys.CARD_STYLE] = style }
    }

    suspend fun setCardGlassIntensity(intensity: Int) {
        dataStore.edit { it[Keys.CARD_GLASS_INTENSITY] = intensity }
    }

    suspend fun setCardTransparency(transparency: Int) {
        dataStore.edit { it[Keys.CARD_TRANSPARENCY] = transparency }
    }

    suspend fun setNavBarTransparency(transparency: Int) {
        dataStore.edit { it[Keys.NAV_BAR_TRANSPARENCY] = transparency }
    }

    suspend fun setFontSize(size: Int) {
        dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun setUiFontSize(size: Int) {
        dataStore.edit { it[Keys.UI_FONT_SIZE] = size }
    }

    suspend fun setReduceMotion(reduce: Boolean) {
        dataStore.edit { it[Keys.REDUCE_MOTION] = reduce }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setAutoSaveInterval(interval: Int) {
        dataStore.edit { it[Keys.AUTO_SAVE_INTERVAL] = interval }
    }

    suspend fun setAutoSaveIntervalUnit(unit: String) {
        dataStore.edit { it[Keys.AUTO_SAVE_INTERVAL_UNIT] = unit }
    }

    suspend fun setShowLineNumbers(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = show }
    }

    suspend fun setHighlightCurrentLine(highlight: Boolean) {
        dataStore.edit { it[Keys.HIGHLIGHT_CURRENT_LINE] = highlight }
    }

    suspend fun setAutoIndent(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_INDENT] = enabled }
    }

    suspend fun setBracketMatching(enabled: Boolean) {
        dataStore.edit { it[Keys.BRACKET_MATCHING] = enabled }
    }

    suspend fun setSpellCheck(enabled: Boolean) {
        dataStore.edit { it[Keys.SPELL_CHECK] = enabled }
    }

    suspend fun setCodeFolding(enabled: Boolean) {
        dataStore.edit { it[Keys.CODE_FOLDING] = enabled }
    }

    suspend fun setTabSize(size: Int) {
        dataStore.edit { it[Keys.TAB_SIZE] = size }
    }

    suspend fun setLineWrap(wrap: String) {
        dataStore.edit { it[Keys.LINE_WRAP] = wrap }
    }

    suspend fun setVersionSnapshot(enabled: Boolean) {
        dataStore.edit { it[Keys.VERSION_SNAPSHOT] = enabled }
    }

    suspend fun setSaveOnExitPrompt(prompt: Boolean) {
        dataStore.edit { it[Keys.SAVE_ON_EXIT_PROMPT] = prompt }
    }

    suspend fun setSyncScroll(enabled: Boolean) {
        dataStore.edit { it[Keys.SYNC_SCROLL] = enabled }
    }

    suspend fun setPreviewTheme(theme: String) {
        dataStore.edit { it[Keys.PREVIEW_THEME] = theme }
    }

    suspend fun setWorkspacePath(path: String?) {
        dataStore.edit {
            if (path != null) it[Keys.WORKSPACE_PATH] = path
            else it.remove(Keys.WORKSPACE_PATH)
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setImageHostEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.IMAGE_HOST_ENABLED] = enabled }
    }

    suspend fun setImageHostProvider(provider: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_PROVIDER] = provider }
    }

    suspend fun setImageHostGithubRepo(repo: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_GITHUB_REPO] = repo }
    }

    suspend fun setImageHostGithubBranch(branch: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_GITHUB_BRANCH] = branch }
    }

    suspend fun setImageHostGithubDomain(domain: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_GITHUB_DOMAIN] = domain }
    }

    suspend fun setImageHostQiniuAk(ak: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_QINIU_AK] = ak }
    }

    suspend fun setImageHostQiniuBucket(bucket: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_QINIU_BUCKET] = bucket }
    }

    suspend fun setImageHostQiniuDomain(domain: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_QINIU_DOMAIN] = domain }
    }

    suspend fun setImageHostAliyunAkId(id: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_ALIYUN_AK_ID] = id }
    }

    suspend fun setImageHostAliyunBucket(bucket: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_ALIYUN_BUCKET] = bucket }
    }

    suspend fun setImageHostAliyunRegion(region: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_ALIYUN_REGION] = region }
    }

    suspend fun setImageHostAliyunDomain(domain: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_ALIYUN_DOMAIN] = domain }
    }

    suspend fun setImageHostTencentBucket(bucket: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_TENCENT_BUCKET] = bucket }
    }

    suspend fun setImageHostTencentRegion(region: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_TENCENT_REGION] = region }
    }

    suspend fun setImageHostTencentDomain(domain: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_TENCENT_DOMAIN] = domain }
    }

    suspend fun setImageHostCustomUrl(url: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_CUSTOM_URL] = url }
    }

    suspend fun setImageHostCustomMethod(method: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_CUSTOM_METHOD] = method }
    }

    suspend fun setImageHostCustomHeaders(headers: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_CUSTOM_HEADERS] = headers }
    }

    suspend fun setImageHostCustomResponse(rule: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_CUSTOM_RESPONSE] = rule }
    }

    suspend fun setImageCompress(enabled: Boolean) {
        dataStore.edit { it[Keys.IMAGE_COMPRESS] = enabled }
    }

    suspend fun setImageCompressQuality(quality: Int) {
        dataStore.edit { it[Keys.IMAGE_COMPRESS_QUALITY] = quality }
    }

    suspend fun setImageResize(enabled: Boolean) {
        dataStore.edit { it[Keys.IMAGE_RESIZE] = enabled }
    }

    suspend fun setImageResizeMaxWidth(width: Int) {
        dataStore.edit { it[Keys.IMAGE_RESIZE_MAX_WIDTH] = width }
    }

    suspend fun setImageResizeMaxHeight(height: Int) {
        dataStore.edit { it[Keys.IMAGE_RESIZE_MAX_HEIGHT] = height }
    }

    suspend fun setImageWatermark(enabled: Boolean) {
        dataStore.edit { it[Keys.IMAGE_WATERMARK] = enabled }
    }

    suspend fun setImageWatermarkText(text: String) {
        dataStore.edit { it[Keys.IMAGE_WATERMARK_TEXT] = text }
    }

    suspend fun setImageAutoRename(enabled: Boolean) {
        dataStore.edit { it[Keys.IMAGE_AUTO_RENAME] = enabled }
    }

    suspend fun setImageRenameRule(rule: String) {
        dataStore.edit { it[Keys.IMAGE_RENAME_RULE] = rule }
    }

    suspend fun setHardwareAccel(enabled: Boolean) {
        dataStore.edit { it[Keys.HARDWARE_ACCEL] = enabled }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.edit { it[Keys.DEBUG_MODE] = enabled }
    }

    suspend fun setLogLevel(level: String) {
        dataStore.edit { it[Keys.LOG_LEVEL] = level }
    }

    suspend fun setDebugLogOutput(enabled: Boolean) {
        dataStore.edit { it[Keys.DEBUG_LOG_OUTPUT] = enabled }
    }

    suspend fun setButtonStyle(style: String) {
        dataStore.edit { it[Keys.BUTTON_STYLE] = style }
    }

    suspend fun setUiFont(font: String) {
        dataStore.edit { it[Keys.UI_FONT] = font }
    }

    suspend fun setEditorFont(font: String) {
        dataStore.edit { it[Keys.EDITOR_FONT] = font }
    }

    suspend fun setCodeFont(font: String) {
        dataStore.edit { it[Keys.CODE_FONT] = font }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        dataStore.edit {
            if (uri != null) it[Keys.BACKGROUND_IMAGE_URI] = uri
            else it.remove(Keys.BACKGROUND_IMAGE_URI)
        }
    }

    suspend fun setBackgroundBlur(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKGROUND_BLUR] = enabled }
    }

    suspend fun setBackgroundBlurIntensity(intensity: Int) {
        dataStore.edit { it[Keys.BACKGROUND_BLUR_INTENSITY] = intensity }
    }

    suspend fun setBackgroundBrightness(brightness: Int) {
        dataStore.edit { it[Keys.BACKGROUND_BRIGHTNESS] = brightness }
    }

    suspend fun setEditorBackgroundImageUri(uri: String?) {
        dataStore.edit { if (uri != null) it[Keys.EDITOR_BACKGROUND_IMAGE_URI] = uri else it.remove(Keys.EDITOR_BACKGROUND_IMAGE_URI) }
    }

    suspend fun setEditorBackgroundBlur(enabled: Boolean) {
        dataStore.edit { it[Keys.EDITOR_BACKGROUND_BLUR] = enabled }
    }

    suspend fun setEditorBackgroundBlurIntensity(intensity: Int) {
        dataStore.edit { it[Keys.EDITOR_BACKGROUND_BLUR_INTENSITY] = intensity }
    }

    suspend fun setEditorBackgroundBrightness(brightness: Int) {
        dataStore.edit { it[Keys.EDITOR_BACKGROUND_BRIGHTNESS] = brightness }
    }

    suspend fun setCardBgColor(color: Int) {
        dataStore.edit { it[Keys.CARD_BG_COLOR] = color }
    }

    suspend fun setNavBarColor(color: Int) {
        dataStore.edit { it[Keys.NAV_BAR_COLOR] = color }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setLineHeight(height: Float) {
        dataStore.edit { it[Keys.LINE_HEIGHT] = height }
    }

    suspend fun setLetterSpacing(spacing: Float) {
        dataStore.edit { it[Keys.LETTER_SPACING] = spacing }
    }

    suspend fun setMarkdownDialect(dialect: String) {
        dataStore.edit { it[Keys.MARKDOWN_DIALECT] = dialect }
    }

    suspend fun setParagraphSpacing(spacing: Float) {
        dataStore.edit { it[Keys.PARAGRAPH_SPACING] = spacing }
    }

    suspend fun setNavBarStyle(style: String) {
        dataStore.edit { it[Keys.NAV_BAR_STYLE] = style }
    }

    suspend fun setNavBarGlassIntensity(intensity: Int) {
        dataStore.edit { it[Keys.NAV_BAR_GLASS_INTENSITY] = intensity }
    }

    // ── 每种风格独立的调节参数 setter ──
    suspend fun setCardMinimalTransparency(value: Int) { dataStore.edit { it[Keys.CARD_MINIMAL_TRANSPARENCY] = value } }
    suspend fun setCardFrostedIntensity(value: Int) { dataStore.edit { it[Keys.CARD_FROSTED_INTENSITY] = value } }
    suspend fun setCardLiquidIntensity(value: Int) { dataStore.edit { it[Keys.CARD_LIQUID_INTENSITY] = value } }
    suspend fun setNavBarMinimalTransparency(value: Int) { dataStore.edit { it[Keys.NAV_BAR_MINIMAL_TRANSPARENCY] = value } }
    suspend fun setNavBarFrostedIntensity(value: Int) { dataStore.edit { it[Keys.NAV_BAR_FROSTED_INTENSITY] = value } }
    suspend fun setNavBarLiquidIntensity(value: Int) { dataStore.edit { it[Keys.NAV_BAR_LIQUID_INTENSITY] = value } }
    suspend fun setHighlightLineColor(color: String) { dataStore.edit { it[Keys.HIGHLIGHT_LINE_COLOR] = color } }
    suspend fun setIndentType(type: String) { dataStore.edit { it[Keys.INDENT_TYPE] = type } }
    suspend fun setIndentSize(size: Int) { dataStore.edit { it[Keys.INDENT_SIZE] = size } }

    // ── 图床方案管理 ──
    suspend fun setImageHostProfiles(profiles: List<ImageHostProfile>) {
        dataStore.edit { it[Keys.IMAGE_HOST_PROFILES] = gson.toJson(profiles) }
    }

    suspend fun setImageHostActiveProfileId(id: String) {
        dataStore.edit { it[Keys.IMAGE_HOST_ACTIVE_PROFILE_ID] = id }
    }

    suspend fun setImageProcessingSettings(settings: ImageProcessingSettings) {
        dataStore.edit { it[Keys.IMAGE_PROCESSING_SETTINGS] = gson.toJson(settings) }
    }

    // ── 云同步设置 ──
    val cloudSyncServiceType: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_SERVICE_TYPE] ?: "WEBDAV" }
    val cloudSyncServerUrl: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_SERVER_URL] ?: "" }
    val cloudSyncPort: Flow<Int> = dataStore.data.map { it[Keys.CLOUD_SYNC_PORT] ?: 0 }
    val cloudSyncUsername: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_USERNAME] ?: "" }
    val cloudSyncRemotePath: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_REMOTE_PATH] ?: "/Mdcito" }
    val cloudSyncMode: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_MODE] ?: "MANUAL" }
    val cloudSyncAutoSyncInterval: Flow<Int> = dataStore.data.map { it[Keys.CLOUD_SYNC_AUTO_SYNC_INTERVAL] ?: 30 }
    val cloudSyncWifiOnly: Flow<Boolean> = dataStore.data.map { it[Keys.CLOUD_SYNC_WIFI_ONLY] ?: true }
    val cloudSyncChargingOnly: Flow<Boolean> = dataStore.data.map { it[Keys.CLOUD_SYNC_CHARGING_ONLY] ?: false }
    val cloudSyncFileFilterRules: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_FILE_FILTER_RULES] ?: "" }
    val cloudSyncLastSyncTime: Flow<Long> = dataStore.data.map { it[Keys.CLOUD_SYNC_LAST_SYNC_TIME] ?: 0L }
    val cloudSyncLastSyncStatus: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_LAST_SYNC_STATUS] ?: "" }
    val cloudSyncEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.CLOUD_SYNC_ENABLED] ?: false }
    val cloudSyncTokenExpiryTime: Flow<Long> = dataStore.data.map { it[Keys.CLOUD_SYNC_TOKEN_EXPIRY_TIME] ?: 0L }
    val cloudSyncConflictResolution: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_CONFLICT_RESOLUTION] ?: "NEWER_WINS" }
    val cloudSyncUseCustomAuthEndpoint: Flow<Boolean> = dataStore.data.map { it[Keys.CLOUD_SYNC_USE_CUSTOM_AUTH_ENDPOINT] ?: false }
    val cloudSyncCustomAuthEndpoint: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_CUSTOM_AUTH_ENDPOINT] ?: "" }
    val cloudSyncCustomTokenEndpoint: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_CUSTOM_TOKEN_ENDPOINT] ?: "" }
    val cloudSyncDeletions: Flow<Boolean> = dataStore.data.map { it[Keys.CLOUD_SYNC_DELETIONS] ?: true }
    val cloudSyncLastSyncedFiles: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_LAST_SYNCED_FILES] ?: "" }
    val cloudSyncHistory: Flow<String> = dataStore.data.map { it[Keys.CLOUD_SYNC_HISTORY] ?: "" }

    suspend fun setCloudSyncServiceType(type: String) { dataStore.edit { it[Keys.CLOUD_SYNC_SERVICE_TYPE] = type } }
    suspend fun setCloudSyncServerUrl(url: String) { dataStore.edit { it[Keys.CLOUD_SYNC_SERVER_URL] = url } }
    suspend fun setCloudSyncPort(port: Int) { dataStore.edit { it[Keys.CLOUD_SYNC_PORT] = port } }
    suspend fun setCloudSyncUsername(username: String) { dataStore.edit { it[Keys.CLOUD_SYNC_USERNAME] = username } }
    suspend fun setCloudSyncRemotePath(path: String) { dataStore.edit { it[Keys.CLOUD_SYNC_REMOTE_PATH] = path } }
    suspend fun setCloudSyncMode(mode: String) { dataStore.edit { it[Keys.CLOUD_SYNC_MODE] = mode } }
    suspend fun setCloudSyncAutoSyncInterval(interval: Int) { dataStore.edit { it[Keys.CLOUD_SYNC_AUTO_SYNC_INTERVAL] = interval } }
    suspend fun setCloudSyncWifiOnly(wifiOnly: Boolean) { dataStore.edit { it[Keys.CLOUD_SYNC_WIFI_ONLY] = wifiOnly } }
    suspend fun setCloudSyncChargingOnly(chargingOnly: Boolean) { dataStore.edit { it[Keys.CLOUD_SYNC_CHARGING_ONLY] = chargingOnly } }
    suspend fun setCloudSyncFileFilterRules(rules: String) { dataStore.edit { it[Keys.CLOUD_SYNC_FILE_FILTER_RULES] = rules } }
    suspend fun setCloudSyncLastSyncTime(time: Long) { dataStore.edit { it[Keys.CLOUD_SYNC_LAST_SYNC_TIME] = time } }
    suspend fun setCloudSyncLastSyncStatus(status: String) { dataStore.edit { it[Keys.CLOUD_SYNC_LAST_SYNC_STATUS] = status } }
    suspend fun setCloudSyncEnabled(enabled: Boolean) { dataStore.edit { it[Keys.CLOUD_SYNC_ENABLED] = enabled } }
    suspend fun setCloudSyncTokenExpiryTime(time: Long) { dataStore.edit { it[Keys.CLOUD_SYNC_TOKEN_EXPIRY_TIME] = time } }
    suspend fun setCloudSyncConflictResolution(resolution: String) { dataStore.edit { it[Keys.CLOUD_SYNC_CONFLICT_RESOLUTION] = resolution } }
    suspend fun setCloudSyncUseCustomAuthEndpoint(use: Boolean) { dataStore.edit { it[Keys.CLOUD_SYNC_USE_CUSTOM_AUTH_ENDPOINT] = use } }
    suspend fun setCloudSyncCustomAuthEndpoint(endpoint: String) { dataStore.edit { it[Keys.CLOUD_SYNC_CUSTOM_AUTH_ENDPOINT] = endpoint } }
    suspend fun setCloudSyncCustomTokenEndpoint(endpoint: String) { dataStore.edit { it[Keys.CLOUD_SYNC_CUSTOM_TOKEN_ENDPOINT] = endpoint } }
    suspend fun setCloudSyncDeletions(enabled: Boolean) { dataStore.edit { it[Keys.CLOUD_SYNC_DELETIONS] = enabled } }
    suspend fun setCloudSyncLastSyncedFiles(files: String) { dataStore.edit { it[Keys.CLOUD_SYNC_LAST_SYNCED_FILES] = files } }
    suspend fun setCloudSyncHistory(history: String) { dataStore.edit { it[Keys.CLOUD_SYNC_HISTORY] = history } }

    // ── 更新管理设置 ──
    val autoCheckUpdate: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_CHECK_UPDATE] ?: true }
    val updateSource: Flow<String> = dataStore.data.map { it[Keys.UPDATE_SOURCE] ?: "AUTO" }
    val lastUpdateCheckTime: Flow<Long> = dataStore.data.map { it[Keys.LAST_UPDATE_CHECK_TIME] ?: 0L }

    suspend fun setAutoCheckUpdate(enabled: Boolean) { dataStore.edit { it[Keys.AUTO_CHECK_UPDATE] = enabled } }
    suspend fun setUpdateSource(source: String) { dataStore.edit { it[Keys.UPDATE_SOURCE] = source } }
    suspend fun setLastUpdateCheckTime(time: Long) { dataStore.edit { it[Keys.LAST_UPDATE_CHECK_TIME] = time } }

    // ── 过渡动画设置 ──
    val splashAnimationEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SPLASH_ANIMATION_ENABLED] ?: true }
    suspend fun setSplashAnimationEnabled(enabled: Boolean) { dataStore.edit { it[Keys.SPLASH_ANIMATION_ENABLED] = enabled } }

    /**
     * 将所有设置项恢复为默认值。
     * 通过一次性编辑 DataStore，将所有 Key 重置为初始默认值。
     */
    suspend fun resetAllToDefaults() {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = "SYSTEM"
            prefs[Keys.THEME_COLOR_INDEX] = 0
            prefs[Keys.LIGHT_SCHEME] = "warm"
            prefs[Keys.DARK_SCHEME] = "warm_dark"
            prefs[Keys.CARD_STYLE] = "minimal"
            prefs[Keys.CARD_GLASS_INTENSITY] = 50
            prefs[Keys.CARD_TRANSPARENCY] = 100
            prefs[Keys.NAV_BAR_TRANSPARENCY] = 100
            prefs[Keys.BUTTON_STYLE] = "default"
            prefs[Keys.FONT_SIZE] = 14
            prefs[Keys.UI_FONT_SIZE] = 14
            prefs[Keys.UI_FONT] = "system"
            prefs[Keys.EDITOR_FONT] = "system"
            prefs[Keys.CODE_FONT] = "jetbrains_mono"
            prefs[Keys.REDUCE_MOTION] = false
            prefs[Keys.AUTO_SAVE] = false
            prefs[Keys.AUTO_SAVE_INTERVAL] = 5
            prefs[Keys.AUTO_SAVE_INTERVAL_UNIT] = "seconds"
            prefs[Keys.SHOW_LINE_NUMBERS] = false
            prefs[Keys.HIGHLIGHT_CURRENT_LINE] = true
            prefs[Keys.AUTO_INDENT] = true
            prefs[Keys.BRACKET_MATCHING] = true
            prefs[Keys.SPELL_CHECK] = false
            prefs[Keys.CODE_FOLDING] = true
            prefs[Keys.TAB_SIZE] = 4
            prefs[Keys.LINE_WRAP] = "soft"
            prefs[Keys.VERSION_SNAPSHOT] = true
            prefs[Keys.SAVE_ON_EXIT_PROMPT] = true
            prefs[Keys.SYNC_SCROLL] = true
            prefs[Keys.PREVIEW_THEME] = "follow"
            prefs.remove(Keys.WORKSPACE_PATH)
            prefs[Keys.DYNAMIC_COLOR] = false
            prefs[Keys.IMAGE_HOST_ENABLED] = false
            prefs[Keys.IMAGE_HOST_PROVIDER] = "github"
            prefs[Keys.IMAGE_HOST_GITHUB_REPO] = ""
            prefs[Keys.IMAGE_HOST_GITHUB_BRANCH] = "main"
            prefs[Keys.IMAGE_HOST_GITHUB_DOMAIN] = ""
            prefs[Keys.IMAGE_HOST_QINIU_AK] = ""
            prefs[Keys.IMAGE_HOST_QINIU_BUCKET] = ""
            prefs[Keys.IMAGE_HOST_QINIU_DOMAIN] = ""
            prefs[Keys.IMAGE_HOST_ALIYUN_AK_ID] = ""
            prefs[Keys.IMAGE_HOST_ALIYUN_BUCKET] = ""
            prefs[Keys.IMAGE_HOST_ALIYUN_REGION] = ""
            prefs[Keys.IMAGE_HOST_ALIYUN_DOMAIN] = ""
            prefs[Keys.IMAGE_HOST_TENCENT_BUCKET] = ""
            prefs[Keys.IMAGE_HOST_TENCENT_REGION] = ""
            prefs[Keys.IMAGE_HOST_TENCENT_DOMAIN] = ""
            prefs[Keys.IMAGE_HOST_CUSTOM_URL] = ""
            prefs[Keys.IMAGE_HOST_CUSTOM_METHOD] = "POST"
            prefs[Keys.IMAGE_HOST_CUSTOM_HEADERS] = ""
            prefs[Keys.IMAGE_HOST_CUSTOM_RESPONSE] = ""
            prefs[Keys.IMAGE_COMPRESS] = false
            prefs[Keys.IMAGE_COMPRESS_QUALITY] = 80
            prefs[Keys.IMAGE_RESIZE] = false
            prefs[Keys.IMAGE_RESIZE_MAX_WIDTH] = 1920
            prefs[Keys.IMAGE_RESIZE_MAX_HEIGHT] = 1080
            prefs[Keys.IMAGE_WATERMARK] = false
            prefs[Keys.IMAGE_WATERMARK_TEXT] = ""
            prefs[Keys.IMAGE_AUTO_RENAME] = true
            prefs[Keys.IMAGE_RENAME_RULE] = "timestamp"
            prefs[Keys.HARDWARE_ACCEL] = true
            prefs[Keys.DEBUG_MODE] = false
            prefs[Keys.LOG_LEVEL] = "debug"
            prefs[Keys.DEBUG_LOG_OUTPUT] = false
            prefs.remove(Keys.BACKGROUND_IMAGE_URI)
            prefs[Keys.BACKGROUND_BLUR] = false
            prefs[Keys.BACKGROUND_BLUR_INTENSITY] = 10
            prefs[Keys.BACKGROUND_BRIGHTNESS] = 100
            prefs.remove(Keys.EDITOR_BACKGROUND_IMAGE_URI)
            prefs[Keys.EDITOR_BACKGROUND_BLUR] = false
            prefs[Keys.EDITOR_BACKGROUND_BLUR_INTENSITY] = 10
            prefs[Keys.EDITOR_BACKGROUND_BRIGHTNESS] = 100
            prefs[Keys.CARD_BG_COLOR] = -1
            prefs[Keys.NAV_BAR_COLOR] = -1
            prefs[Keys.ONBOARDING_COMPLETED] = true // 保留引导完成状态，避免重置后重新进入引导页
            prefs[Keys.LINE_HEIGHT] = 1.7f
            prefs[Keys.LETTER_SPACING] = 0.0f
            prefs[Keys.PARAGRAPH_SPACING] = 1.0f
            prefs[Keys.MARKDOWN_DIALECT] = "gfm"
            prefs[Keys.NAV_BAR_STYLE] = "minimal"
            prefs[Keys.NAV_BAR_GLASS_INTENSITY] = 50
            prefs[Keys.CARD_MINIMAL_TRANSPARENCY] = 0
            prefs[Keys.CARD_FROSTED_INTENSITY] = 150
            prefs[Keys.CARD_LIQUID_INTENSITY] = 50
            prefs[Keys.NAV_BAR_MINIMAL_TRANSPARENCY] = 0
            prefs[Keys.NAV_BAR_FROSTED_INTENSITY] = 150
            prefs[Keys.NAV_BAR_LIQUID_INTENSITY] = 50
            prefs[Keys.HIGHLIGHT_LINE_COLOR] = "#4C6EF5"
            prefs[Keys.INDENT_TYPE] = "spaces"
            prefs[Keys.INDENT_SIZE] = 4
            prefs.remove(Keys.IMAGE_HOST_PROFILES)
            prefs.remove(Keys.IMAGE_HOST_ACTIVE_PROFILE_ID)
            prefs.remove(Keys.IMAGE_PROCESSING_SETTINGS)

            // ── 云同步设置 ──
            prefs[Keys.CLOUD_SYNC_SERVICE_TYPE] = "WEBDAV"
            prefs[Keys.CLOUD_SYNC_SERVER_URL] = ""
            prefs[Keys.CLOUD_SYNC_PORT] = 0
            prefs[Keys.CLOUD_SYNC_USERNAME] = ""
            prefs[Keys.CLOUD_SYNC_REMOTE_PATH] = "/Mdcito"
            prefs[Keys.CLOUD_SYNC_MODE] = "MANUAL"
            prefs[Keys.CLOUD_SYNC_AUTO_SYNC_INTERVAL] = 30
            prefs[Keys.CLOUD_SYNC_WIFI_ONLY] = true
            prefs[Keys.CLOUD_SYNC_CHARGING_ONLY] = false
            prefs[Keys.CLOUD_SYNC_FILE_FILTER_RULES] = ""
            prefs[Keys.CLOUD_SYNC_LAST_SYNC_TIME] = 0L
            prefs[Keys.CLOUD_SYNC_LAST_SYNC_STATUS] = ""
            prefs[Keys.CLOUD_SYNC_ENABLED] = false
            prefs[Keys.CLOUD_SYNC_TOKEN_EXPIRY_TIME] = 0L
            prefs[Keys.CLOUD_SYNC_CONFLICT_RESOLUTION] = "NEWER_WINS"
            prefs[Keys.CLOUD_SYNC_USE_CUSTOM_AUTH_ENDPOINT] = false
            prefs[Keys.CLOUD_SYNC_CUSTOM_AUTH_ENDPOINT] = ""
            prefs[Keys.CLOUD_SYNC_CUSTOM_TOKEN_ENDPOINT] = ""
            prefs[Keys.CLOUD_SYNC_DELETIONS] = true
            prefs[Keys.CLOUD_SYNC_LAST_SYNCED_FILES] = ""
            prefs[Keys.CLOUD_SYNC_HISTORY] = ""

            // ── 更新管理设置 ──
            prefs[Keys.AUTO_CHECK_UPDATE] = true
            prefs[Keys.UPDATE_SOURCE] = "AUTO"
            prefs[Keys.LAST_UPDATE_CHECK_TIME] = 0L

            // ── 过渡动画设置 ──
            prefs[Keys.SPLASH_ANIMATION_ENABLED] = true
        }
    }
}
