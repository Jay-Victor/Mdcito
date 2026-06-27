package com.mdcito.app.ui.components.glass

// =============================================================================
// 磨砂玻璃设计 - 系统级跨窗口模糊 (System Cross-Window Blur)
// =============================================================================
// Android 系统级的跨窗口模糊效果。与 Compose 的 Modifier.blur 不同，
// 这种模糊由 Android 系统的 SurfaceFlinger 在合成层面完成，可以模糊"本窗口之外"
// 的所有内容(包括其他应用)，性能极高且效果类似 iOS 的控制中心磨砂玻璃。
//
// 关键技术点:
//   - WindowManager.LayoutParams.FLAG_BLUR_BEHIND: 系统窗口模糊标志位
//   - params.setBlurBehindRadius(radiusPx): 设置模糊半径(像素)
//   - windowManager.isCrossWindowBlurEnabled: 运行时检测设备是否支持跨窗口模糊
//   - 需要 API >= 31 (Android S / 12)
//
// 应用场景: 悬浮窗/Overlay 全屏模式时，启用 48dp 半径的系统模糊。
// 注意: 该方法仅适用于通过 WindowManager 添加的系统窗口。
// =============================================================================

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf

/**
 * 系统级跨窗口模糊半径常量 (单位: dp)
 *
 * 48dp 提供适中的磨砂玻璃质感。
 * 可根据设计需求调整: 20dp(轻磨砂) ~ 80dp(重磨砂)
 */
const val FULLSCREEN_BLUR_RADIUS_DP = 48

/**
 * 全屏系统模糊是否生效的运行时状态
 *
 * 即使设置了 FLAG_BLUR_BEHIND，设备也未必真正生效
 * (厂商可能禁用，或省电模式下关闭)，因此需要运行时追踪实际状态。
 */
val fullscreenSystemBlurActive = mutableStateOf(false)

/**
 * 应用全屏系统窗口模糊
 *
 * 在 WindowManager.LayoutParams 上设置跨窗口模糊。
 *
 * @param params 窗口布局参数
 * @param enabled 是否启用模糊
 * @param windowManager WindowManager 实例
 * @param context Context (用于获取 density 转换 dp→px)
 *
 * 工作流程:
 *   1. 检查 API >= 31 (Android S)，低版本直接跳过
 *   2. 检测 windowManager.isCrossWindowBlurEnabled (设备是否支持)
 *   3. 启用时: 添加 FLAG_BLUR_BEHIND 标志 + 设置模糊半径
 *   4. 禁用时: 清除标志 + 半径归零
 *   5. 更新 fullscreenSystemBlurActive 状态供 UI 层判断
 */
fun applyFullscreenBlur(
    params: WindowManager.LayoutParams,
    enabled: Boolean,
    windowManager: WindowManager,
    context: Context,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        fullscreenSystemBlurActive.value = false
        return
    }

    val crossWindowBlurEnabled = windowManager.isCrossWindowBlurEnabled

    if (enabled) {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        val density = context.resources.displayMetrics.density
        val blurRadiusPx = (FULLSCREEN_BLUR_RADIUS_DP * density).toInt()
        params.setBlurBehindRadius(blurRadiusPx)
    } else {
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        params.setBlurBehindRadius(0)
    }

    fullscreenSystemBlurActive.value = enabled && crossWindowBlurEnabled
}

/**
 * 应用全屏悬浮窗的显示 cutout 模式
 *
 * 配合模糊使用，让悬浮窗可以延伸到刘海/挖孔区域，实现真正的全屏磨砂玻璃覆盖。
 */
fun applyFullscreenOverlayWindowPolicy(
    params: WindowManager.LayoutParams,
    enabled: Boolean,
) {
    if (!enabled) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        params.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}
