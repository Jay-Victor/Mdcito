package com.mdcito.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mdcito.app.ui.editor.EditorScreen
import com.mdcito.app.ui.editor.VersionHistoryScreen
import com.mdcito.app.ui.files.FilesScreen
import com.mdcito.app.ui.onboarding.OnboardingScreen
import com.mdcito.app.ui.settings.AboutScreen
import com.mdcito.app.ui.settings.AdvancedSettingsScreen
import com.mdcito.app.ui.settings.AppearanceSettingsScreen
import com.mdcito.app.ui.settings.BackgroundSettingsScreen
import com.mdcito.app.ui.settings.CardStyleSettingsScreen
import com.mdcito.app.ui.settings.EditorSettingsScreen
import com.mdcito.app.ui.settings.FontSettingsScreen
import com.mdcito.app.ui.settings.ImageHostSettingsScreen
import com.mdcito.app.ui.settings.LogSettingsScreen
import com.mdcito.app.ui.settings.OpenSourceScreen
import com.mdcito.app.ui.settings.PerformanceSettingsScreen
import com.mdcito.app.ui.settings.StorageSettingsScreen
import com.mdcito.app.ui.settings.ThemeSettingsScreen
import com.mdcito.app.ui.settings.TransitionSettingsScreen
import com.mdcito.app.ui.settings.changelog.ChangelogScreen
import com.mdcito.app.ui.settings.cloudsync.CloudSyncSettingsScreen
import timber.log.Timber

private val mainTabRouteSet = bottomNavRoutes.map { it.route }.toSet()

private fun isMainTab(route: String?) = route in mainTabRouteSet

// 设置页面路由集合（主设置页 + 所有子页面）
private val settingsRouteSet = setOf(
    Route.Settings.route,
    Route.AppearanceSettings.route,
    Route.ThemeSettings.route,
    Route.FontSettings.route,
    Route.BackgroundSettings.route,
    Route.CardStyleSettings.route,
    Route.TransitionSettings.route,
    Route.EditorSettings.route,
    Route.StorageSettings.route,
    Route.CloudSyncSettings.route,
    Route.ImageHostSettings.route,
    Route.AdvancedSettings.route,
    Route.PerformanceSettings.route,
    Route.LogSettings.route,
    Route.About.route,
    Route.OpenSource.route,
    Route.Changelog.route,
)

private fun isSettingsRoute(route: String?) = route in settingsRouteSet

// ── Fade Through 动画（用于编辑器等普通页面） ──
private val fadeThroughEnter = fadeIn(
    animationSpec = tween(
        durationMillis = 210,
        delayMillis = 90,
        easing = LinearOutSlowInEasing,
    ),
) + scaleIn(
    initialScale = 0.92f,
    animationSpec = tween(
        durationMillis = 210,
        delayMillis = 90,
        easing = LinearOutSlowInEasing,
    ),
)

private val fadeThroughExit = fadeOut(
    animationSpec = tween(
        durationMillis = 90,
        easing = FastOutLinearInEasing,
    ),
)

// ── SharedXAxis 动画（Material Design 模式，用于设置页面层级导航） ──
// 进入设置子页面：从右侧滑入 + 淡入
private val settingsEnterTransition = slideInHorizontally(
    initialOffsetX = { it / 3 },
    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
) + fadeIn(
    animationSpec = tween(durationMillis = 210, delayMillis = 90, easing = LinearOutSlowInEasing),
)

// 当前页退出（进入设置子页面时）：向左滑出 + 淡出
private val settingsExitTransition = slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
) + fadeOut(
    animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing),
)

// 返回时前页重新进入：从左侧滑入 + 淡入
private val settingsPopEnterTransition = slideInHorizontally(
    initialOffsetX = { -it / 3 },
    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
) + fadeIn(
    animationSpec = tween(durationMillis = 210, delayMillis = 90, easing = LinearOutSlowInEasing),
)

// 返回时当前页退出：向右滑出 + 淡出
private val settingsPopExitTransition = slideOutHorizontally(
    targetOffsetX = { it / 3 },
    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
) + fadeOut(
    animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing),
)

