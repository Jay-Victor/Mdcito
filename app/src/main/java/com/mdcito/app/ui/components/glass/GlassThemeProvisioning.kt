package com.mdcito.app.ui.components.glass

// =============================================================================
// 液态玻璃设计 - 主题级 Backdrop/LiquidState 装配 (Theme Glass Provisioning)
// =============================================================================
// 功能说明:
//   在应用主题层级装配液态玻璃所需的 Backdrop 和 LiquidState。
//   这是液态玻璃效果的"基础设施" - 必须在根主题中正确配置,
//   子组件的 .liquidGlass() 和 .waterGlass() 修饰符才能正常工作。
//
// 装配结构 (层级关系):
//   BoxWithConstraints (根)
//     └─ CompositionLocalProvider (提供 LocalLiquidGlassBackdrop + LocalWaterGlassState)
//          └─ Box (根容器, fillMaxSize)
//               ├─ Box.layerBackdrop(liquidGlassBackdrop)  ← Backdrop 捕获层【仅含背景】
//               │    ├─ Box.liquefiable() (纯色背景)
//               │    └─ Box.liquefiable() { backgroundContent() } (背景图)
//               └─ content()  ← 应用内容【同级,不在捕获范围内】
//                    └─ 子组件用 .liquidGlass()/.waterGlass() 采样背景纹理
//
// 关键注意事项:
//   1. content() 必须在 layerBackdrop Box 的【同级】, 不能在其【内部】
//      否则 drawBackdrop 的输出会被 layerBackdrop 捕获, 形成渲染树自引用环
//      → RenderNode::prepareTreeImpl 无限递归 → 栈溢出 SIGSEGV
//   2. liquefiable() 必须在希望被水玻璃采样的背景层上调用
//   3. rememberLiquidState() 在 API < 33 时返回 null，需做空判断
//   4. 背景层顺序很重要: liquefiable 的层应在内容层之下
// =============================================================================

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// ---- kyant backdrop: LayerBackdrop 装配 ----
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
// ---- fletchmckee liquid: LiquidState 装配 ----
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

/**
 * 液态玻璃主题装配
 *
 * 在主题层级正确装配液态玻璃基础设施。应用内容中的 .liquidGlass() / .waterGlass()
 * 会自动从此处装配的 Backdrop / LiquidState 获取背景。
 *
 * @param darkTheme 是否暗色主题 (决定兜底底色为黑或白)
 * @param backgroundContent 背景层内容（由调用方提供，例如项目的 BackgroundImage 组件）
 * @param content 应用内容
 */
@Composable
fun GlassThemeProvisioning(
    darkTheme: Boolean,
    backgroundContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // ===== 步骤 1: 创建液态玻璃 Backdrop 实例 =====
        val liquidGlassBackdrop = rememberLayerBackdrop()

        // ===== 步骤 2: 创建水玻璃 LiquidState 实例 (仅 API >= 33) =====
        val waterGlassState = if (isWaterGlassSupported()) rememberLiquidState() else null

        // ===== 步骤 3: 通过 CompositionLocal 向子树传递 Backdrop/State =====
        CompositionLocalProvider(
            LocalLiquidGlassBackdrop provides liquidGlassBackdrop,
            LocalWaterGlassState provides waterGlassState,
        ) {
            // ===== 步骤 4: 根容器 (fillMaxSize) =====
            // 背景捕获层与应用内容作为同级节点, 避免渲染树自引用环。
            Box(modifier = Modifier.fillMaxSize()) {
                // ===== 步骤 5: Backdrop 捕获层【仅含背景, 不含 drawBackdrop 调用】=====
                // layerBackdrop 捕获此 Box 内所有内容作为纹理, 供 drawBackdrop 采样。
                // 注意: 不能在此 Box 内放置使用 .liquidGlass() 的组件,
                // 否则 drawBackdrop 输出会被再次捕获 → 渲染树环 → 栈溢出。
                Box(
                    modifier = Modifier.fillMaxSize().layerBackdrop(liquidGlassBackdrop)
                ) {
                    // ===== 步骤 5a: 兜底纯色背景 + 液化标记 =====
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(if (darkTheme) Color.Black else Color.White)
                                .then(
                                    if (waterGlassState != null) {
                                        Modifier.liquefiable(waterGlassState)
                                    } else {
                                        Modifier
                                    },
                                )
                    )

                    // ===== 步骤 5b: 调用方提供的背景层 (例如 BackgroundImage) =====
                    // 同样标记为 liquefiable，水玻璃可采样到背景图内容。
                    if (waterGlassState != null) {
                        Box(modifier = Modifier.fillMaxSize().liquefiable(waterGlassState)) {
                            backgroundContent()
                        }
                    } else {
                        backgroundContent()
                    }
                }

                // ===== 步骤 6: 应用内容【同级节点, 不在 layerBackdrop 捕获范围内】=====
                // 子组件的 .liquidGlass()/.waterGlass() 通过 LocalLiquidGlassBackdrop
                // 获取背景纹理并采样, 不会将自身输出反馈回捕获层, 避免渲染树环。
                content()
            }
        }
    }
}
