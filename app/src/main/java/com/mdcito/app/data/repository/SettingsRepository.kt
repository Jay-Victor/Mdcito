package com.mdcito.app.data.repository

import com.mdcito.app.data.datastore.SecureSettingsDataStore
import com.mdcito.app.data.datastore.SettingsDataStore
import com.mdcito.app.data.model.ImageHostProfile
import com.mdcito.app.data.model.ImageProcessingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val secureSettingsDataStore: SecureSettingsDataStore,
) {

    val themeMode: Flow<String> = settingsDataStore.themeMode
    val themeColorIndex: Flow<Int> = settingsDataStore.themeColorIndex
    val lightScheme: Flow<String> = settingsDataStore.lightScheme
    val darkScheme: Flow<String> = settingsDataStore.darkScheme
    val cardStyle: Flow<String> = settingsDataStore.cardStyle
    val cardGlassIntensity: Flow<Int> = settingsDataStore.cardGlassIntensity
    val cardTransparency: Flow<Int> = settingsDataStore.cardTransparency
    val navBarTransparency: Flow<Int> = settingsDataStore.navBarTransparency
    val fontSize: Flow<Int> = settingsDataStore.fontSize
    val reduceMotion: Flow<Boolean> = settingsDataStore.reduceMotion
    val autoSave: Flow<Boolean> = settingsDataStore.autoSave
    val autoSaveInterval: Flow<Int> = settingsDataStore.autoSaveInterval
    val autoSaveIntervalUnit: Flow<String> = settingsDataStore.autoSaveIntervalUnit
    val showLineNumbers: Flow<Boolean> = settingsDataStore.showLineNumbers
    val highlightCurrentLine: Flow<Boolean> = settingsDataStore.highlightCurrentLine
    val autoIndent: Flow<Boolean> = settingsDataStore.autoIndent
    val bracketMatching: Flow<Boolean> = settingsDataStore.bracketMatching
    val spellCheck: Flow<Boolean> = settingsDataStore.spellCheck
    val codeFolding: Flow<Boolean> = settingsDataStore.codeFolding
    val tabSize: Flow<Int> = settingsDataStore.tabSize
    val lineWrap: Flow<String> = settingsDataStore.lineWrap
    val versionSnapshot: Flow<Boolean> = settingsDataStore.versionSnapshot
    val saveOnExitPrompt: Flow<Boolean> = settingsDataStore.saveOnExitPrompt
    val syncScroll: Flow<Boolean> = settingsDataStore.syncScroll
    val previewTheme: Flow<String> = settingsDataStore.previewTheme
    val workspacePath: Flow<String?> = settingsDataStore.workspacePath
    val dynamicColor: Flow<Boolean> = settingsDataStore.dynamicColor
    val imageHostEnabled: Flow<Boolean> = settingsDataStore.imageHostEnabled
    val imageHostProvider: Flow<String> = settingsDataStore.imageHostProvider
    val imageHostGithubRepo: Flow<String> = settingsDataStore.imageHostGithubRepo
    val imageHostGithubBranch: Flow<String> = settingsDataStore.imageHostGithubBranch
    private val _imageHostGithubToken = MutableStateFlow(secureSettingsDataStore.getImageHostGithubToken())
    val imageHostGithubToken: StateFlow<String> = _imageHostGithubToken.asStateFlow()
    val imageHostGithubDomain: Flow<String> = settingsDataStore.imageHostGithubDomain
    val imageHostQiniuAk: Flow<String> = settingsDataStore.imageHostQiniuAk
    private val _imageHostQiniuSk = MutableStateFlow(secureSettingsDataStore.getImageHostQiniuSk())
    val imageHostQiniuSk: StateFlow<String> = _imageHostQiniuSk.asStateFlow()
    val imageHostQiniuBucket: Flow<String> = settingsDataStore.imageHostQiniuBucket
    val imageHostQiniuDomain: Flow<String> = settingsDataStore.imageHostQiniuDomain
    private val _imageHostSmmsToken = MutableStateFlow(secureSettingsDataStore.getImageHostSmmsToken())
    val imageHostSmmsToken: StateFlow<String> = _imageHostSmmsToken.asStateFlow()
    val imageHostAliyunAkId: Flow<String> = settingsDataStore.imageHostAliyunAkId
    private val _imageHostAliyunAkSecret = MutableStateFlow(secureSettingsDataStore.getImageHostAliyunAkSecret())
    val imageHostAliyunAkSecret: StateFlow<String> = _imageHostAliyunAkSecret.asStateFlow()
    val imageHostAliyunBucket: Flow<String> = settingsDataStore.imageHostAliyunBucket
    val imageHostAliyunRegion: Flow<String> = settingsDataStore.imageHostAliyunRegion
    val imageHostAliyunDomain: Flow<String> = settingsDataStore.imageHostAliyunDomain
    private val _imageHostTencentSecretId = MutableStateFlow(secureSettingsDataStore.getImageHostTencentSecretId())
    val imageHostTencentSecretId: StateFlow<String> = _imageHostTencentSecretId.asStateFlow()
    private val _imageHostTencentSecretKey = MutableStateFlow(secureSettingsDataStore.getImageHostTencentSecretKey())
    val imageHostTencentSecretKey: StateFlow<String> = _imageHostTencentSecretKey.asStateFlow()
    val imageHostTencentBucket: Flow<String> = settingsDataStore.imageHostTencentBucket
    val imageHostTencentRegion: Flow<String> = settingsDataStore.imageHostTencentRegion
    val imageHostTencentDomain: Flow<String> = settingsDataStore.imageHostTencentDomain
    val imageHostCustomUrl: Flow<String> = settingsDataStore.imageHostCustomUrl
    val imageHostCustomMethod: Flow<String> = settingsDataStore.imageHostCustomMethod
    // 自定义图床请求头可能包含 Authorization 等敏感凭据，使用加密存储
    private val _imageHostCustomHeaders = MutableStateFlow(secureSettingsDataStore.getImageHostCustomHeaders())
    val imageHostCustomHeaders: StateFlow<String> = _imageHostCustomHeaders.asStateFlow()
    val imageHostCustomResponse: Flow<String> = settingsDataStore.imageHostCustomResponse
    val imageCompress: Flow<Boolean> = settingsDataStore.imageCompress
    val imageCompressQuality: Flow<Int> = settingsDataStore.imageCompressQuality
    val imageResize: Flow<Boolean> = settingsDataStore.imageResize
    val imageResizeMaxWidth: Flow<Int> = settingsDataStore.imageResizeMaxWidth
    val imageResizeMaxHeight: Flow<Int> = settingsDataStore.imageResizeMaxHeight
    val imageWatermark: Flow<Boolean> = settingsDataStore.imageWatermark
    val imageWatermarkText: Flow<String> = settingsDataStore.imageWatermarkText
    val imageAutoRename: Flow<Boolean> = settingsDataStore.imageAutoRename
    val imageRenameRule: Flow<String> = settingsDataStore.imageRenameRule
    val hardwareAccel: Flow<Boolean> = settingsDataStore.hardwareAccel
    val debugMode: Flow<Boolean> = settingsDataStore.debugMode
    val logLevel: Flow<String> = settingsDataStore.logLevel
    val debugLogOutput: Flow<Boolean> = settingsDataStore.debugLogOutput
    val buttonStyle: Flow<String> = settingsDataStore.buttonStyle
    val uiFont: Flow<String> = settingsDataStore.uiFont
    val uiFontSize: Flow<Int> = settingsDataStore.uiFontSize
    val editorFont: Flow<String> = settingsDataStore.editorFont
    val codeFont: Flow<String> = settingsDataStore.codeFont
    val backgroundImageUri: Flow<String?> = settingsDataStore.backgroundImageUri
    val backgroundBlur: Flow<Boolean> = settingsDataStore.backgroundBlur
    val backgroundBlurIntensity: Flow<Int> = settingsDataStore.backgroundBlurIntensity
    val backgroundBrightness: Flow<Int> = settingsDataStore.backgroundBrightness
    val editorBackgroundImageUri: Flow<String?> = settingsDataStore.editorBackgroundImageUri
    val editorBackgroundBlur: Flow<Boolean> = settingsDataStore.editorBackgroundBlur
    val editorBackgroundBlurIntensity: Flow<Int> = settingsDataStore.editorBackgroundBlurIntensity
    val editorBackgroundBrightness: Flow<Int> = settingsDataStore.editorBackgroundBrightness
    val cardBgColor: Flow<Int> = settingsDataStore.cardBgColor
    val navBarColor: Flow<Int> = settingsDataStore.navBarColor
    val onboardingCompleted: Flow<Boolean> = settingsDataStore.onboardingCompleted
    val lineHeight: Flow<Float> = settingsDataStore.lineHeight
    val letterSpacing: Flow<Float> = settingsDataStore.letterSpacing
    val paragraphSpacing: Flow<Float> = settingsDataStore.paragraphSpacing
    val markdownDialect: Flow<String> = settingsDataStore.markdownDialect
    val navBarStyle: Flow<String> = settingsDataStore.navBarStyle
    val navBarGlassIntensity: Flow<Int> = settingsDataStore.navBarGlassIntensity

    // ── 每种风格独立的调节参数 ──
    val cardMinimalTransparency: Flow<Int> = settingsDataStore.cardMinimalTransparency
    val cardFrostedIntensity: Flow<Int> = settingsDataStore.cardFrostedIntensity
    val cardLiquidIntensity: Flow<Int> = settingsDataStore.cardLiquidIntensity
    val navBarMinimalTransparency: Flow<Int> = settingsDataStore.navBarMinimalTransparency
    val navBarFrostedIntensity: Flow<Int> = settingsDataStore.navBarFrostedIntensity
    val navBarLiquidIntensity: Flow<Int> = settingsDataStore.navBarLiquidIntensity
    val highlightLineColor: Flow<String> = settingsDataStore.highlightLineColor
    val indentType: Flow<String> = settingsDataStore.indentType
    val indentSize: Flow<Int> = settingsDataStore.indentSize

    // ── 图床方案管理 ──
    val imageHostProfiles: Flow<List<ImageHostProfile>> = settingsDataStore.imageHostProfiles
    val imageHostActiveProfileId: Flow<String> = settingsDataStore.imageHostActiveProfileId
    val imageProcessingSettings: Flow<ImageProcessingSettings> = settingsDataStore.imageProcessingSettings

    suspend fun setThemeMode(mode: String) = settingsDataStore.setThemeMode(mode)
    suspend fun setThemeColorIndex(index: Int) = settingsDataStore.setThemeColorIndex(index)
    suspend fun setLightScheme(scheme: String) = settingsDataStore.setLightScheme(scheme)
    suspend fun setDarkScheme(scheme: String) = settingsDataStore.setDarkScheme(scheme)
    suspend fun setCardStyle(style: String) = settingsDataStore.setCardStyle(style)
    suspend fun setCardGlassIntensity(intensity: Int) = settingsDataStore.setCardGlassIntensity(intensity)
    suspend fun setCardTransparency(transparency: Int) = settingsDataStore.setCardTransparency(transparency)
    suspend fun setNavBarTransparency(transparency: Int) = settingsDataStore.setNavBarTransparency(transparency)
    suspend fun setFontSize(size: Int) = settingsDataStore.setFontSize(size)
    suspend fun setReduceMotion(reduce: Boolean) = settingsDataStore.setReduceMotion(reduce)
    suspend fun setAutoSave(enabled: Boolean) = settingsDataStore.setAutoSave(enabled)
    suspend fun setAutoSaveInterval(interval: Int) = settingsDataStore.setAutoSaveInterval(interval)
    suspend fun setAutoSaveIntervalUnit(unit: String) = settingsDataStore.setAutoSaveIntervalUnit(unit)
    suspend fun setShowLineNumbers(show: Boolean) = settingsDataStore.setShowLineNumbers(show)
    suspend fun setHighlightCurrentLine(highlight: Boolean) = settingsDataStore.setHighlightCurrentLine(highlight)
    suspend fun setAutoIndent(enabled: Boolean) = settingsDataStore.setAutoIndent(enabled)
    suspend fun setBracketMatching(enabled: Boolean) = settingsDataStore.setBracketMatching(enabled)
    suspend fun setSpellCheck(enabled: Boolean) = settingsDataStore.setSpellCheck(enabled)
    suspend fun setCodeFolding(enabled: Boolean) = settingsDataStore.setCodeFolding(enabled)
    suspend fun setTabSize(size: Int) = settingsDataStore.setTabSize(size)
    suspend fun setLineWrap(wrap: String) = settingsDataStore.setLineWrap(wrap)
    suspend fun setVersionSnapshot(enabled: Boolean) = settingsDataStore.setVersionSnapshot(enabled)
    suspend fun setSaveOnExitPrompt(prompt: Boolean) = settingsDataStore.setSaveOnExitPrompt(prompt)
    suspend fun setSyncScroll(enabled: Boolean) = settingsDataStore.setSyncScroll(enabled)
    suspend fun setPreviewTheme(theme: String) = settingsDataStore.setPreviewTheme(theme)
    suspend fun setWorkspacePath(path: String?) = settingsDataStore.setWorkspacePath(path)
    suspend fun setDynamicColor(enabled: Boolean) = settingsDataStore.setDynamicColor(enabled)
    suspend fun setImageHostEnabled(enabled: Boolean) = settingsDataStore.setImageHostEnabled(enabled)
    suspend fun setImageHostProvider(provider: String) = settingsDataStore.setImageHostProvider(provider)
    suspend fun setImageHostGithubRepo(repo: String) = settingsDataStore.setImageHostGithubRepo(repo)
    suspend fun setImageHostGithubBranch(branch: String) = settingsDataStore.setImageHostGithubBranch(branch)
    fun setImageHostGithubToken(token: String) {
        Timber.tag("SettingsRepo").i("图床GitHub Token已更新")
        secureSettingsDataStore.saveImageHostGithubToken(token)
        _imageHostGithubToken.value = token
    }
    suspend fun setImageHostGithubDomain(domain: String) = settingsDataStore.setImageHostGithubDomain(domain)
    suspend fun setImageHostQiniuAk(ak: String) = settingsDataStore.setImageHostQiniuAk(ak)
    fun setImageHostQiniuSk(sk: String) {
        Timber.tag("SettingsRepo").i("图床七牛SK已更新")
        secureSettingsDataStore.saveImageHostQiniuSk(sk)
        _imageHostQiniuSk.value = sk
    }
    suspend fun setImageHostQiniuBucket(bucket: String) = settingsDataStore.setImageHostQiniuBucket(bucket)
    suspend fun setImageHostQiniuDomain(domain: String) = settingsDataStore.setImageHostQiniuDomain(domain)
    fun setImageHostSmmsToken(token: String) {
        Timber.tag("SettingsRepo").i("图床SM.MS Token已更新")
        secureSettingsDataStore.saveImageHostSmmsToken(token)
        _imageHostSmmsToken.value = token
    }
    suspend fun setImageHostAliyunAkId(id: String) = settingsDataStore.setImageHostAliyunAkId(id)
    fun setImageHostAliyunAkSecret(secret: String) {
        Timber.tag("SettingsRepo").i("图床阿里云AK Secret已更新")
        secureSettingsDataStore.saveImageHostAliyunAkSecret(secret)
        _imageHostAliyunAkSecret.value = secret
    }
    suspend fun setImageHostAliyunBucket(bucket: String) = settingsDataStore.setImageHostAliyunBucket(bucket)
    suspend fun setImageHostAliyunRegion(region: String) = settingsDataStore.setImageHostAliyunRegion(region)
    suspend fun setImageHostAliyunDomain(domain: String) = settingsDataStore.setImageHostAliyunDomain(domain)
    fun setImageHostTencentSecretId(id: String) {
        Timber.tag("SettingsRepo").i("图床腾讯云SecretId已更新")
        secureSettingsDataStore.saveImageHostTencentSecretId(id)
        _imageHostTencentSecretId.value = id
    }
    fun setImageHostTencentSecretKey(key: String) {
        Timber.tag("SettingsRepo").i("图床腾讯云SecretKey已更新")
        secureSettingsDataStore.saveImageHostTencentSecretKey(key)
        _imageHostTencentSecretKey.value = key
    }
    suspend fun setImageHostTencentBucket(bucket: String) = settingsDataStore.setImageHostTencentBucket(bucket)
    suspend fun setImageHostTencentRegion(region: String) = settingsDataStore.setImageHostTencentRegion(region)
    suspend fun setImageHostTencentDomain(domain: String) = settingsDataStore.setImageHostTencentDomain(domain)
    suspend fun setImageHostCustomUrl(url: String) = settingsDataStore.setImageHostCustomUrl(url)
    suspend fun setImageHostCustomMethod(method: String) = settingsDataStore.setImageHostCustomMethod(method)
    fun setImageHostCustomHeaders(headers: String) {
        Timber.tag("SettingsRepo").i("图床自定义请求头已更新（加密存储）")
        secureSettingsDataStore.saveImageHostCustomHeaders(headers)
        _imageHostCustomHeaders.value = headers
    }
    suspend fun setImageHostCustomResponse(rule: String) = settingsDataStore.setImageHostCustomResponse(rule)
    suspend fun setImageCompress(enabled: Boolean) = settingsDataStore.setImageCompress(enabled)
    suspend fun setImageCompressQuality(quality: Int) = settingsDataStore.setImageCompressQuality(quality)
    suspend fun setImageResize(enabled: Boolean) = settingsDataStore.setImageResize(enabled)
    suspend fun setImageResizeMaxWidth(width: Int) = settingsDataStore.setImageResizeMaxWidth(width)
    suspend fun setImageResizeMaxHeight(height: Int) = settingsDataStore.setImageResizeMaxHeight(height)
    suspend fun setImageWatermark(enabled: Boolean) = settingsDataStore.setImageWatermark(enabled)
    suspend fun setImageWatermarkText(text: String) = settingsDataStore.setImageWatermarkText(text)
    suspend fun setImageAutoRename(enabled: Boolean) = settingsDataStore.setImageAutoRename(enabled)
    suspend fun setImageRenameRule(rule: String) = settingsDataStore.setImageRenameRule(rule)
    suspend fun setHardwareAccel(enabled: Boolean) = settingsDataStore.setHardwareAccel(enabled)
    suspend fun setDebugMode(enabled: Boolean) = settingsDataStore.setDebugMode(enabled)
    suspend fun setLogLevel(level: String) = settingsDataStore.setLogLevel(level)
    suspend fun setDebugLogOutput(enabled: Boolean) = settingsDataStore.setDebugLogOutput(enabled)
    suspend fun setButtonStyle(style: String) = settingsDataStore.setButtonStyle(style)
    suspend fun setUiFont(font: String) = settingsDataStore.setUiFont(font)
    suspend fun setUiFontSize(size: Int) = settingsDataStore.setUiFontSize(size)
    suspend fun setEditorFont(font: String) = settingsDataStore.setEditorFont(font)
    suspend fun setCodeFont(font: String) = settingsDataStore.setCodeFont(font)
    suspend fun setBackgroundImageUri(uri: String?) = settingsDataStore.setBackgroundImageUri(uri)
    suspend fun setBackgroundBlur(enabled: Boolean) = settingsDataStore.setBackgroundBlur(enabled)
    suspend fun setBackgroundBlurIntensity(intensity: Int) = settingsDataStore.setBackgroundBlurIntensity(intensity)
    suspend fun setBackgroundBrightness(brightness: Int) = settingsDataStore.setBackgroundBrightness(brightness)
    suspend fun setEditorBackgroundImageUri(uri: String?) = settingsDataStore.setEditorBackgroundImageUri(uri)
    suspend fun setEditorBackgroundBlur(enabled: Boolean) = settingsDataStore.setEditorBackgroundBlur(enabled)
    suspend fun setEditorBackgroundBlurIntensity(intensity: Int) = settingsDataStore.setEditorBackgroundBlurIntensity(intensity)
    suspend fun setEditorBackgroundBrightness(brightness: Int) = settingsDataStore.setEditorBackgroundBrightness(brightness)
    suspend fun setCardBgColor(color: Int) = settingsDataStore.setCardBgColor(color)
    suspend fun setNavBarColor(color: Int) = settingsDataStore.setNavBarColor(color)
    suspend fun setOnboardingCompleted(completed: Boolean) = settingsDataStore.setOnboardingCompleted(completed)
    suspend fun setLineHeight(height: Float) = settingsDataStore.setLineHeight(height)
    suspend fun setLetterSpacing(spacing: Float) = settingsDataStore.setLetterSpacing(spacing)
    suspend fun setMarkdownDialect(dialect: String) = settingsDataStore.setMarkdownDialect(dialect)
    suspend fun setParagraphSpacing(spacing: Float) = settingsDataStore.setParagraphSpacing(spacing)
    suspend fun setNavBarStyle(style: String) = settingsDataStore.setNavBarStyle(style)
    suspend fun setNavBarGlassIntensity(intensity: Int) = settingsDataStore.setNavBarGlassIntensity(intensity)

    // ── 每种风格独立的调节参数 setter ──
    suspend fun setCardMinimalTransparency(value: Int) = settingsDataStore.setCardMinimalTransparency(value)
    suspend fun setCardFrostedIntensity(value: Int) = settingsDataStore.setCardFrostedIntensity(value)
    suspend fun setCardLiquidIntensity(value: Int) = settingsDataStore.setCardLiquidIntensity(value)
    suspend fun setNavBarMinimalTransparency(value: Int) = settingsDataStore.setNavBarMinimalTransparency(value)
    suspend fun setNavBarFrostedIntensity(value: Int) = settingsDataStore.setNavBarFrostedIntensity(value)
    suspend fun setNavBarLiquidIntensity(value: Int) = settingsDataStore.setNavBarLiquidIntensity(value)
    suspend fun setHighlightLineColor(color: String) = settingsDataStore.setHighlightLineColor(color)
    suspend fun setIndentType(type: String) = settingsDataStore.setIndentType(type)
    suspend fun setIndentSize(size: Int) = settingsDataStore.setIndentSize(size)

    // ── 图床方案管理 ──
    suspend fun setImageHostProfiles(profiles: List<ImageHostProfile>) = settingsDataStore.setImageHostProfiles(profiles)
    suspend fun setImageHostActiveProfileId(id: String) = settingsDataStore.setImageHostActiveProfileId(id)
    suspend fun setImageProcessingSettings(settings: ImageProcessingSettings) = settingsDataStore.setImageProcessingSettings(settings)

    // ── 云同步设置 ──
    val cloudSyncServiceType: Flow<String> = settingsDataStore.cloudSyncServiceType
    val cloudSyncServerUrl: Flow<String> = settingsDataStore.cloudSyncServerUrl
    val cloudSyncPort: Flow<Int> = settingsDataStore.cloudSyncPort
    val cloudSyncUsername: Flow<String> = settingsDataStore.cloudSyncUsername
    val cloudSyncRemotePath: Flow<String> = settingsDataStore.cloudSyncRemotePath
    val cloudSyncMode: Flow<String> = settingsDataStore.cloudSyncMode
    val cloudSyncAutoSyncInterval: Flow<Int> = settingsDataStore.cloudSyncAutoSyncInterval
    val cloudSyncWifiOnly: Flow<Boolean> = settingsDataStore.cloudSyncWifiOnly
    val cloudSyncChargingOnly: Flow<Boolean> = settingsDataStore.cloudSyncChargingOnly
    val cloudSyncFileFilterRules: Flow<String> = settingsDataStore.cloudSyncFileFilterRules
    val cloudSyncLastSyncTime: Flow<Long> = settingsDataStore.cloudSyncLastSyncTime
    val cloudSyncLastSyncStatus: Flow<String> = settingsDataStore.cloudSyncLastSyncStatus
    val cloudSyncEnabled: Flow<Boolean> = settingsDataStore.cloudSyncEnabled
    val cloudSyncTokenExpiryTime: Flow<Long> = settingsDataStore.cloudSyncTokenExpiryTime
    val cloudSyncConflictResolution: Flow<String> = settingsDataStore.cloudSyncConflictResolution
    val cloudSyncUseCustomAuthEndpoint: Flow<Boolean> = settingsDataStore.cloudSyncUseCustomAuthEndpoint
    val cloudSyncCustomAuthEndpoint: Flow<String> = settingsDataStore.cloudSyncCustomAuthEndpoint
    val cloudSyncCustomTokenEndpoint: Flow<String> = settingsDataStore.cloudSyncCustomTokenEndpoint
    val cloudSyncDeletions: Flow<Boolean> = settingsDataStore.cloudSyncDeletions
    val cloudSyncLastSyncedFiles: Flow<String> = settingsDataStore.cloudSyncLastSyncedFiles
    val cloudSyncHistory: Flow<String> = settingsDataStore.cloudSyncHistory

    // ── 更新管理设置 ──
    val autoCheckUpdate: Flow<Boolean> = settingsDataStore.autoCheckUpdate
    val updateSource: Flow<String> = settingsDataStore.updateSource

    // ── 过渡动画设置 ──
    val splashAnimationEnabled: Flow<Boolean> = settingsDataStore.splashAnimationEnabled

    // ── 云同步敏感凭据（加密存储） ──
    private val _cloudSyncPassword = MutableStateFlow(secureSettingsDataStore.getCloudSyncPassword())
    val cloudSyncPassword: StateFlow<String> = _cloudSyncPassword.asStateFlow()

    private val _cloudSyncAccessToken = MutableStateFlow(secureSettingsDataStore.getCloudSyncAccessToken())
    val cloudSyncAccessToken: StateFlow<String> = _cloudSyncAccessToken.asStateFlow()

    private val _cloudSyncRefreshToken = MutableStateFlow(secureSettingsDataStore.getCloudSyncRefreshToken())
    val cloudSyncRefreshToken: StateFlow<String> = _cloudSyncRefreshToken.asStateFlow()

    private val _cloudSyncOAuthClientId = MutableStateFlow(secureSettingsDataStore.getCloudSyncOAuthClientId())
    val cloudSyncOAuthClientId: StateFlow<String> = _cloudSyncOAuthClientId.asStateFlow()

    suspend fun setCloudSyncServiceType(type: String) = settingsDataStore.setCloudSyncServiceType(type)
    suspend fun setCloudSyncServerUrl(url: String) = settingsDataStore.setCloudSyncServerUrl(url)
    suspend fun setCloudSyncPort(port: Int) = settingsDataStore.setCloudSyncPort(port)
    suspend fun setCloudSyncUsername(username: String) = settingsDataStore.setCloudSyncUsername(username)
    suspend fun setCloudSyncRemotePath(path: String) = settingsDataStore.setCloudSyncRemotePath(path)
    suspend fun setCloudSyncMode(mode: String) = settingsDataStore.setCloudSyncMode(mode)
    suspend fun setCloudSyncAutoSyncInterval(interval: Int) = settingsDataStore.setCloudSyncAutoSyncInterval(interval)
    suspend fun setCloudSyncWifiOnly(wifiOnly: Boolean) = settingsDataStore.setCloudSyncWifiOnly(wifiOnly)
    suspend fun setCloudSyncChargingOnly(chargingOnly: Boolean) = settingsDataStore.setCloudSyncChargingOnly(chargingOnly)
    suspend fun setCloudSyncFileFilterRules(rules: String) = settingsDataStore.setCloudSyncFileFilterRules(rules)
    suspend fun setCloudSyncLastSyncTime(time: Long) = settingsDataStore.setCloudSyncLastSyncTime(time)
    suspend fun setCloudSyncLastSyncStatus(status: String) = settingsDataStore.setCloudSyncLastSyncStatus(status)
    suspend fun setCloudSyncEnabled(enabled: Boolean) = settingsDataStore.setCloudSyncEnabled(enabled)
    suspend fun setCloudSyncTokenExpiryTime(time: Long) = settingsDataStore.setCloudSyncTokenExpiryTime(time)
    suspend fun setCloudSyncConflictResolution(resolution: String) = settingsDataStore.setCloudSyncConflictResolution(resolution)
    suspend fun setCloudSyncUseCustomAuthEndpoint(use: Boolean) = settingsDataStore.setCloudSyncUseCustomAuthEndpoint(use)
    suspend fun setCloudSyncCustomAuthEndpoint(endpoint: String) = settingsDataStore.setCloudSyncCustomAuthEndpoint(endpoint)
    suspend fun setCloudSyncCustomTokenEndpoint(endpoint: String) = settingsDataStore.setCloudSyncCustomTokenEndpoint(endpoint)
    suspend fun setCloudSyncDeletions(enabled: Boolean) = settingsDataStore.setCloudSyncDeletions(enabled)
    suspend fun setCloudSyncLastSyncedFiles(files: String) = settingsDataStore.setCloudSyncLastSyncedFiles(files)
    suspend fun setCloudSyncHistory(history: String) = settingsDataStore.setCloudSyncHistory(history)

    // ── 更新管理设置 setter ──
    suspend fun setAutoCheckUpdate(enabled: Boolean) = settingsDataStore.setAutoCheckUpdate(enabled)
    suspend fun setUpdateSource(source: String) = settingsDataStore.setUpdateSource(source)

    // ── 过渡动画设置 setter ──
    suspend fun setSplashAnimationEnabled(enabled: Boolean) = settingsDataStore.setSplashAnimationEnabled(enabled)

    // ── 云同步敏感凭据 setter（加密存储） ──
    fun setCloudSyncPassword(password: String) {
        Timber.tag("SettingsRepo").i("云同步密码已更新")
        secureSettingsDataStore.saveCloudSyncPassword(password)
        _cloudSyncPassword.value = password
    }

    fun setCloudSyncAccessToken(token: String) {
        Timber.tag("SettingsRepo").i("云同步AccessToken已更新")
        secureSettingsDataStore.saveCloudSyncAccessToken(token)
        _cloudSyncAccessToken.value = token
    }

    fun setCloudSyncRefreshToken(token: String) {
        Timber.tag("SettingsRepo").i("云同步RefreshToken已更新")
        secureSettingsDataStore.saveCloudSyncRefreshToken(token)
        _cloudSyncRefreshToken.value = token
    }

    fun setCloudSyncOAuthClientId(clientId: String) {
        Timber.tag("SettingsRepo").i("云同步OAuth ClientId已更新")
        secureSettingsDataStore.saveCloudSyncOAuthClientId(clientId)
        _cloudSyncOAuthClientId.value = clientId
    }

    fun saveCloudSyncPkceCodeVerifier(verifier: String) {
        secureSettingsDataStore.saveCloudSyncPkceCodeVerifier(verifier)
    }

    fun getCloudSyncPkceCodeVerifier(): String {
        return secureSettingsDataStore.getCloudSyncPkceCodeVerifier()
    }

    fun saveCloudSyncOAuthState(state: String) {
        secureSettingsDataStore.saveCloudSyncOAuthState(state)
    }

    fun getCloudSyncOAuthState(): String {
        return secureSettingsDataStore.getCloudSyncOAuthState()
    }

    suspend fun resetAllToDefaults() {
        Timber.tag("SettingsRepo").w("所有设置已重置为默认值")
        settingsDataStore.resetAllToDefaults()
    }

    /**
     * 清除内存中的敏感凭据缓存，用于应用进入后台时保护数据安全。
     * 下次读取时会从加密存储重新加载。
     */
    fun clearSensitiveCache() {
        Timber.tag("SettingsRepo").i("敏感凭据缓存已清除")
        _cloudSyncPassword.value = ""
        _cloudSyncAccessToken.value = ""
        _cloudSyncRefreshToken.value = ""
        _cloudSyncOAuthClientId.value = ""
        _imageHostGithubToken.value = ""
        _imageHostQiniuSk.value = ""
        _imageHostSmmsToken.value = ""
        _imageHostAliyunAkSecret.value = ""
        _imageHostTencentSecretId.value = ""
        _imageHostTencentSecretKey.value = ""
    }

    /**
     * 从加密存储重新加载敏感凭据到内存缓存，用于应用回到前台时。
     */
    fun reloadSensitiveCache() {
        Timber.tag("SettingsRepo").i("敏感凭据缓存已重新加载")
        _cloudSyncPassword.value = secureSettingsDataStore.getCloudSyncPassword()
        _cloudSyncAccessToken.value = secureSettingsDataStore.getCloudSyncAccessToken()
        _cloudSyncRefreshToken.value = secureSettingsDataStore.getCloudSyncRefreshToken()
        _cloudSyncOAuthClientId.value = secureSettingsDataStore.getCloudSyncOAuthClientId()
        _imageHostGithubToken.value = secureSettingsDataStore.getImageHostGithubToken()
        _imageHostQiniuSk.value = secureSettingsDataStore.getImageHostQiniuSk()
        _imageHostSmmsToken.value = secureSettingsDataStore.getImageHostSmmsToken()
        _imageHostAliyunAkSecret.value = secureSettingsDataStore.getImageHostAliyunAkSecret()
        _imageHostTencentSecretId.value = secureSettingsDataStore.getImageHostTencentSecretId()
        _imageHostTencentSecretKey.value = secureSettingsDataStore.getImageHostTencentSecretKey()
    }
}
