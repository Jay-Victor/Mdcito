package com.mdcito.app.data.model

import com.mdcito.app.R
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 图床配置方案数据模型
 * 每个方案包含独立的供应商类型和对应配置参数
 */
data class ImageHostProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: String = "github",
    val config: Map<String, String> = emptyMap(),
    val isDefault: Boolean = false,
    val lastTestResult: TestResult? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * 连接测试结果
 */
data class TestResult(
    val success: Boolean,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 图片处理设置
 */
data class ImageProcessingSettings(
    val compressEnabled: Boolean = false,
    val compressQuality: Int = 80,
    val resizeEnabled: Boolean = false,
    val resizeMaxWidth: Int = 1920,
    val resizeMaxHeight: Int = 1080,
    val resizePreset: String = "custom",
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "",
    val watermarkPosition: String = "bottom_right",
    val watermarkOpacity: Int = 50,
    val autoRenameEnabled: Boolean = true,
    val renameRule: String = "{timestamp}",
)

/**
 * 命名规则变量定义
 */
object RenameVariables {
    const val TIMESTAMP = "{timestamp}"
    const val DATE = "{date}"
    const val RANDOM = "{random}"
    const val ORIGINAL = "{original}"
    const val UUID = "{uuid}"

    data class Variable(val key: String, val labelResId: Int, val example: String)

    val ALL = listOf(
        Variable(TIMESTAMP, R.string.variable_timestamp, "1718000000000"),
        Variable(DATE, R.string.variable_date, "20240610"),
        Variable(RANDOM, R.string.variable_random, "4271"),
        Variable(ORIGINAL, R.string.variable_original, "example"),
        Variable(UUID, R.string.variable_uuid, "a1b2c3d4"),
    )

    /**
     * 生成命名规则预览
     */
    fun preview(rule: String): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        var result = rule
        result = result.replace(TIMESTAMP, System.currentTimeMillis().toString())
        result = result.replace(DATE, sdf.format(Date()))
        result = result.replace(RANDOM, (1000..9999).random().toString())
        result = result.replace(ORIGINAL, "example")
        result = result.replace(UUID, java.util.UUID.randomUUID().toString().substring(0, 8))
        return "$result.png"
    }
}

/**
 * 缩放预设尺寸
 */
object ResizePresets {
    data class Preset(val key: String, val labelResId: Int, val width: Int, val height: Int)

    val PRESETS = listOf(
        Preset("original", R.string.resize_original, Int.MAX_VALUE, Int.MAX_VALUE),
        Preset("hd", R.string.resize_hd, 1280, 720),
        Preset("full_hd", R.string.resize_full_hd, 1920, 1080),
        Preset("2k", R.string.resize_2k, 2560, 1440),
        Preset("4k", R.string.resize_4k, 3840, 2160),
        Preset("custom", R.string.resize_custom, 1920, 1080),
    )
}

/**
 * 水印位置
 */
object WatermarkPositions {
    data class Position(val key: String, val labelResId: Int)

    val POSITIONS = listOf(
        Position("top_left", R.string.position_top_left),
        Position("top_center", R.string.position_top_center),
        Position("top_right", R.string.position_top_right),
        Position("center_left", R.string.position_center_left),
        Position("center", R.string.position_center),
        Position("center_right", R.string.position_center_right),
        Position("bottom_left", R.string.position_bottom_left),
        Position("bottom_center", R.string.position_bottom_center),
        Position("bottom_right", R.string.position_bottom_right),
    )
}

/**
 * 图床供应商配置字段定义
 */
object ProviderFieldDefs {

    data class FieldDef(
        val key: String,
        val labelKey: String,
        val placeholder: String,
        val isSecret: Boolean = false,
        val isRequired: Boolean = true,
        val helpTextKey: String = "",
    )

    val GITHUB = listOf(
        FieldDef("repo", "field_repo_name", "username/repo", isRequired = true),
        FieldDef("branch", "field_branch", "main", isRequired = true),
        FieldDef("path", "field_upload_path", "images", isRequired = false),
        FieldDef("token", "field_token", "ghp_xxxx", isSecret = true, isRequired = true, helpTextKey = "help_text_github_token"),
        FieldDef("domain", "field_custom_domain", "https://cdn.example.com", isRequired = false),
    )

    val QINIU = listOf(
        FieldDef("ak", "field_access_key", "", isSecret = true, isRequired = true),
        FieldDef("sk", "field_secret_key", "", isSecret = true, isRequired = true),
        FieldDef("bucket", "field_bucket", "", isRequired = true),
        FieldDef("region", "field_qiniu_region", "z0", isRequired = true, helpTextKey = "help_text_qiniu_region"),
        FieldDef("domain", "field_upload_domain", "https://cdn.example.com", isRequired = true),
    )

    val SMMS = listOf(
        FieldDef("token", "field_api_token", "", isSecret = true, isRequired = true, helpTextKey = "help_text_smms_token"),
    )

    val ALIYUN = listOf(
        FieldDef("akId", "field_access_key_id", "", isSecret = true, isRequired = true),
        FieldDef("akSecret", "field_access_key_secret", "", isSecret = true, isRequired = true),
        FieldDef("bucket", "field_bucket", "", isRequired = true),
        FieldDef("region", "field_region", "oss-cn-hangzhou", isRequired = true),
        FieldDef("domain", "field_custom_domain", "", isRequired = false),
    )

    val TENCENT = listOf(
        FieldDef("secretId", "field_secret_id", "", isSecret = true, isRequired = true),
        FieldDef("secretKey", "field_secret_key", "", isSecret = true, isRequired = true),
        FieldDef("bucket", "field_bucket", "", isRequired = true),
        FieldDef("region", "field_region", "ap-guangzhou", isRequired = true),
        FieldDef("domain", "field_custom_domain", "", isRequired = false),
    )

    val IMGUR = listOf(
        FieldDef("clientId", "field_client_id", "", isSecret = true, isRequired = true, helpTextKey = "help_text_imgur_client_id"),
    )

    val CUSTOM = listOf(
        FieldDef("url", "field_endpoint", "https://api.example.com/upload", isRequired = true),
        FieldDef("method", "field_method", "POST", isRequired = true),
        FieldDef("headers", "field_headers", "{\"Authorization\": \"Bearer xxx\"}", isSecret = true, isRequired = false),
        FieldDef("response", "field_response_rule", "$.data.url", isRequired = true, helpTextKey = "help_text_custom_response"),
    )

    fun getFields(provider: String): List<FieldDef> = when (provider) {
        "github" -> GITHUB
        "qiniu" -> QINIU
        "smms" -> SMMS
        "aliyun" -> ALIYUN
        "tencent" -> TENCENT
        "imgur" -> IMGUR
        "custom" -> CUSTOM
        else -> emptyList()
    }

    fun getHelpTextResId(helpTextKey: String): Int = when (helpTextKey) {
        "help_text_github_token" -> R.string.help_text_github_token
        "help_text_smms_token" -> R.string.help_text_smms_token
        "help_text_imgur_client_id" -> R.string.help_text_imgur_client_id
        "help_text_custom_response" -> R.string.help_text_custom_response
        "help_text_qiniu_region" -> R.string.help_text_qiniu_region
        else -> 0
    }
}
