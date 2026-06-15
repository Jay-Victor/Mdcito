package com.mdcito.app.data.datastore

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密存储敏感数据（如 API Token、Secret Key 等）
 * 使用 Android EncryptedSharedPreferences + MasterKey 进行 AES256 加密
 */
@Singleton
class SecureSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * 安全地创建 EncryptedSharedPreferences。
     * 如果加密存储不可用（如 Keystore 损坏），返回 null。
     * 调用方必须处理 null 返回值，禁止降级为明文存储。
     */
    private fun createEncryptedPrefs(fileName: String): SharedPreferences? {
        return try {
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences for $fileName. " +
                "Sensitive data will NOT be stored. Device Keystore may be unavailable.")
            null
        }
    }

    /**
     * 创建加密存储，若不可用则返回 no-op 实现（写入丢弃、读取返回空值）。
     * 这确保敏感凭据永远不会降级为明文存储。
     */
    private fun createSecurePrefsOrNoOp(fileName: String): SharedPreferences {
        val encrypted = createEncryptedPrefs(fileName)
        if (encrypted != null) return encrypted

        Timber.w("Encryption unavailable for $fileName - using no-op storage (data will NOT be persisted)")
        return NoOpSharedPreferences()
    }

    /**
     * No-op SharedPreferences 实现：所有写入操作被丢弃，所有读取返回默认值。
     * 用于加密存储不可用时，确保敏感数据不会降级为明文存储。
     */
    private class NoOpSharedPreferences : SharedPreferences {
        override fun getAll(): Map<String, *> = emptyMap<String, String>()
        override fun getString(key: String?, defValue: String?): String? = defValue
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long = defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
        override fun contains(key: String?): Boolean = false
        override fun edit(): SharedPreferences.Editor = NoOpEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class NoOpEditor : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = this
            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor = this
            override fun clear(): SharedPreferences.Editor = this
            override fun commit(): Boolean = false
            override fun apply() {}
        }
    }

    private val encryptedPrefs: SharedPreferences = createSecurePrefsOrNoOp("image_host_secrets")

    /**
     * 保存敏感配置字段
     * @param profileId 方案 ID
     * @param fieldKey 字段键名（如 "token", "ak", "sk" 等）
     * @param value 敏感值
     */
    fun saveSecret(profileId: String, fieldKey: String, value: String) {
        encryptedPrefs.edit()
            .putString("${profileId}_$fieldKey", value)
            .apply()
    }

    /**
     * 读取敏感配置字段
     * @param profileId 方案 ID
     * @param fieldKey 字段键名
     * @return 敏感值，不存在则返回空字符串
     */
    fun getSecret(profileId: String, fieldKey: String): String {
        return encryptedPrefs.getString("${profileId}_$fieldKey", "") ?: ""
    }

    /**
     * 删除某个方案的所有敏感数据
     * @param profileId 方案 ID
     */
    fun deleteProfileSecrets(profileId: String) {
        val editor = encryptedPrefs.edit()
        val prefix = "${profileId}_"
        encryptedPrefs.all.keys.forEach { key ->
            if (key.startsWith(prefix)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    /**
     * 保存方案的所有敏感配置
     * @param profileId 方案 ID
     * @param secrets 敏感字段键值对
     */
    fun saveAllSecrets(profileId: String, secrets: Map<String, String>) {
        val editor = encryptedPrefs.edit()
        // 先清除旧的
        val prefix = "${profileId}_"
        encryptedPrefs.all.keys.forEach { key ->
            if (key.startsWith(prefix)) {
                editor.remove(key)
            }
        }
        // 写入新的
        secrets.forEach { (key, value) ->
            editor.putString("${profileId}_$key", value)
        }
        editor.apply()
    }

    /**
     * 读取方案的所有敏感配置
     * @param profileId 方案 ID
     * @param fieldKeys 需要读取的字段键名列表
     * @return 敏感字段键值对
     */
    fun getAllSecrets(profileId: String, fieldKeys: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        fieldKeys.forEach { key ->
            val value = getSecret(profileId, key)
            if (value.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }

    /**
     * 清除所有敏感数据
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    // ── 云同步敏感凭据 ──

    private val cloudSyncEncryptedPrefs: SharedPreferences = createSecurePrefsOrNoOp("cloud_sync_secrets")

    /**
     * 检查加密存储是否可用
     */
    val isEncryptionAvailable: Boolean by lazy {
        try {
            // 尝试写入并读取一个测试值来验证加密存储是否真正可用
            val testKey = "__encryption_test__"
            cloudSyncEncryptedPrefs.edit().putString(testKey, "test").commit()
            val result = cloudSyncEncryptedPrefs.getString(testKey, null)
            cloudSyncEncryptedPrefs.edit().remove(testKey).apply()
            result != null
        } catch (_: Exception) {
            false
        }
    }

    private object CloudSyncKeys {
        const val PASSWORD = "cloud_sync_password"
        const val ACCESS_TOKEN = "cloud_sync_access_token"
        const val REFRESH_TOKEN = "cloud_sync_refresh_token"
        const val OAUTH_CLIENT_ID = "cloud_sync_oauth_client_id"
        const val PKCE_CODE_VERIFIER = "cloud_sync_pkce_code_verifier"
        const val OAUTH_STATE = "cloud_sync_oauth_state"
    }

    fun saveCloudSyncPassword(password: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.PASSWORD, password).commit()
    }

    fun getCloudSyncPassword(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.PASSWORD, "") ?: ""
    }

    fun saveCloudSyncAccessToken(token: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.ACCESS_TOKEN, token).commit()
    }

    fun getCloudSyncAccessToken(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.ACCESS_TOKEN, "") ?: ""
    }

    fun saveCloudSyncRefreshToken(token: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.REFRESH_TOKEN, token).commit()
    }

    fun getCloudSyncRefreshToken(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.REFRESH_TOKEN, "") ?: ""
    }

    fun saveCloudSyncOAuthClientId(clientId: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.OAUTH_CLIENT_ID, clientId).commit()
    }

    fun getCloudSyncOAuthClientId(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.OAUTH_CLIENT_ID, "") ?: ""
    }

    fun saveCloudSyncPkceCodeVerifier(verifier: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.PKCE_CODE_VERIFIER, verifier).commit()
    }

    fun getCloudSyncPkceCodeVerifier(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.PKCE_CODE_VERIFIER, "") ?: ""
    }

    fun saveCloudSyncOAuthState(state: String) {
        cloudSyncEncryptedPrefs.edit().putString(CloudSyncKeys.OAUTH_STATE, state).commit()
    }

    fun getCloudSyncOAuthState(): String {
        return cloudSyncEncryptedPrefs.getString(CloudSyncKeys.OAUTH_STATE, "") ?: ""
    }

    fun clearCloudSyncSecrets() {
        cloudSyncEncryptedPrefs.edit().clear().apply()
    }

    // ── 图床遗留敏感凭据 ──

    private object ImageHostLegacyKeys {
        const val GITHUB_TOKEN = "image_host_github_token"
        const val QINIU_SK = "image_host_qiniu_sk"
        const val SMMS_TOKEN = "image_host_smms_token"
        const val ALIYUN_AK_SECRET = "image_host_aliyun_ak_secret"
        const val TENCENT_SECRET_ID = "image_host_tencent_secret_id"
        const val TENCENT_SECRET_KEY = "image_host_tencent_secret_key"
    }

    fun saveImageHostGithubToken(token: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.GITHUB_TOKEN, token).commit()
    }
    fun getImageHostGithubToken(): String = encryptedPrefs.getString(ImageHostLegacyKeys.GITHUB_TOKEN, "") ?: ""

    fun saveImageHostQiniuSk(sk: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.QINIU_SK, sk).commit()
    }
    fun getImageHostQiniuSk(): String = encryptedPrefs.getString(ImageHostLegacyKeys.QINIU_SK, "") ?: ""

    fun saveImageHostSmmsToken(token: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.SMMS_TOKEN, token).commit()
    }
    fun getImageHostSmmsToken(): String = encryptedPrefs.getString(ImageHostLegacyKeys.SMMS_TOKEN, "") ?: ""

    fun saveImageHostAliyunAkSecret(secret: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.ALIYUN_AK_SECRET, secret).commit()
    }
    fun getImageHostAliyunAkSecret(): String = encryptedPrefs.getString(ImageHostLegacyKeys.ALIYUN_AK_SECRET, "") ?: ""

    fun saveImageHostTencentSecretId(id: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.TENCENT_SECRET_ID, id).commit()
    }
    fun getImageHostTencentSecretId(): String = encryptedPrefs.getString(ImageHostLegacyKeys.TENCENT_SECRET_ID, "") ?: ""

    fun saveImageHostTencentSecretKey(key: String) {
        encryptedPrefs.edit().putString(ImageHostLegacyKeys.TENCENT_SECRET_KEY, key).commit()
    }
    fun getImageHostTencentSecretKey(): String = encryptedPrefs.getString(ImageHostLegacyKeys.TENCENT_SECRET_KEY, "") ?: ""

    fun clearImageHostLegacySecrets() {
        encryptedPrefs.edit().apply {
            remove(ImageHostLegacyKeys.GITHUB_TOKEN)
            remove(ImageHostLegacyKeys.QINIU_SK)
            remove(ImageHostLegacyKeys.SMMS_TOKEN)
            remove(ImageHostLegacyKeys.ALIYUN_AK_SECRET)
            remove(ImageHostLegacyKeys.TENCENT_SECRET_ID)
            remove(ImageHostLegacyKeys.TENCENT_SECRET_KEY)
        }.apply()
    }
}
