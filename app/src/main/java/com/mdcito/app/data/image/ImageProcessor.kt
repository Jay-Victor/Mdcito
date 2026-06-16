package com.mdcito.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import com.mdcito.app.data.model.ImageProcessingSettings
import com.mdcito.app.data.model.RenameVariables
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
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

    companion object {
        /** 单张图片最大字节数（20MB），超过则拒绝处理 */
        private const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
        /** 解码时的目标采样上限（宽×高），避免 OOM */
        private const val MAX_DECODE_PIXELS = 4096 * 4096
        /** 允许上传的图片格式扩展名白名单（SVG 因可内嵌脚本导致存储型 XSS，已移除） */
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    }

    /**
     * 处理图片：根据设置进行压缩、缩放、水印等操作
     * @param imageFile 原始图片文件
     * @param fileName 原始文件名
     * @param settings 图片处理设置
     * @return 处理后的图片数据和最终文件名
     */
    fun process(imageFile: File, fileName: String, settings: ImageProcessingSettings): ProcessedResult {
        // 大小校验：拒绝过大的文件，避免内存与流量浪费
        val fileBytes = imageFile.length()
        if (fileBytes > MAX_IMAGE_BYTES) {
            Timber.tag("ImageProcessor").w("图片过大（%d 字节），拒绝处理: %s", fileBytes, fileName)
            return ProcessedResult(
                imageData = ByteArray(0),
                fileName = resolveFileName(fileName, settings),
            )
        }

        // 格式校验：仅允许白名单扩展名
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty() && ext !in ALLOWED_EXTENSIONS) {
            Timber.tag("ImageProcessor").w("不支持的图片格式: %s", ext)
            return ProcessedResult(
                imageData = ByteArray(0),
                fileName = resolveFileName(fileName, settings),
            )
        }

        // 先用 inJustDecodeBounds 探测尺寸，再按 inSampleSize 降采样解码，避免 OOM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)
        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight

        // 解码失败（非图片格式或 BitmapFactory 不支持）：剥离 EXIF 后返回原始字节
        if (srcWidth <= 0 || srcHeight <= 0) {
            Timber.tag("ImageProcessor").w("无法解码图片，剥离 EXIF 后上传原始字节: %s", fileName)
            val stripped = stripExifFromBytes(imageFile.readBytes(), ext)
            return ProcessedResult(
                imageData = stripped,
                fileName = resolveFileName(fileName, settings),
            )
        }

        // 计算 inSampleSize：按设置中的缩放上限与全局像素上限取较严格者
        val targetMaxWidth = if (settings.resizeEnabled) settings.resizeMaxWidth else Int.MAX_VALUE
        val targetMaxHeight = if (settings.resizeEnabled) settings.resizeMaxHeight else Int.MAX_VALUE
        val sampleSize = calculateInSampleSize(srcWidth, srcHeight, targetMaxWidth, targetMaxHeight)

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
            ?: run {
                // 解码失败回退：剥离 EXIF 后上传原始字节，防止隐私泄露
                Timber.tag("ImageProcessor").w("解码图片失败，剥离 EXIF 后上传原始字节: %s", fileName)
                val stripped = stripExifFromBytes(imageFile.readBytes(), ext)
                return ProcessedResult(
                    imageData = stripped,
                    fileName = resolveFileName(fileName, settings),
                )
            }

        // 缩放
        if (settings.resizeEnabled) {
            val resized = resizeBitmap(bitmap, settings.resizeMaxWidth, settings.resizeMaxHeight)
            if (resized !== bitmap) {
                bitmap.recycle()
                bitmap = resized
            }
        }

        // 水印
        if (settings.watermarkEnabled && settings.watermarkText.isNotBlank()) {
            val watermarked = addWatermark(bitmap, settings.watermarkText, settings.watermarkPosition, settings.watermarkOpacity)
            if (watermarked !== bitmap) {
                bitmap.recycle()
                bitmap = watermarked
            }
        }

        // 压缩并输出（保留原始格式）。compress 会重新编码，EXIF 不会保留
        val imageData = compressBitmap(bitmap, fileName, settings)
        bitmap.recycle()

        // 确定最终文件名
        val finalFileName = resolveFileName(fileName, settings)

        return ProcessedResult(imageData = imageData, fileName = finalFileName)
    }

    /**
     * 计算 inSampleSize，使解码后的尺寸不超过目标上限与全局像素上限
     */
    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, maxWidth: Int, maxHeight: Int): Int {
        var sampleSize = 1
        // 全局像素上限：避免超大图直接全分辨率解码
        while ((srcWidth / sampleSize) * (srcHeight / sampleSize) > MAX_DECODE_PIXELS) {
            sampleSize *= 2
        }
        // 缩放上限
        while ((srcWidth / sampleSize) > maxWidth * 2 || (srcHeight / sampleSize) > maxHeight * 2) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
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
     * 重新编码不会保留 EXIF 元数据，天然剥离隐私信息
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
     * 从原始字节中剥离 EXIF 隐私信息（GPS、设备型号、拍摄时间等）。
     * 用于无法解码的图片回退上传场景，防止隐私泄露。
     * 仅对 JPEG 有效（EXIF 仅存在于 JPEG）；其他格式直接返回原字节。
     */
    private fun stripExifFromBytes(data: ByteArray, ext: String): ByteArray {
        if (data.isEmpty()) return data
        // 非 JPEG 格式无标准 EXIF，直接返回
        if (ext != "jpg" && ext != "jpeg") return data
        return try {
            val sensitiveTags = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_AREA_INFORMATION,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_LENS_SERIAL_NUMBER,
            )
            // ExifInterface 需要可写文件路径才能 saveAttributes，使用临时文件
            val tempFile = File.createTempFile("strip_exif_", ".jpg")
            try {
                tempFile.outputStream().use { it.write(data) }
                val fileExif = ExifInterface(tempFile.absolutePath)
                sensitiveTags.forEach { tag -> fileExif.setAttribute(tag, null) }
                fileExif.saveAttributes()
                tempFile.readBytes()
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Timber.tag("ImageProcessor").w(e, "剥离 EXIF 失败，返回原始字节")
            data
        }
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
