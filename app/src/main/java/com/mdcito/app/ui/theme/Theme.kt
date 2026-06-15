package com.mdcito.app.ui.theme

import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

fun buildLightColorScheme(themeColor: Color, scheme: MdcitoColors.SchemeColors): androidx.compose.material3.ColorScheme {
    val primaryDark = themeColor.copy(alpha = 0.7f)
    val primaryLight = themeColor.copy(alpha = 0.4f)
    val primarySurface = themeColor.copy(alpha = 0.08f)
    return lightColorScheme(
        primary = themeColor,
        onPrimary = Color.White,
        primaryContainer = primaryLight,
        onPrimaryContainer = primaryDark,
        secondary = primaryLight,
        onSecondary = scheme.onBackground ?: Color(0xFF3A3632),
        secondaryContainer = primarySurface,
        onSecondaryContainer = primaryDark,
        tertiary = Color(0xFF8B7EC8),
        onTertiary = Color.White,
        error = Color(0xFFC8796E),
        onError = Color.White,
        background = scheme.background,
        onBackground = scheme.onBackground ?: Color(0xFF3A3632),
        surface = scheme.surface,
        onSurface = scheme.onSurface ?: Color(0xFF3A3632),
        surfaceVariant = scheme.surfaceContainerHigh,
        onSurfaceVariant = scheme.onSurfaceVariant ?: Color(0xFF8A847C),
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant ?: Color(0xFFB0A99F),
        scrim = Color(0x663A3632),
        inverseSurface = scheme.surface,
        inverseOnSurface = scheme.onSurface ?: Color(0xFF3A3632),
    )
}

