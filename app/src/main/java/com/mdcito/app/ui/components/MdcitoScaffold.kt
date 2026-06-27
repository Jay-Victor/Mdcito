package com.mdcito.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mdcito.app.ui.components.glass.GlassThemeProvisioning
import com.mdcito.app.ui.files.FilesScreen
import com.mdcito.app.ui.history.HistoryScreen
import com.mdcito.app.ui.home.HomeScreen
import com.mdcito.app.ui.navigation.Route
import com.mdcito.app.ui.navigation.bottomNavRoutes
import com.mdcito.app.ui.settings.SettingsScreen
import com.mdcito.app.ui.settings.SettingsViewModel
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

private const val ENTER_DURATION = 300
private const val ENTER_FADE_DURATION = 210
private const val ENTER_DELAY = 90
private const val EXIT_FADE_DURATION = 90

// 用于 AnimatedContent 的屏幕状态类型，区分设置子页面以应用不同的过渡动画
sealed interface ScreenState {
    data object MainTab : ScreenState
    data class SubPage(val isSettingsChild: Boolean) : ScreenState
}

private fun determineScreenState(route: String?): ScreenState {
    if (route == null) return ScreenState.SubPage(isSettingsChild = false)
    val mainTabRoutes = bottomNavRoutes.map { it.route }.toSet()
    if (route in mainTabRoutes) return ScreenState.MainTab
    return ScreenState.SubPage(isSettingsChild = route.startsWith("settings/"))
}