@Composable
fun MdcitoNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navigateBack: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier,
        enterTransition = {
            when {
                isMainTab(initialState.destination.route) || isMainTab(targetState.destination.route) -> EnterTransition.None
                isSettingsRoute(targetState.destination.route) -> settingsEnterTransition
                else -> fadeThroughEnter
            }
        },
        exitTransition = {
            when {
                isMainTab(initialState.destination.route) || isMainTab(targetState.destination.route) -> ExitTransition.None
                isSettingsRoute(targetState.destination.route) -> settingsExitTransition
                else -> fadeThroughExit
            }
        },
        popEnterTransition = {
            when {
                isMainTab(initialState.destination.route) || isMainTab(targetState.destination.route) -> EnterTransition.None
                isSettingsRoute(initialState.destination.route) -> settingsPopEnterTransition
                else -> fadeThroughEnter
            }
        },
        popExitTransition = {
            when {
                isMainTab(initialState.destination.route) || isMainTab(targetState.destination.route) -> ExitTransition.None
                isSettingsRoute(initialState.destination.route) -> settingsPopExitTransition
                else -> fadeThroughExit
            }
        },
    ) {
        composable(Route.Home.route) { }
        composable(Route.Files.route) { }
        composable(Route.History.route) { }
        composable(Route.Settings.route) { }

        composable(
            route = Route.Editor.route,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType }),
        ) {
            EditorScreen(
                onNavigateBack = navigateBack,
                onNavigateToVersionHistory = { fileId, fileName ->
                    Timber.tag("Navigation").d("导航到版本历史：$fileId")
                    navController.navigate(Route.VersionHistory.createRoute(fileId, fileName))
                },
                onNavigateToSettings = {
                    Timber.tag("Navigation").d("导航到编辑器设置")
                    navController.navigate(Route.EditorSettings.route)
                },
            )
        }

        composable(Route.NewFile.route) {
            EditorScreen(
                onNavigateBack = navigateBack,
                onNavigateToSettings = {
                    Timber.tag("Navigation").d("导航到编辑器设置")
                    navController.navigate(Route.EditorSettings.route)
                },
            )
        }

        composable(
            route = Route.Folder.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) {
            FilesScreen(
                onNavigateToEditor = { fileId ->
                    Timber.tag("Navigation").d("导航到编辑器：$fileId")
                    navController.navigate(Route.Editor.createRoute(fileId))
                },
                onNavigateToNewFile = {
                    Timber.tag("Navigation").d("导航到新建文件")
                    navController.navigate(Route.NewFile.route)
                },
            )
        }

        composable(Route.AppearanceSettings.route) {
            AppearanceSettingsScreen(
                onNavigateBack = navigateBack,
                onNavigateToTheme = {
                    Timber.tag("Navigation").d("导航到主题设置")
                    navController.navigate(Route.ThemeSettings.route)
                },
                onNavigateToFont = {
                    Timber.tag("Navigation").d("导航到字体设置")
                    navController.navigate(Route.FontSettings.route)
                },
                onNavigateToBackground = {
                    Timber.tag("Navigation").d("导航到背景设置")
                    navController.navigate(Route.BackgroundSettings.route)
                },
                onNavigateToCardStyle = {
                    Timber.tag("Navigation").d("导航到卡片样式设置")
                    navController.navigate(Route.CardStyleSettings.route)
                },
                onNavigateToTransition = {
                    Timber.tag("Navigation").d("导航到过渡动画设置")
                    navController.navigate(Route.TransitionSettings.route)
                },
            )
        }

        composable(Route.ThemeSettings.route) {
            ThemeSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.FontSettings.route) {
            FontSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.BackgroundSettings.route) {
            BackgroundSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.CardStyleSettings.route) {
            CardStyleSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.TransitionSettings.route) {
            TransitionSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.EditorSettings.route) {
            EditorSettingsScreen(
                onNavigateBack = navigateBack,
            )
        }

        composable(Route.StorageSettings.route) {
            StorageSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(
            route = Route.CloudSyncSettings.route,
            arguments = listOf(navArgument("code") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }, navArgument("state") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { backStackEntry ->
            val oauthCode = backStackEntry.arguments?.getString("code")
            val oauthState = backStackEntry.arguments?.getString("state")
            CloudSyncSettingsScreen(
                onNavigateBack = navigateBack,
                oauthCode = oauthCode,
                oauthState = oauthState,
            )
        }

        composable(Route.ImageHostSettings.route) {
            ImageHostSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.AdvancedSettings.route) {
            AdvancedSettingsScreen(
                onNavigateBack = navigateBack,
                onNavigateToPerformance = {
                    Timber.tag("Navigation").d("导航到性能设置")
                    navController.navigate(Route.PerformanceSettings.route)
                },
                onNavigateToLog = {
                    Timber.tag("Navigation").d("导航到日志设置")
                    navController.navigate(Route.LogSettings.route)
                },
            )
        }

        composable(Route.PerformanceSettings.route) {
            PerformanceSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.LogSettings.route) {
            LogSettingsScreen(onNavigateBack = navigateBack)
        }

        composable(Route.About.route) {
            AboutScreen(
                onNavigateBack = navigateBack,
                onNavigateToOpenSource = {
                    navController.navigate(Route.OpenSource.route)
                },
                onNavigateToChangelog = {
                    navController.navigate(Route.Changelog.route)
                },
            )
        }

        composable(Route.OpenSource.route) {
            OpenSourceScreen(onNavigateBack = navigateBack)
        }

        composable(Route.Changelog.route) {
            ChangelogScreen(onNavigateBack = navigateBack)
        }

        composable(Route.Onboarding.route) {
            OnboardingScreen(
                onComplete = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.VersionHistory.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.LongType },
                navArgument("fileName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getLong("fileId") ?: 0L
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val editorBackStackEntry = runCatching {
                navController.getBackStackEntry(Route.Editor.createRoute(fileId))
            }.getOrNull()
            val editorViewModel: com.mdcito.app.ui.editor.EditorViewModel? = if (editorBackStackEntry != null) {
                androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = editorBackStackEntry)
            } else null
            VersionHistoryScreen(
                fileId = fileId,
                fileName = java.net.URLDecoder.decode(fileName, "UTF-8"),
                onNavigateBack = navigateBack,
                onRestoreVersion = { content ->
                    editorViewModel?.requestRestore(content)
                    navigateBack()
                },
            )
        }
    }
}
