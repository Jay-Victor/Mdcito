package com.mdcito.app.ui.settings

import android.app.Application
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.data.db.MdcitoDatabase
import com.mdcito.app.data.font.FontService
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.SettingsRepository
import com.mdcito.app.data.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository,
    private val versionRepository: VersionRepository,
    private val database: MdcitoDatabase,
    val fontService: FontService,
) : ViewModel() {

    sealed class ClearVersionEvent {
        data class Success(val count: Int) : ClearVersionEvent()
        data class Error(val message: String) : ClearVersionEvent()
    }

    sealed class ClearCacheEvent {
        data class Success(val freedSize: String) : ClearCacheEvent()
        data class Error(val message: String) : ClearCacheEvent()
    }

    sealed class WorkspaceEvent {
        data object Restored : WorkspaceEvent()
        data class Error(val message: String) : WorkspaceEvent()
    }

    sealed class ResetSettingsEvent {
        data object Success : ResetSettingsEvent()
        data class Error(val message: String) : ResetSettingsEvent()
    }

    sealed class ClearAllDataEvent {
        data object Success : ClearAllDataEvent()
        data class Error(val message: String) : ClearAllDataEvent()
    }

    private val _clearVersionEvent = MutableSharedFlow<ClearVersionEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val clearVersionEvent: SharedFlow<ClearVersionEvent> = _clearVersionEvent

    private val _clearCacheEvent = MutableSharedFlow<ClearCacheEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val clearCacheEvent: SharedFlow<ClearCacheEvent> = _clearCacheEvent

    private val _workspaceEvent = MutableSharedFlow<WorkspaceEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val workspaceEvent: SharedFlow<WorkspaceEvent> = _workspaceEvent

    private val _resetSettingsEvent = MutableSharedFlow<ResetSettingsEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val resetSettingsEvent: SharedFlow<ResetSettingsEvent> = _resetSettingsEvent

    private val _clearAllDataEvent = MutableSharedFlow<ClearAllDataEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val clearAllDataEvent: SharedFlow<ClearAllDataEvent> = _clearAllDataEvent

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _isClearingVersions = MutableStateFlow(false)
    val isClearingVersions: StateFlow<Boolean> = _isClearingVersions.asStateFlow()

    private val _isRestoringWorkspace = MutableStateFlow(false)
    val isRestoringWorkspace: StateFlow<Boolean> = _isRestoringWorkspace.asStateFlow()

    private val _cacheSize = MutableStateFlow("")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _snapshotCount = MutableStateFlow(0)
    val snapshotCount: StateFlow<Int> = _snapshotCount.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSize.value = calculateCacheSize()
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _snapshotCount.value = versionRepository.getCount()
            } catch (e: Exception) {
                Timber.tag("Settings").w(e, "获取版本快照数量失败")
                _snapshotCount.value = 0
            }
        }
    }

    val workspacePath: StateFlow<String?> = settingsRepository.workspacePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val themeColorIndex: StateFlow<Int> = settingsRepository.themeColorIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lightScheme: StateFlow<String> = settingsRepository.lightScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "warm")

    val darkScheme: StateFlow<String> = settingsRepository.darkScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "warm_dark")

    val cardStyle: StateFlow<String> = settingsRepository.cardStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "minimal")

    val cardGlassIntensity: StateFlow<Int> = settingsRepository.cardGlassIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val cardTransparency: StateFlow<Int> = settingsRepository.cardTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val navBarTransparency: StateFlow<Int> = settingsRepository.navBarTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val navBarStyle: StateFlow<String> = settingsRepository.navBarStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "minimal")

    val navBarGlassIntensity: StateFlow<Int> = settingsRepository.navBarGlassIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    // ── 每种风格独立的调节参数 ──
    val cardMinimalTransparency: StateFlow<Int> = settingsRepository.cardMinimalTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)
    val cardFrostedIntensity: StateFlow<Int> = settingsRepository.cardFrostedIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)
    val cardLiquidIntensity: StateFlow<Int> = settingsRepository.cardLiquidIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)
    val navBarMinimalTransparency: StateFlow<Int> = settingsRepository.navBarMinimalTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)
    val navBarFrostedIntensity: StateFlow<Int> = settingsRepository.navBarFrostedIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)
    val navBarLiquidIntensity: StateFlow<Int> = settingsRepository.navBarLiquidIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)
    val highlightLineColor: StateFlow<String> = settingsRepository.highlightLineColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#4C6EF5")
    val indentType: StateFlow<String> = settingsRepository.indentType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "spaces")
    val indentSize: StateFlow<Int> = settingsRepository.indentSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val buttonStyle: StateFlow<String> = settingsRepository.buttonStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")

    val fontSize: StateFlow<Int> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)

    val uiFont: StateFlow<String> = settingsRepository.uiFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val uiFontSize: StateFlow<Int> = settingsRepository.uiFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)

    val editorFont: StateFlow<String> = settingsRepository.editorFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val lineHeight: StateFlow<Float> = settingsRepository.lineHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.7f)

    val letterSpacing: StateFlow<Float> = settingsRepository.letterSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

    val markdownDialect: StateFlow<String> = settingsRepository.markdownDialect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gfm")

    val codeFont: StateFlow<String> = settingsRepository.codeFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "jetbrains_mono")

    val reduceMotion: StateFlow<Boolean> = settingsRepository.reduceMotion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dynamicColor: StateFlow<Boolean> = settingsRepository.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSave: StateFlow<Boolean> = settingsRepository.autoSave
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSaveInterval: StateFlow<Int> = settingsRepository.autoSaveInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val autoSaveIntervalUnit: StateFlow<String> = settingsRepository.autoSaveIntervalUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "seconds")

    val showLineNumbers: StateFlow<Boolean> = settingsRepository.showLineNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val highlightCurrentLine: StateFlow<Boolean> = settingsRepository.highlightCurrentLine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoIndent: StateFlow<Boolean> = settingsRepository.autoIndent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val bracketMatching: StateFlow<Boolean> = settingsRepository.bracketMatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val spellCheck: StateFlow<Boolean> = settingsRepository.spellCheck
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val tabSize: StateFlow<Int> = settingsRepository.tabSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val lineWrap: StateFlow<String> = settingsRepository.lineWrap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "soft")

    val versionSnapshot: StateFlow<Boolean> = settingsRepository.versionSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val saveOnExitPrompt: StateFlow<Boolean> = settingsRepository.saveOnExitPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val syncScroll: StateFlow<Boolean> = settingsRepository.syncScroll
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val previewTheme: StateFlow<String> = settingsRepository.previewTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "follow")

    val imageHostEnabled: StateFlow<Boolean> = settingsRepository.imageHostEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val imageHostProvider: StateFlow<String> = settingsRepository.imageHostProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "github")

    val imageHostGithubRepo: StateFlow<String> = settingsRepository.imageHostGithubRepo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostGithubBranch: StateFlow<String> = settingsRepository.imageHostGithubBranch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "main")

    val imageHostGithubToken: StateFlow<String> = settingsRepository.imageHostGithubToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostGithubDomain: StateFlow<String> = settingsRepository.imageHostGithubDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostQiniuAk: StateFlow<String> = settingsRepository.imageHostQiniuAk
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostQiniuSk: StateFlow<String> = settingsRepository.imageHostQiniuSk
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostQiniuBucket: StateFlow<String> = settingsRepository.imageHostQiniuBucket
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostQiniuDomain: StateFlow<String> = settingsRepository.imageHostQiniuDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostSmmsToken: StateFlow<String> = settingsRepository.imageHostSmmsToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostAliyunAkId: StateFlow<String> = settingsRepository.imageHostAliyunAkId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostAliyunAkSecret: StateFlow<String> = settingsRepository.imageHostAliyunAkSecret
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostAliyunBucket: StateFlow<String> = settingsRepository.imageHostAliyunBucket
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostAliyunRegion: StateFlow<String> = settingsRepository.imageHostAliyunRegion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostAliyunDomain: StateFlow<String> = settingsRepository.imageHostAliyunDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostTencentSecretId: StateFlow<String> = settingsRepository.imageHostTencentSecretId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostTencentSecretKey: StateFlow<String> = settingsRepository.imageHostTencentSecretKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostTencentBucket: StateFlow<String> = settingsRepository.imageHostTencentBucket
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostTencentRegion: StateFlow<String> = settingsRepository.imageHostTencentRegion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostTencentDomain: StateFlow<String> = settingsRepository.imageHostTencentDomain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostCustomUrl: StateFlow<String> = settingsRepository.imageHostCustomUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageHostCustomMethod: StateFlow<String> = settingsRepository.imageHostCustomMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "POST")

    // 直接使用 Repository 暴露的 StateFlow（已从加密存储读取）
    val imageHostCustomHeaders: StateFlow<String> = settingsRepository.imageHostCustomHeaders

    val imageHostCustomResponse: StateFlow<String> = settingsRepository.imageHostCustomResponse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageCompress: StateFlow<Boolean> = settingsRepository.imageCompress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val imageCompressQuality: StateFlow<Int> = settingsRepository.imageCompressQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    val imageResize: StateFlow<Boolean> = settingsRepository.imageResize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val imageWatermark: StateFlow<Boolean> = settingsRepository.imageWatermark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val imageWatermarkText: StateFlow<String> = settingsRepository.imageWatermarkText
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val imageAutoRename: StateFlow<Boolean> = settingsRepository.imageAutoRename
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val imageRenameRule: StateFlow<String> = settingsRepository.imageRenameRule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "timestamp")

    val hardwareAccel: StateFlow<Boolean> = settingsRepository.hardwareAccel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val debugMode: StateFlow<Boolean> = settingsRepository.debugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val logLevel: StateFlow<String> = settingsRepository.logLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "warning")

    val debugLogOutput: StateFlow<Boolean> = settingsRepository.debugLogOutput
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backgroundImageUri: StateFlow<String?> = settingsRepository.backgroundImageUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val backgroundBlur: StateFlow<Boolean> = settingsRepository.backgroundBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backgroundBlurIntensity: StateFlow<Int> = settingsRepository.backgroundBlurIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val backgroundBrightness: StateFlow<Int> = settingsRepository.backgroundBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val editorBackgroundImageUri: StateFlow<String?> = settingsRepository.editorBackgroundImageUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val editorBackgroundBlur: StateFlow<Boolean> = settingsRepository.editorBackgroundBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val editorBackgroundBlurIntensity: StateFlow<Int> = settingsRepository.editorBackgroundBlurIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val editorBackgroundBrightness: StateFlow<Int> = settingsRepository.editorBackgroundBrightness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val cardBgColor: StateFlow<Int> = settingsRepository.cardBgColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val navBarColor: StateFlow<Int> = settingsRepository.navBarColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val splashAnimationEnabled: StateFlow<Boolean> = settingsRepository.splashAnimationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            Timber.tag("Settings").d("切换主题模式：$mode")
        }
    }

    fun setThemeColorIndex(index: Int) {
        viewModelScope.launch { settingsRepository.setThemeColorIndex(index) }
    }

    fun setLightScheme(scheme: String) {
        viewModelScope.launch { settingsRepository.setLightScheme(scheme) }
    }

    fun setDarkScheme(scheme: String) {
        viewModelScope.launch { settingsRepository.setDarkScheme(scheme) }
    }

    fun setCardStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.setCardStyle(style)
            Timber.tag("Settings").d("切换卡片风格：$style")
        }
    }

    fun setCardGlassIntensity(intensity: Int) {
        viewModelScope.launch { settingsRepository.setCardGlassIntensity(intensity) }
    }

    fun setCardTransparency(transparency: Int) {
        viewModelScope.launch { settingsRepository.setCardTransparency(transparency) }
    }

    fun setNavBarTransparency(transparency: Int) {
        viewModelScope.launch { settingsRepository.setNavBarTransparency(transparency) }
    }

    fun setNavBarStyle(style: String) {
        viewModelScope.launch { settingsRepository.setNavBarStyle(style) }
    }

    fun setNavBarGlassIntensity(intensity: Int) {
        viewModelScope.launch { settingsRepository.setNavBarGlassIntensity(intensity) }
    }

    // ── 每种风格独立的调节参数 setter ──
    fun setCardMinimalTransparency(value: Int) { viewModelScope.launch { settingsRepository.setCardMinimalTransparency(value) } }
    fun setCardFrostedIntensity(value: Int) { viewModelScope.launch { settingsRepository.setCardFrostedIntensity(value) } }
    fun setCardLiquidIntensity(value: Int) { viewModelScope.launch { settingsRepository.setCardLiquidIntensity(value) } }
    fun setNavBarMinimalTransparency(value: Int) { viewModelScope.launch { settingsRepository.setNavBarMinimalTransparency(value) } }
    fun setNavBarFrostedIntensity(value: Int) { viewModelScope.launch { settingsRepository.setNavBarFrostedIntensity(value) } }
    fun setNavBarLiquidIntensity(value: Int) { viewModelScope.launch { settingsRepository.setNavBarLiquidIntensity(value) } }
    fun setHighlightLineColor(color: String) { viewModelScope.launch { settingsRepository.setHighlightLineColor(color) } }
    fun setIndentType(type: String) { viewModelScope.launch { settingsRepository.setIndentType(type) } }
    fun setIndentSize(size: Int) { viewModelScope.launch { settingsRepository.setIndentSize(size) } }

    fun setButtonStyle(style: String) {
        Timber.tag("Settings").d("按钮风格：$style")
        viewModelScope.launch { settingsRepository.setButtonStyle(style) }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setUiFont(font: String) {
        Timber.tag("Settings").d("UI 字体：$font")
        viewModelScope.launch { settingsRepository.setUiFont(font) }
    }

    fun setUiFontSize(size: Int) {
        Timber.tag("Settings").d("UI 字体大小：$size")
        viewModelScope.launch { settingsRepository.setUiFontSize(size) }
    }

    fun setEditorFont(font: String) {
        Timber.tag("Settings").d("编辑器字体：$font")
        viewModelScope.launch { settingsRepository.setEditorFont(font) }
    }

    fun setLineHeight(height: Float) {
        Timber.tag("Settings").d("行高：$height")
        viewModelScope.launch { settingsRepository.setLineHeight(height) }
    }

    fun setLetterSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepository.setLetterSpacing(spacing) }
    }

    fun setMarkdownDialect(dialect: String) {
        Timber.tag("Settings").d("Markdown 方言：$dialect")
        viewModelScope.launch { settingsRepository.setMarkdownDialect(dialect) }
    }

    fun setCodeFont(font: String) {
        Timber.tag("Settings").d("代码字体：$font")
        viewModelScope.launch { settingsRepository.setCodeFont(font) }
    }

    fun setReduceMotion(reduce: Boolean) {
        Timber.tag("Settings").d("减少动画：${if (reduce) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setReduceMotion(reduce) }
    }

    fun setDynamicColor(enabled: Boolean) {
        Timber.tag("Settings").d("动态取色：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setAutoSave(enabled: Boolean) {
        Timber.tag("Settings").d("自动保存：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setAutoSave(enabled) }
    }

    fun setAutoSaveInterval(interval: Int) {
        Timber.tag("Settings").d("自动保存间隔：$interval")
        viewModelScope.launch { settingsRepository.setAutoSaveInterval(interval) }
    }

    fun setAutoSaveIntervalUnit(unit: String) {
        viewModelScope.launch { settingsRepository.setAutoSaveIntervalUnit(unit) }
    }

    fun setShowLineNumbers(show: Boolean) {
        Timber.tag("Settings").d("显示行号：${if (show) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setShowLineNumbers(show) }
    }

    fun setHighlightCurrentLine(highlight: Boolean) {
        viewModelScope.launch { settingsRepository.setHighlightCurrentLine(highlight) }
    }

    fun setAutoIndent(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoIndent(enabled) }
    }

    fun setBracketMatching(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBracketMatching(enabled) }
    }

    fun setSpellCheck(enabled: Boolean) {
        Timber.tag("Settings").d("拼写检查：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setSpellCheck(enabled) }
    }

    fun setTabSize(size: Int) {
        Timber.tag("Settings").d("Tab 大小：$size")
        viewModelScope.launch { settingsRepository.setTabSize(size) }
    }

    fun setLineWrap(wrap: String) {
        Timber.tag("Settings").d("换行方式：$wrap")
        viewModelScope.launch { settingsRepository.setLineWrap(wrap) }
    }

    fun setVersionSnapshot(enabled: Boolean) {
        Timber.tag("Settings").d("版本快照：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setVersionSnapshot(enabled) }
    }

    fun setSaveOnExitPrompt(prompt: Boolean) {
        viewModelScope.launch { settingsRepository.setSaveOnExitPrompt(prompt) }
    }

    fun setSyncScroll(enabled: Boolean) {
        Timber.tag("Settings").d("同步滚动：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setSyncScroll(enabled) }
    }

    fun setPreviewTheme(theme: String) {
        Timber.tag("Settings").d("预览主题：$theme")
        viewModelScope.launch { settingsRepository.setPreviewTheme(theme) }
    }

    fun setImageHostEnabled(enabled: Boolean) {
        Timber.tag("Settings").d("图床功能：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setImageHostEnabled(enabled) }
    }

    fun setImageHostProvider(provider: String) {
        viewModelScope.launch { settingsRepository.setImageHostProvider(provider) }
    }

    fun setImageHostGithubRepo(repo: String) {
        viewModelScope.launch { settingsRepository.setImageHostGithubRepo(repo) }
    }

    fun setImageHostGithubBranch(branch: String) {
        viewModelScope.launch { settingsRepository.setImageHostGithubBranch(branch) }
    }

    fun setImageHostGithubToken(token: String) {
        viewModelScope.launch { settingsRepository.setImageHostGithubToken(token) }
    }

    fun setImageHostGithubDomain(domain: String) {
        viewModelScope.launch { settingsRepository.setImageHostGithubDomain(domain) }
    }

    fun setImageHostQiniuAk(ak: String) {
        viewModelScope.launch { settingsRepository.setImageHostQiniuAk(ak) }
    }

    fun setImageHostQiniuSk(sk: String) {
        viewModelScope.launch { settingsRepository.setImageHostQiniuSk(sk) }
    }

    fun setImageHostQiniuBucket(bucket: String) {
        viewModelScope.launch { settingsRepository.setImageHostQiniuBucket(bucket) }
    }

    fun setImageHostQiniuDomain(domain: String) {
        viewModelScope.launch { settingsRepository.setImageHostQiniuDomain(domain) }
    }

    fun setImageHostSmmsToken(token: String) {
        viewModelScope.launch { settingsRepository.setImageHostSmmsToken(token) }
    }

    fun setImageHostAliyunAkId(id: String) {
        viewModelScope.launch { settingsRepository.setImageHostAliyunAkId(id) }
    }

    fun setImageHostAliyunAkSecret(secret: String) {
        viewModelScope.launch { settingsRepository.setImageHostAliyunAkSecret(secret) }
    }

    fun setImageHostAliyunBucket(bucket: String) {
        viewModelScope.launch { settingsRepository.setImageHostAliyunBucket(bucket) }
    }

    fun setImageHostAliyunRegion(region: String) {
        viewModelScope.launch { settingsRepository.setImageHostAliyunRegion(region) }
    }

    fun setImageHostAliyunDomain(domain: String) {
        viewModelScope.launch { settingsRepository.setImageHostAliyunDomain(domain) }
    }

    fun setImageHostTencentSecretId(id: String) {
        viewModelScope.launch { settingsRepository.setImageHostTencentSecretId(id) }
    }

    fun setImageHostTencentSecretKey(key: String) {
        viewModelScope.launch { settingsRepository.setImageHostTencentSecretKey(key) }
    }

    fun setImageHostTencentBucket(bucket: String) {
        viewModelScope.launch { settingsRepository.setImageHostTencentBucket(bucket) }
    }

    fun setImageHostTencentRegion(region: String) {
        viewModelScope.launch { settingsRepository.setImageHostTencentRegion(region) }
    }

    fun setImageHostTencentDomain(domain: String) {
        viewModelScope.launch { settingsRepository.setImageHostTencentDomain(domain) }
    }

    fun setImageHostCustomUrl(url: String) {
        viewModelScope.launch { settingsRepository.setImageHostCustomUrl(url) }
    }

    fun setImageHostCustomMethod(method: String) {
        viewModelScope.launch { settingsRepository.setImageHostCustomMethod(method) }
    }

    fun setImageHostCustomHeaders(headers: String) {
        settingsRepository.setImageHostCustomHeaders(headers)
    }

    fun setImageHostCustomResponse(rule: String) {
        viewModelScope.launch { settingsRepository.setImageHostCustomResponse(rule) }
    }

    fun setImageCompress(enabled: Boolean) {
        Timber.tag("Settings").d("图片压缩：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setImageCompress(enabled) }
    }

    fun setImageCompressQuality(quality: Int) {
        viewModelScope.launch { settingsRepository.setImageCompressQuality(quality) }
    }

    fun setImageResize(enabled: Boolean) {
        Timber.tag("Settings").d("图片缩放：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setImageResize(enabled) }
    }

    fun setImageWatermark(enabled: Boolean) {
        Timber.tag("Settings").d("图片水印：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setImageWatermark(enabled) }
    }

    fun setImageWatermarkText(text: String) {
        viewModelScope.launch { settingsRepository.setImageWatermarkText(text) }
    }

    fun setImageAutoRename(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setImageAutoRename(enabled) }
    }

    fun setImageRenameRule(rule: String) {
        viewModelScope.launch { settingsRepository.setImageRenameRule(rule) }
    }

    fun setHardwareAccel(enabled: Boolean) {
        Timber.tag("Settings").d("硬件加速：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setHardwareAccel(enabled) }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDebugMode(enabled)
            Timber.tag("Settings").d("调试模式：${if (enabled) "开启" else "关闭"}")
        }
    }

    fun setLogLevel(level: String) {
        viewModelScope.launch {
            settingsRepository.setLogLevel(level)
            Timber.tag("Settings").d("日志级别切换为：$level")
        }
    }

    fun setDebugLogOutput(enabled: Boolean) {
        Timber.tag("Settings").d("调试日志输出：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setDebugLogOutput(enabled) }
    }

    fun setBackgroundImageUri(uri: String?) {
        viewModelScope.launch {
            // 更换或移除背景图片时，删除旧的持久化文件
            val oldUri = backgroundImageUri.value
            if (oldUri != null && oldUri != uri) {
                deletePersistedBackgroundFile(oldUri)
            }
            val persistedUri = uri?.let { persistBackgroundImage(it) }
            settingsRepository.setBackgroundImageUri(persistedUri)
        }
    }

    fun setBackgroundBlur(enabled: Boolean) {
        Timber.tag("Settings").d("背景模糊：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setBackgroundBlur(enabled) }
    }

    fun setBackgroundBlurIntensity(intensity: Int) {
        viewModelScope.launch { settingsRepository.setBackgroundBlurIntensity(intensity) }
    }

    fun setBackgroundBrightness(brightness: Int) {
        viewModelScope.launch { settingsRepository.setBackgroundBrightness(brightness) }
    }

    fun setEditorBackgroundImageUri(uri: String?) {
        viewModelScope.launch {
            // 更换或移除背景图片时，删除旧的持久化文件
            val oldUri = editorBackgroundImageUri.value
            if (oldUri != null && oldUri != uri) {
                deletePersistedBackgroundFile(oldUri)
            }
            val persistedUri = uri?.let { persistBackgroundImage(it, "editor_background" ) }
            settingsRepository.setEditorBackgroundImageUri(persistedUri)
        }
    }

    fun setEditorBackgroundBlur(enabled: Boolean) {
        Timber.tag("Settings").d("编辑器背景模糊：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setEditorBackgroundBlur(enabled) }
    }

    fun setEditorBackgroundBlurIntensity(intensity: Int) {
        viewModelScope.launch { settingsRepository.setEditorBackgroundBlurIntensity(intensity) }
    }

    fun setEditorBackgroundBrightness(brightness: Int) {
        viewModelScope.launch { settingsRepository.setEditorBackgroundBrightness(brightness) }
    }

    fun setCardBgColor(color: Int) {
        viewModelScope.launch { settingsRepository.setCardBgColor(color) }
    }

    fun setNavBarColor(color: Int) {
        viewModelScope.launch { settingsRepository.setNavBarColor(color) }
    }

    fun setSplashAnimationEnabled(enabled: Boolean) {
        Timber.tag("Settings").d("开场动画：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setSplashAnimationEnabled(enabled) }
    }

    fun setWorkspacePath(path: String) {
        viewModelScope.launch {
            settingsRepository.setWorkspacePath(path)
            Timber.tag("Settings").i("设置工作区路径：$path")
        }
    }

    /**
     * 将背景图片从缓存目录复制到持久化存储目录（files/backgrounds），
     * 防止清除缓存时丢失背景图片。
     * @param uri 原始 URI（可能是 content:// 或 file:// 指向缓存目录）
     * @param prefix 文件名前缀，用于区分不同背景
     * @return 持久化后的文件 URI，如果复制失败则返回原始 URI
     */
    private suspend fun persistBackgroundImage(uri: String, prefix: String = "app_background"): String {
        return withContext(Dispatchers.IO) {
            try {
                // 如果已经是持久化目录中的文件，直接返回
                val backgroundsDir = File(application.filesDir, "backgrounds")
                if (uri.startsWith("file://") && uri.contains(backgroundsDir.absolutePath)) {
                    return@withContext uri
                }
                // 如果是已经持久化的 file:// URI（非缓存），直接返回
                if (uri.startsWith("file://") && !uri.contains(application.cacheDir.absolutePath)) {
                    return@withContext uri
                }

                backgroundsDir.mkdirs()
                val destFile = File(backgroundsDir, "${prefix}_${System.currentTimeMillis()}.png")

                if (uri.startsWith("content://")) {
                    application.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (uri.startsWith("file://")) {
                    val sourceFile = File(android.net.Uri.parse(uri).path ?: return@withContext uri)
                    if (sourceFile.exists()) {
                        sourceFile.copyTo(destFile, overwrite = true)
                    } else {
                        return@withContext uri
                    }
                } else {
                    return@withContext uri
                }

                if (destFile.exists()) {
                    "file://${destFile.absolutePath}"
                } else {
                    uri
                }
            } catch (e: Exception) {
                Timber.tag("Settings").e(e, "持久化背景图片失败")
                uri
            }
        }
    }

    /**
     * 删除持久化的背景图片文件
     */
    private fun deletePersistedBackgroundFile(uri: String) {
        if (uri.startsWith("file://")) {
            val backgroundsDir = File(application.filesDir, "backgrounds").absolutePath
            if (uri.contains(backgroundsDir)) {
                val file = File(android.net.Uri.parse(uri).path ?: return)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            try {
                val sizeBefore = calculateCacheSize()
                withContext(Dispatchers.IO) {
                    deleteDirContents(application.cacheDir)
                    application.externalCacheDir?.let { deleteDirContents(it) }
                }
                _clearCacheEvent.emit(ClearCacheEvent.Success(sizeBefore))
                Timber.tag("Settings").i("清除缓存完成，释放：$sizeBefore")
                _cacheSize.value = calculateCacheSize()
            } catch (e: Exception) {
                _clearCacheEvent.emit(ClearCacheEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Settings").e(e, "清除缓存失败")
            } finally {
                _isClearingCache.value = false
            }
        }
    }

    fun clearVersionHistory() {
        viewModelScope.launch {
            _isClearingVersions.value = true
            try {
                val count = versionRepository.deleteAll()
                if (count > 0) {
                    runCatching { versionRepository.incrementalVacuum() }
                }
                _snapshotCount.value = 0
                _clearVersionEvent.emit(ClearVersionEvent.Success(count))
                Timber.tag("Settings").i("清除版本历史：$count 个快照")
            } catch (e: Exception) {
                _clearVersionEvent.emit(ClearVersionEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Settings").e(e, "清除版本历史失败")
            } finally {
                _isClearingVersions.value = false
            }
        }
    }

    fun restoreWorkspace() {
        viewModelScope.launch {
            _isRestoringWorkspace.value = true
            try {
                val defaultDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "Mdcito/Workspace"
                )
                if (!defaultDir.exists()) {
                    defaultDir.mkdirs()
                }
                settingsRepository.setWorkspacePath(defaultDir.absolutePath)
                _workspaceEvent.emit(WorkspaceEvent.Restored)
                Timber.tag("Settings").i("恢复默认工作区路径")
            } catch (e: Exception) {
                _workspaceEvent.emit(WorkspaceEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Settings").e(e, "恢复默认工作区路径失败")
            } finally {
                _isRestoringWorkspace.value = false
            }
        }
    }

    /**
     * 获取默认工作区路径（Documents/Mdcito/Workspace）的绝对路径。
     * 用于 UI 显示和判断当前路径是否为默认路径。
     */
    fun getDefaultWorkspacePath(): String {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Mdcito/Workspace"
        ).absolutePath
    }

    /**
     * 判断当前工作区路径是否为默认路径。
     */
    fun isDefaultWorkspace(): Boolean {
        val current = workspacePath.value ?: return false
        return current == getDefaultWorkspacePath()
    }

    // ── 缓存大小计算辅助方法 ──

    /**
     * 重置所有设置项为默认值。
     * 逐一将每个设置项写回其默认值，DataStore 的 Flow 会自动通知 UI 更新。
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 清除持久化的背景图片文件
                    deletePersistedBackgroundFile(backgroundImageUri.value ?: "")
                    deletePersistedBackgroundFile(editorBackgroundImageUri.value ?: "")
                    settingsRepository.resetAllToDefaults()
                }
                _resetSettingsEvent.emit(ResetSettingsEvent.Success)
                Timber.tag("Settings").i("重置所有设置为默认值")
            } catch (e: Exception) {
                _resetSettingsEvent.emit(ResetSettingsEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Settings").e(e, "重置设置失败")
            }
        }
    }

    /**
     * 清除所有用户数据，包括：
     * - 用户文件存储（数据库中的文件记录）
     * - 应用缓存数据
     * - 系统快照文件（版本历史）
     * - 个性化设置信息（DataStore）
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. 清除数据库所有表
                    database.clearAllTables()
                    // 2. 清除缓存
                    deleteDirContents(application.cacheDir)
                    application.externalCacheDir?.let { deleteDirContents(it) }
                    // 3. 清除持久化的背景图片文件
                    deletePersistedBackgroundFile(backgroundImageUri.value ?: "")
                    deletePersistedBackgroundFile(editorBackgroundImageUri.value ?: "")
                    // 4. 重置所有设置
                    settingsRepository.resetAllToDefaults()
                }
                _clearAllDataEvent.emit(ClearAllDataEvent.Success)
                Timber.tag("Settings").i("清除所有用户数据")
            } catch (e: Exception) {
                _clearAllDataEvent.emit(ClearAllDataEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Settings").e(e, "清除所有数据失败")
            }
        }
    }

    private fun calculateCacheSize(): String {
        var size = getFolderSize(application.cacheDir)
        application.externalCacheDir?.let { size += getFolderSize(it) }
        return formatFileSize(size)
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                size += if (child.isDirectory) getFolderSize(child) else child.length()
            }
        } else {
            size = file.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    child.deleteRecursively()
                } else {
                    child.delete()
                }
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val formatter = DecimalFormat("0.##")
        return when {
            size <= 0 -> "0 B"
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${formatter.format(size.toDouble() / 1024)} KB"
            size < 1024 * 1024 * 1024 -> "${formatter.format(size.toDouble() / (1024 * 1024))} MB"
            else -> "${formatter.format(size.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}