@Composable
fun MdcitoScaffold(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    // 读取软件背景设置
    val backgroundImageUri by settingsViewModel.backgroundImageUri.collectAsState()
    val backgroundBlur by settingsViewModel.backgroundBlur.collectAsState()
    val backgroundBlurIntensity by settingsViewModel.backgroundBlurIntensity.collectAsState()
    val backgroundBrightness by settingsViewModel.backgroundBrightness.collectAsState()
    val navBarStyle by settingsViewModel.navBarStyle.collectAsState()
    val navBarTransparency by settingsViewModel.navBarTransparency.collectAsState()
    val navBarGlassIntensity by settingsViewModel.navBarGlassIntensity.collectAsState()
    val cardStyle by settingsViewModel.cardStyle.collectAsState()
    // ── 每种风格独立的调节参数 ──
    val cardMinimalTransparency by settingsViewModel.cardMinimalTransparency.collectAsState()
    val cardFrostedIntensity by settingsViewModel.cardFrostedIntensity.collectAsState()
    val cardLiquidIntensity by settingsViewModel.cardLiquidIntensity.collectAsState()
    val navBarMinimalTransparency by settingsViewModel.navBarMinimalTransparency.collectAsState()
    val navBarFrostedIntensity by settingsViewModel.navBarFrostedIntensity.collectAsState()
    val navBarLiquidIntensity by settingsViewModel.navBarLiquidIntensity.collectAsState()

    // 根据当前 cardStyle 选择对应的 glassIntensity 和 transparency
    val resolvedCardGlassIntensity = when (cardStyle) {
        "frosted_glass" -> cardFrostedIntensity
        "liquid_glass" -> cardLiquidIntensity
        else -> 0
    }
    val resolvedCardTransparency = when (cardStyle) {
        "minimal" -> cardMinimalTransparency
        else -> 100
    }
    // 根据当前 navBarStyle 选择对应的值
    val resolvedNavBarGlassIntensity = when (navBarStyle) {
        "frosted_glass" -> navBarFrostedIntensity
        "liquid_glass" -> navBarLiquidIntensity
        else -> 0
    }
    val resolvedNavBarTransparency = when (navBarStyle) {
        "minimal" -> navBarMinimalTransparency
        else -> 100
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { bottomNavRoutes.size })
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnMainTab = bottomNavRoutes.any { it.route == currentRoute }
    val screenState = determineScreenState(currentRoute)

    LaunchedEffect(currentRoute) {
        val pageIndex = bottomNavRoutes.indexOfFirst { it.route == currentRoute }
        if (pageIndex >= 0 && pagerState.currentPage != pageIndex) {
            pagerState.scrollToPage(pageIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetRoute = bottomNavRoutes[page].route
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(Route.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    // 软件背景图片（主标签页和设置子页面显示，编辑器页面使用自己的背景）
    val isEditorPage = currentRoute?.startsWith("editor") == true || currentRoute == "new_file"

    val isDarkTheme = LocalIsDarkTheme.current

    CompositionLocalProvider(
        LocalCardStyle provides when (cardStyle) {
            "frosted_glass" -> CardStyle.FROSTED_GLASS
            "liquid_glass" -> CardStyle.LIQUID_GLASS
            else -> CardStyle.MINIMAL
        },
        LocalCardGlassIntensity provides resolvedCardGlassIntensity,
        LocalCardTransparency provides resolvedCardTransparency,
    ) {
        // ── 装配液态/水玻璃基础设施 ──
        // GlassThemeProvisioning 在主题层级创建 Backdrop + LiquidState，
        // 并将背景层标记为 liquefiable，使子组件的 .liquidGlass() / .waterGlass() 可正常工作。
        GlassThemeProvisioning(
            darkTheme = isDarkTheme,
            backgroundContent = {
                // 背景层：覆盖整个屏幕（包括导航栏区域）
                // 编辑器页面自行渲染背景，这里仅在非编辑器页面渲染主背景
                if (!isEditorPage) {
                    BackgroundImage(
                        imageUri = backgroundImageUri,
                        blurEnabled = backgroundBlur,
                        blurIntensity = backgroundBlurIntensity,
                        brightness = backgroundBrightness,
                    )
                    // 无自定义背景图时，使用主题背景色作为默认背景
                    if (backgroundImageUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                } else {
                    // 编辑器页面：仍渲染主题背景色作为水玻璃采样兜底
                    // 编辑器内容会在此层之上渲染其自己的背景图
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            },
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    if (isOnMainTab) {
                        MdcitoBottomNavBar(
                            currentRoute = bottomNavRoutes[pagerState.currentPage].route,
                            onNavigate = { route ->
                                val pageIndex = bottomNavRoutes.indexOfFirst { it.route == route }
                                if (pageIndex >= 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pageIndex)
                                    }
                                }
                            },
                            navBarStyle = navBarStyle,
                            navBarTransparency = resolvedNavBarTransparency,
                            navBarGlassIntensity = resolvedNavBarGlassIntensity,
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            // 编辑器页面自行处理状态栏沉浸，不应用顶部内边距
                            if (isEditorPage) {
                                Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            } else {
                                Modifier.padding(innerPadding)
                            }
                        ),
                ) {
                    AnimatedContent(
                        targetState = screenState,
                        transitionSpec = {
                            val initial = initialState
                            val target = targetState
                            when {
                                // 主Tab → 设置子页面：水平滑动（SharedXAxis，从右侧滑入）
                                initial is ScreenState.MainTab && target is ScreenState.SubPage && target.isSettingsChild -> {
                                    (slideInHorizontally(
                                        initialOffsetX = { it / 3 },
                                        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = ENTER_FADE_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    )) togetherWith (slideOutHorizontally(
                                        targetOffsetX = { -it / 3 },
                                        animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = EXIT_FADE_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ))
                                }
                                // 设置子页面 → 主Tab：水平滑动（SharedXAxis，向右滑出）
                                initial is ScreenState.SubPage && initial.isSettingsChild && target is ScreenState.MainTab -> {
                                    (slideInHorizontally(
                                        initialOffsetX = { -it / 3 },
                                        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = ENTER_FADE_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    )) togetherWith (slideOutHorizontally(
                                        targetOffsetX = { it / 3 },
                                        animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = EXIT_FADE_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ))
                                }
                                // 主Tab → 其他子页面（如编辑器）：缩放+淡入淡出
                                initial is ScreenState.MainTab -> {
                                    (fadeIn(
                                        animationSpec = tween(
                                            durationMillis = ENTER_FADE_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    ) + scaleIn(
                                        initialScale = 0.85f,
                                        animationSpec = tween(
                                            durationMillis = ENTER_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    )) togetherWith (fadeOut(
                                        animationSpec = tween(
                                            durationMillis = EXIT_FADE_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ) + scaleOut(
                                        targetScale = 0.92f,
                                        animationSpec = tween(
                                            durationMillis = ENTER_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ))
                                }
                                // 其他子页面 → 主Tab：缩放+淡入淡出
                                target is ScreenState.MainTab -> {
                                    (fadeIn(
                                        animationSpec = tween(
                                            durationMillis = ENTER_FADE_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    ) + scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(
                                            durationMillis = ENTER_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    )) togetherWith (fadeOut(
                                        animationSpec = tween(
                                            durationMillis = EXIT_FADE_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ) + scaleOut(
                                        targetScale = 1.05f,
                                        animationSpec = tween(
                                            durationMillis = ENTER_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ))
                                }
                                // 其他情况（如设置子页面之间切换不会走到这里，由 NavHost 处理）
                                else -> {
                                    (fadeIn(
                                        animationSpec = tween(
                                            durationMillis = ENTER_FADE_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    ) + scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(
                                            durationMillis = ENTER_DURATION,
                                            delayMillis = ENTER_DELAY,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    )) togetherWith (fadeOut(
                                        animationSpec = tween(
                                            durationMillis = EXIT_FADE_DURATION,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    ))
                                }
                            }
                        },
                        label = "main_tab_transition",
                    ) { targetScreenState ->
                        when (targetScreenState) {
                            is ScreenState.MainTab -> MainTabPager(
                                pagerState = pagerState,
                                navController = navController,
                                onSwitchTab = { page ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page)
                                    }
                                },
                            )
                            is ScreenState.SubPage -> content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabPager(
    pagerState: PagerState,
    navController: NavHostController,
    onSwitchTab: (Int) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false },
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onNavigateToEditor = { fileId ->
                    navController.navigate(Route.Editor.createRoute(fileId))
                },
                onNavigateToNewFile = {
                    navController.navigate(Route.NewFile.route)
                },
                onNavigateToFiles = { onSwitchTab(1) },
                onNavigateToHistory = { onSwitchTab(2) },
            )

            1 -> FilesScreen(
                onNavigateToEditor = { fileId ->
                    navController.navigate(Route.Editor.createRoute(fileId))
                },
                onNavigateToNewFile = {
                    navController.navigate(Route.NewFile.route)
                },
            )

            2 -> HistoryScreen(
                onNavigateToEditor = { fileId ->
                    navController.navigate(Route.Editor.createRoute(fileId))
                },
                onNavigateToVersionHistory = { fileId ->
                    navController.navigate(Route.VersionHistory.createRoute(fileId, ""))
                },
            )

            3 -> SettingsScreen(
                onNavigateToAppearance = {
                    navController.navigate(Route.AppearanceSettings.route)
                },
                onNavigateToEditorSettings = {
                    navController.navigate(Route.EditorSettings.route)
                },
                onNavigateToStorage = {
                    navController.navigate(Route.StorageSettings.route)
                },
                onNavigateToCloudSync = {
                    navController.navigate(Route.CloudSyncSettings.createRoute())
                },
                onNavigateToImageHost = {
                    navController.navigate(Route.ImageHostSettings.route)
                },
                onNavigateToAdvanced = {
                    navController.navigate(Route.AdvancedSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Route.About.route)
                },
            )
        }
    }
}
