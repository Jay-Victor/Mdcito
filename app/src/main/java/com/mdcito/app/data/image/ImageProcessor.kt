package com.mdcito.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.mdcito.app.data.model.ImageProcessingSettings
import com.mdcito.app.data.model.RenameVariables
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片处理器
 * 负责图片的压缩、缩放、水印和重命名等预处理操作
 */
@Singleton
class ImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * 处理图片：根据设置进行压缩、缩放、水印等操作
     * @param imageFile 原始图片文件
     * @param fileName 原始文件名
     * @param settings 图片处理设置
     * @return 处理后的图片数据和最终文件名
     */
    fun process(imageFile: File, fileName: String, settings: ImageProcessingSettings): ProcessedResult {
        // 读取原始图片
        var bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: return ProcessedResult(
                imageData = imageFile.readBytes(),
                fileName = resolveFileName(fileName, settings),
            )

        // 缩放
        if (settings.resizeEnabled) {
            bitmap = resizeBitmap(bitmap, settings.resizeMaxWidth, settings.resizeMaxHeight)
        }

        // 水印
        if (settings.watermarkEnabled && settings.watermarkText.isNotBlank()) {
            bitmap = addWatermark(bitmap, settings.watermarkText, settings.watermarkPosition, settings.watermarkOpacity)
        }

        // 压缩并输出（保留原始格式）
        val imageData = compressBitmap(bitmap, fileName, settings)

        // 确定最终文件名
        val finalFileName = resolveFileName(fileName, settings)

        return ProcessedResult(imageData = imageData, fileName = finalFileName)
    }

    /**
     * 缩放图片，保持宽高比
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * 添加文字水印
     */
    private fun addWatermark(bitmap: Bitmap, text: String, position: String, opacity: Int): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (opacity / 100f * 255).toInt().coerceIn(0, 255)
            textSize = bitmap.width / 25f
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }

        val textWidth = paint.measureText(text)
        val textHeight = paint.textSize
        val padding = bitmap.width / 25f

        val (x, y) = when (position) {
            "top_left" -> padding to textHeight + padding
            "top_center" -> (bitmap.width - textWidth) / 2 to textHeight + padding
            "top_right" -> bitmap.width - textWidth - padding to textHeight + padding
            "center_left" -> padding to (bitmap.height + textHeight) / 2
            "center" -> (bitmap.width - textWidth) / 2 to (bitmap.height + textHeight) / 2
            "center_right" -> bitmap.width - textWidth - padding to (bitmap.height + textHeight) / 2
            "bottom_left" -> padding to bitmap.height - padding
            "bottom_center" -> (bitmap.width - textWidth) / 2 to bitmap.height - padding
            "bottom_right" -> bitmap.width - textWidth - padding to bitmap.height - padding
            else -> bitmap.width - textWidth - padding to bitmap.height - padding
        }

        canvas.drawText(text, x, y, paint)
        return result
    }

    /**
     * 压缩图片为字节数组，保留原始格式
     * PNG/WebP 使用无损压缩，JPEG 使用有损压缩
     */
    private fun compressBitmap(bitmap: Bitmap, fileName: String, settings: ImageProcessingSettings): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val quality = if (settings.compressEnabled) settings.compressQuality else 90
        val ext = fileName.substringAfterLast('.', "jpg").lowercase()
        val format = when (ext) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        // PNG 是无损格式，quality 参数无效，但仍需传入
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * 根据重命名规则生成最终文件名
     */
    private fun resolveFileName(originalName: String, settings: ImageProcessingSettings): String {
        if (!settings.autoRenameEnabled) return originalName

        val extension = originalName.substringAfterLast('.', "jpg")
        val baseName = originalName.substringBeforeLast('.', originalName)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        var result = settings.renameRule
        result = result.replace(RenameVariables.TIMESTAMP, System.currentTimeMillis().toString())
        result = result.replace(RenameVariables.DATE, sdf.format(Date()))
        result = result.replace(RenameVariables.RANDOM, (1000..9999).random().toString())
        result = result.replace(RenameVariables.ORIGINAL, baseName)
        result = result.replace(RenameVariables.UUID, UUID.randomUUID().toString().substring(0, 8))

        return "$result.$extension"
    }

    /**
     * 处理结果
     */
    data class ProcessedResult(
        val imageData: ByteArray,
        val fileName: String,
    )
}