fun buildDarkColorScheme(themeColor: Color, scheme: MdcitoColors.SchemeColors): androidx.compose.material3.ColorScheme {
    val primaryDark = themeColor.copy(alpha = 0.3f)
    return darkColorScheme(
        primary = themeColor,
        onPrimary = Color(0xFF1A1816),
        primaryContainer = primaryDark,
        onPrimaryContainer = themeColor,
        secondary = Color(0xFF6B8A7E),
        onSecondary = scheme.onBackground ?: Color(0xFFE5E0D8),
        secondaryContainer = Color(0x148BB5A6),
        onSecondaryContainer = themeColor,
        tertiary = Color(0xFFA898D8),
        onTertiary = Color(0xFF1A1816),
        error = Color(0xFFE88A80),
        onError = Color.White,
        background = scheme.background,
        onBackground = scheme.onBackground ?: Color(0xFFE5E0D8),
        surface = scheme.surface,
        onSurface = scheme.onSurface ?: Color(0xFFE5E0D8),
        surfaceVariant = scheme.surfaceContainerHigh,
        onSurfaceVariant = scheme.onSurfaceVariant ?: Color(0xFF9A948C),
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant ?: Color(0xFF3A3632),
        scrim = Color(0x8C000000),
        inverseSurface = scheme.surface,
        inverseOnSurface = scheme.onSurface ?: Color(0xFFE5E0D8),
    )
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * 应用内深色主题状态 — 由 MdcitoTheme 根据用户选择的主题模式提供。
 * 替代 isSystemInDarkTheme()，确保玻璃效果等组件正确跟随应用内主题切换。
 */
val LocalIsDarkTheme = androidx.compose.runtime.compositionLocalOf { false }

private fun resolveSchemeColors(
    isDark: Boolean,
    lightScheme: String,
    darkScheme: String,
): MdcitoColors.SchemeColors {
    return if (isDark) {
        when (darkScheme) {
            "cool_dark" -> MdcitoColors.Scheme.Dark.coolDark
            "oled" -> MdcitoColors.Scheme.Dark.oled
            "midnight" -> MdcitoColors.Scheme.Dark.midnight
            else -> MdcitoColors.Scheme.Dark.warmDark
        }
    } else {
        when (lightScheme) {
            "cool" -> MdcitoColors.Scheme.Light.cool
            "paper" -> MdcitoColors.Scheme.Light.paper
            "fresh" -> MdcitoColors.Scheme.Light.fresh
            else -> MdcitoColors.Scheme.Light.warm
        }
    }
}

@Composable
fun MdcitoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    themeColorIndex: Int = 0,
    lightScheme: String = "warm",
    darkScheme: String = "warm_dark",
    uiFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    uiFontSize: Int? = null,
    uiModeVersion: Int = 0,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> {
            // uiModeVersion 变化时触发重组，isSystemInDarkTheme() 重新读取当前配置
            @Suppress("UNUSED_EXPRESSION")
            uiModeVersion
            isSystemInDarkTheme()
        }
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    // 监听壁纸颜色变化，确保壁纸更换后动态色自动更新
    var wallpaperColorVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { _, _ ->
                wallpaperColorVersion++
            }
            try {
                wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {
                // 某些 OEM 实现可能抛出异常，安全忽略
            }
            onDispose {
                try {
                    wallpaperManager.removeOnColorsChangedListener(listener)
                } catch (_: Exception) {
                    // 安全忽略
                }
            }
        } else {
            onDispose {}
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // wallpaperColorVersion 作为 key 确保壁纸变化时重新获取颜色
            remember(wallpaperColorVersion, darkTheme, dynamicColor) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
        }
        else -> {
            val themeColor = MdcitoColors.ThemeColors.all.getOrElse(themeColorIndex) {
                MdcitoColors.ThemeColors.all[0]
            }
            val schemeColors = resolveSchemeColors(darkTheme, lightScheme, darkScheme)
            if (darkTheme) buildDarkColorScheme(themeColor, schemeColors)
            else buildLightColorScheme(themeColor, schemeColors)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? AppCompatActivity
        activity?.enableEdgeToEdge()
    }

    val typography = if (uiFontFamily != null || uiFontSize != null) {
        val scaleFactor = if (uiFontSize != null && uiFontSize != 14) {
            uiFontSize.toFloat() / 14f
        } else 1f
        Typography(
            displayLarge = MdcitoTypography.displayLarge.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.displayLarge.fontFamily,
                fontSize = (MdcitoTypography.displayLarge.fontSize.value * scaleFactor).sp,
            ),
            headlineLarge = MdcitoTypography.headlineLarge.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.headlineLarge.fontFamily,
                fontSize = (MdcitoTypography.headlineLarge.fontSize.value * scaleFactor).sp,
            ),
            headlineMedium = MdcitoTypography.headlineMedium.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.headlineMedium.fontFamily,
                fontSize = (MdcitoTypography.headlineMedium.fontSize.value * scaleFactor).sp,
            ),
            headlineSmall = MdcitoTypography.headlineSmall.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.headlineSmall.fontFamily,
                fontSize = (MdcitoTypography.headlineSmall.fontSize.value * scaleFactor).sp,
            ),
            titleLarge = MdcitoTypography.titleLarge.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.titleLarge.fontFamily,
                fontSize = (MdcitoTypography.titleLarge.fontSize.value * scaleFactor).sp,
            ),
            titleMedium = MdcitoTypography.titleMedium.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.titleMedium.fontFamily,
                fontSize = (MdcitoTypography.titleMedium.fontSize.value * scaleFactor).sp,
            ),
            titleSmall = MdcitoTypography.titleSmall.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.titleSmall.fontFamily,
                fontSize = (MdcitoTypography.titleSmall.fontSize.value * scaleFactor).sp,
            ),
            bodyLarge = MdcitoTypography.bodyLarge.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.bodyLarge.fontFamily,
                fontSize = (MdcitoTypography.bodyLarge.fontSize.value * scaleFactor).sp,
            ),
            bodyMedium = MdcitoTypography.bodyMedium.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.bodyMedium.fontFamily,
                fontSize = (MdcitoTypography.bodyMedium.fontSize.value * scaleFactor).sp,
            ),
            bodySmall = MdcitoTypography.bodySmall.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.bodySmall.fontFamily,
                fontSize = (MdcitoTypography.bodySmall.fontSize.value * scaleFactor).sp,
            ),
            labelLarge = MdcitoTypography.labelLarge.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.labelLarge.fontFamily,
                fontSize = (MdcitoTypography.labelLarge.fontSize.value * scaleFactor).sp,
            ),
            labelMedium = MdcitoTypography.labelMedium.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.labelMedium.fontFamily,
                fontSize = (MdcitoTypography.labelMedium.fontSize.value * scaleFactor).sp,
            ),
            labelSmall = MdcitoTypography.labelSmall.copy(
                fontFamily = uiFontFamily ?: MdcitoTypography.labelSmall.fontFamily,
                fontSize = (MdcitoTypography.labelSmall.fontSize.value * scaleFactor).sp,
            ),
        )
    } else {
        MdcitoTypography
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MdcitoShapes,
            content = content,
        )
    }
}
