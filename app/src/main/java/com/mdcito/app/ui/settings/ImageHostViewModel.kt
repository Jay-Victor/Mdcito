package com.mdcito.app.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import com.mdcito.app.R
import com.mdcito.app.data.datastore.SecureSettingsDataStore
import com.mdcito.app.data.image.ImageUploadService
import com.mdcito.app.data.model.ImageHostProfile
import com.mdcito.app.data.model.ImageProcessingSettings
import com.mdcito.app.data.model.ProviderFieldDefs
import com.mdcito.app.data.model.TestResult
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ImageHostViewModel @Inject constructor(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val secureSettingsDataStore: SecureSettingsDataStore,
    private val imageUploadService: ImageUploadService,
) : ViewModel() {

    // ── 图床总开关 ──
    val imageHostEnabled: StateFlow<Boolean> = settingsRepository.imageHostEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── 方案列表与当前激活方案 ──
    val profiles: StateFlow<List<ImageHostProfile>> = settingsRepository.imageHostProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfileId: StateFlow<String> = settingsRepository.imageHostActiveProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 当前激活的方案（由 profiles + activeProfileId 组合计算）
    val activeProfile: StateFlow<ImageHostProfile?> = combine(profiles, activeProfileId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── 图片处理设置 ──
    val processingSettings: StateFlow<ImageProcessingSettings> = settingsRepository.imageProcessingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImageProcessingSettings())

    // ── UI 状态 ──
    private val _isSwitching = MutableStateFlow(false)
    val isSwitching: StateFlow<Boolean> = _isSwitching.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult: StateFlow<TestResult?> = _testResult.asStateFlow()

    private val _saveResult = MutableStateFlow<TestResult?>(null)
    val saveResult: StateFlow<TestResult?> = _saveResult.asStateFlow()

    private val _deleteResult = MutableStateFlow<TestResult?>(null)
    val deleteResult: StateFlow<TestResult?> = _deleteResult.asStateFlow()

    // ── 临时编辑状态（用于在保存前暂存用户输入） ──
    private val _editingConfig = MutableStateFlow<Map<String, String>>(emptyMap())
    val editingConfig: StateFlow<Map<String, String>> = _editingConfig.asStateFlow()

    private val _editingProvider = MutableStateFlow("github")
    val editingProvider: StateFlow<String> = _editingProvider.asStateFlow()

    private val _editingName = MutableStateFlow("")
    val editingName: StateFlow<String> = _editingName.asStateFlow()

    init {
        viewModelScope.launch {
            val profileList = profiles.first()
            if (profileList.isEmpty()) {
                // 方案列表为空时自动创建默认方案
                createProfile()
                Timber.tag("ImageHost").i("自动创建默认图床方案")
            } else {
                Timber.tag("ImageHost").d("加载图床方案列表，共 ${profileList.size} 个")
            }
        }
    }

    // ── 图床开关 ──
    fun setImageHostEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // 关闭时先保存当前编辑中的配置，避免数据丢失
            if (!enabled) {
                saveCurrentEditingToProfile()
            }
            settingsRepository.setImageHostEnabled(enabled)
            Timber.tag("ImageHost").i("图床${if (enabled) "已启用" else "已禁用"}")
        }
    }

    // ── 方案管理 ──

    /**
     * 切换当前激活方案
     */
    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            _isSwitching.value = true
            try {
                // 先保存当前编辑中的配置
                saveCurrentEditingToProfile()
                settingsRepository.setImageHostActiveProfileId(profileId)
                Timber.tag("ImageHost").d("切换图床方案：$profileId")
            } finally {
                _isSwitching.value = false
            }
        }
    }

    /**
     * 新建方案
     */
    fun createProfile() {
        viewModelScope.launch {
            val currentProfiles = profiles.value
            val newIndex = currentProfiles.size + 1
            val newProfile = ImageHostProfile(
                id = UUID.randomUUID().toString(),
                name = application.getString(R.string.profile_default_name, newIndex),
                provider = "github",
                config = emptyMap(),
                isDefault = currentProfiles.isEmpty(),
            )
            val updatedProfiles = currentProfiles + newProfile
            settingsRepository.setImageHostProfiles(updatedProfiles)
            settingsRepository.setImageHostActiveProfileId(newProfile.id)
            loadProfileToEditing(newProfile)
            Timber.tag("ImageHost").i("新建图床方案：${newProfile.name}")
        }
    }

    /**
     * 重命名当前方案
     */
    fun renameProfile(newName: String) {
        if (newName.isBlank() || newName.length > 50) return
        viewModelScope.launch {
            val currentId = activeProfileId.value
            val updatedProfiles = profiles.value.map { profile ->
                if (profile.id == currentId) profile.copy(name = newName, updatedAt = System.currentTimeMillis())
                else profile
            }
            settingsRepository.setImageHostProfiles(updatedProfiles)
            _editingName.value = newName
        }
    }

    /**
     * 删除方案
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            try {
                // 清除加密存储中的敏感数据
                secureSettingsDataStore.deleteProfileSecrets(profileId)

                val currentProfiles = profiles.value
                val updatedProfiles = currentProfiles.filter { it.id != profileId }
                // 如果删除的是当前激活方案，切换到第一个
                if (profileId == activeProfileId.value && updatedProfiles.isNotEmpty()) {
                    settingsRepository.setImageHostActiveProfileId(updatedProfiles.first().id)
                    loadProfileToEditing(updatedProfiles.first())
                } else if (updatedProfiles.isEmpty()) {
                    settingsRepository.setImageHostActiveProfileId("")
                    _editingConfig.value = emptyMap()
                    _editingProvider.value = "github"
                    _editingName.value = ""
                }
                // 确保至少有一个默认方案
                if (updatedProfiles.isNotEmpty() && updatedProfiles.none { it.isDefault }) {
                    val fixed = updatedProfiles.toMutableList()
                    fixed[0] = fixed[0].copy(isDefault = true)
                    settingsRepository.setImageHostProfiles(fixed)
                } else {
                    settingsRepository.setImageHostProfiles(updatedProfiles)
                }
                _deleteResult.value = TestResult(success = true, message = application.getString(R.string.profile_deleted))
                Timber.tag("ImageHost").i("删除图床方案：$profileId")
            } catch (e: Exception) {
                _deleteResult.value = TestResult(success = false, message = e.message ?: application.getString(R.string.profile_delete_failed))
                Timber.tag("ImageHost").e(e, "删除图床方案失败：$profileId")
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    /**
     * 设为默认方案
     */
    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            val updatedProfiles = profiles.value.map { profile ->
                profile.copy(isDefault = profile.id == profileId)
            }
            settingsRepository.setImageHostProfiles(updatedProfiles)
        }
    }

    /**
     * 保存当前编辑中的配置到方案
     */
    fun saveCurrentProfile() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                saveCurrentEditingToProfile()
                val profileId = activeProfileId.value
                Timber.tag("ImageHost").i("保存图床方案配置：$profileId")
                _saveResult.value = TestResult(success = true, message = application.getString(R.string.save_success))
            } catch (e: Exception) {
                _saveResult.value = TestResult(success = false, message = e.message ?: application.getString(R.string.save_failed))
                val profileId = activeProfileId.value
                Timber.tag("ImageHost").e(e, "保存图床方案失败：$profileId")
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 清除保存结果提示
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * 将当前编辑状态保存到 DataStore
     * 敏感字段（isSecret=true）保存到加密存储，非敏感字段保存到 DataStore
     */
    private suspend fun saveCurrentEditingToProfile() {
        val currentId = activeProfileId.value
        if (currentId.isBlank()) return

        val provider = _editingProvider.value
        val config = _editingConfig.value
        val fields = ProviderFieldDefs.getFields(provider)

        // 分离敏感字段和非敏感字段
        val secretFields = fields.filter { it.isSecret }
        val secretKeys = secretFields.map { it.key }.toSet()

        // 非敏感字段存入 profile config
        val nonSecretConfig = config.filterKeys { it !in secretKeys }
        // 敏感字段存入加密存储
        val secretConfig = config.filterKeys { it in secretKeys }
        secureSettingsDataStore.saveAllSecrets(currentId, secretConfig)

        val updatedProfiles = profiles.value.map { profile ->
            if (profile.id == currentId) {
                profile.copy(
                    provider = provider,
                    config = nonSecretConfig,
                    name = _editingName.value.ifBlank { profile.name },
                    updatedAt = System.currentTimeMillis(),
                )
            } else profile
        }
        settingsRepository.setImageHostProfiles(updatedProfiles)
    }

    /**
     * 加载方案到编辑状态
     * 从 DataStore 读取非敏感配置，从加密存储读取敏感配置，合并后加载
     */
    fun loadProfileToEditing(profile: ImageHostProfile) {
        val provider = profile.provider
        val fields = ProviderFieldDefs.getFields(provider)
        val secretKeys = fields.filter { it.isSecret }.map { it.key }.toSet()

        // 从加密存储读取敏感字段
        val secrets = secureSettingsDataStore.getAllSecrets(profile.id, secretKeys.toList())

        // 合并非敏感配置和敏感配置
        val mergedConfig = profile.config.toMutableMap()
        mergedConfig.putAll(secrets)

        _editingConfig.value = mergedConfig
        _editingProvider.value = provider
        _editingName.value = profile.name
        _testResult.value = profile.lastTestResult
    }

    /**
     * 更新编辑中的配置字段
     */
    fun updateConfigField(key: String, value: String) {
        _editingConfig.value = _editingConfig.value.toMutableMap().apply { this[key] = value }
    }

    /**
     * 更新编辑中的供应商类型
     */
    fun updateProvider(provider: String) {
        _editingProvider.value = provider
        // 切换供应商时清空配置
        _editingConfig.value = emptyMap()
    }

    /**
     * 更新编辑中的方案名称
     */
    fun updateEditingName(name: String) {
        _editingName.value = name
    }

    /**
     * 测试连接
     */
    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            try {
                val provider = _editingProvider.value
                val config = _editingConfig.value
                val result = performTest(provider, config)
                _testResult.value = result
                Timber.tag("ImageHost").i("图床连接测试：${if (result.success) "成功" else "失败 - ${result.message}"}")
                // 更新方案的测试结果
                val currentId = activeProfileId.value
                if (currentId.isNotBlank()) {
                    val updatedProfiles = profiles.value.map { profile ->
                        if (profile.id == currentId) profile.copy(lastTestResult = result, updatedAt = System.currentTimeMillis())
                        else profile
                    }
                    settingsRepository.setImageHostProfiles(updatedProfiles)
                }
                // 5秒后自动清除测试结果
                delay(5000)
                _testResult.value = null
            } catch (e: Exception) {
                _testResult.value = TestResult(success = false, message = e.message ?: application.getString(R.string.test_failed))
                Timber.tag("ImageHost").e(e, "图床连接测试异常")
                // 5秒后自动清除错误结果
                delay(5000)
                _testResult.value = null
            } finally {
                _isTesting.value = false
            }
        }
    }

    /**
     * 执行连接测试（通过 ImageUploadService 发送真实 HTTP 请求）
     */
    private suspend fun performTest(provider: String, config: Map<String, String>): TestResult {
        return withContext(Dispatchers.IO) {
            // 先检查必填字段
            val fields = ProviderFieldDefs.getFields(provider)
            val missingFields = fields.filter { it.isRequired && config[it.key].isNullOrBlank() }
            if (missingFields.isNotEmpty()) {
                return@withContext TestResult(
                    success = false,
                    message = application.getString(R.string.upload_missing_required_fields, missingFields.joinToString { it.key })
                )
            }
            // 基本格式验证
            when (provider) {
                "github" -> {
                    val repo = config["repo"] ?: ""
                    if (!repo.contains("/")) {
                        return@withContext TestResult(success = false, message = application.getString(R.string.validation_repo_format))
                    }
                    val token = config["token"] ?: ""
                    if (token.length < 10) {
                        return@withContext TestResult(success = false, message = application.getString(R.string.validation_token_format))
                    }
                }
                "custom" -> {
                    val url = config["url"] ?: ""
                    if (!url.startsWith("http")) {
                        return@withContext TestResult(success = false, message = application.getString(R.string.validation_url_http))
                    }
                }
            }
            // 发送真实 HTTP 请求测试连接
            imageUploadService.testConnection(provider, config)
        }
    }

    // ── 图片处理设置 ──

    fun updateProcessingSettings(update: (ImageProcessingSettings) -> ImageProcessingSettings) {
        viewModelScope.launch {
            val current = processingSettings.value
            settingsRepository.setImageProcessingSettings(update(current))
        }
    }

    fun setCompressEnabled(enabled: Boolean) {
        updateProcessingSettings { it.copy(compressEnabled = enabled) }
    }

    fun setCompressQuality(quality: Int) {
        updateProcessingSettings { it.copy(compressQuality = quality.coerceIn(0, 100)) }
    }

    fun setResizeEnabled(enabled: Boolean) {
        updateProcessingSettings { it.copy(resizeEnabled = enabled) }
    }

    fun setResizePreset(preset: String) {
        updateProcessingSettings { settings ->
            val presetObj = com.mdcito.app.data.model.ResizePresets.PRESETS.find { it.key == preset }
            if (presetObj != null && preset != "custom") {
                settings.copy(resizePreset = preset, resizeMaxWidth = presetObj.width, resizeMaxHeight = presetObj.height)
            } else {
                settings.copy(resizePreset = "custom")
            }
        }
    }

    fun setResizeMaxWidth(width: Int) {
        updateProcessingSettings { it.copy(resizeMaxWidth = width, resizePreset = "custom") }
    }

    fun setResizeMaxHeight(height: Int) {
        updateProcessingSettings { it.copy(resizeMaxHeight = height, resizePreset = "custom") }
    }

    fun setWatermarkEnabled(enabled: Boolean) {
        updateProcessingSettings { it.copy(watermarkEnabled = enabled) }
    }

    fun setWatermarkText(text: String) {
        updateProcessingSettings { it.copy(watermarkText = text) }
    }

    fun setWatermarkPosition(position: String) {
        updateProcessingSettings { it.copy(watermarkPosition = position) }
    }

    fun setWatermarkOpacity(opacity: Int) {
        updateProcessingSettings { it.copy(watermarkOpacity = opacity.coerceIn(0, 100)) }
    }

    fun setAutoRenameEnabled(enabled: Boolean) {
        updateProcessingSettings { it.copy(autoRenameEnabled = enabled) }
    }

    fun setRenameRule(rule: String) {
        updateProcessingSettings { it.copy(renameRule = rule) }
    }

    /**
     * 当激活方案变化时，自动加载到编辑状态
     */
    fun onActiveProfileChanged(profile: ImageHostProfile?) {
        if (profile != null) {
            loadProfileToEditing(profile)
        }
    }
}
