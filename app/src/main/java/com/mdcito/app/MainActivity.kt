package com.mdcito.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mdcito.app.data.font.FontService
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.navigation.Route
import com.mdcito.app.data.repository.SettingsRepository
import com.mdcito.app.ui.components.MdcitoScaffold
import com.mdcito.app.ui.components.SplashAnimation
import com.mdcito.app.ui.navigation.MdcitoNavGraph
import com.mdcito.app.ui.onboarding.OnboardingScreen
import com.mdcito.app.ui.onboarding.hasAllFilesAccess
import com.mdcito.app.ui.theme.MdcitoTheme
import com.mdcito.app.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var fontService: FontService

    private var pendingDeepLink: String? = null
    private var localeVersion = mutableIntStateOf(0)
    private var uiModeVersion = mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        LanguageHelper.init(newBase)
        super.attachBaseContext(LanguageHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        LanguageHelper.onLocaleChanged = {
            localeVersion.intValue++
        }

        handleDeepLink(intent)

        // 是否为 Activity 重建（如语言切换导致的 recreate），重建时跳过开场动画
        val isRecreating = savedInstanceState != null

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "SYSTEM")
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = false)
            val themeColorIndex by settingsRepository.themeColorIndex.collectAsState(initial = 0)
            val lightScheme by settingsRepository.lightScheme.collectAsState(initial = "warm")
            val darkScheme by settingsRepository.darkScheme.collectAsState(initial = "warm_dark")
            val uiFont by settingsRepository.uiFont.collectAsState(initial = "system")
            val uiFontSize by settingsRepository.uiFontSize.collectAsState(initial = 14)
            val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = false)

            val currentThemeMode = when (themeMode) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            val uiFontFamily = remember(uiFont) { fontService.getComposeFontFamily(uiFont) }

            // 开场动画状态：首次启动时显示，Activity 重建（如语言切换）时跳过
            // 使用 null 表示"等待设置加载"，加载完成后根据设置决定是否显示
            var showSplash by remember { mutableStateOf<Boolean?>(if (isRecreating) false else null) }

            // 追踪是否已收到 DataStore 的真实值（而非 collectAsState 的初始值）
            var splashSettingLoaded by remember { mutableStateOf(false) }

            // 当 splashAnimationEnabled 从 DataStore 加载完成后，决定是否显示开场动画
            // 使用 snapshotFlow 确保只在真实值到达时触发，避免 collectAsState 初始值干扰
            androidx.compose.runtime.LaunchedEffect(Unit) {
                settingsRepository.splashAnimationEnabled.collect { enabled ->
                    if (!splashSettingLoaded) {
                        splashSettingLoaded = true
                        if (showSplash == null) {
                            showSplash = enabled
                        }
                    }
                }
            }

            // 检测系统级「减少动画」无障碍设置
            val context = LocalContext.current
            val accessibilityManager = remember {
                context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            }
            val reduceMotion = accessibilityManager?.isTouchExplorationEnabled == true ||
                    accessibilityManager?.isEnabled == true && android.provider.Settings.Global.getInt(
                        context.contentResolver,
                        android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                        1,
                    ) == 0

            MdcitoTheme(
                themeMode = currentThemeMode,
                dynamicColor = dynamicColor,
                themeColorIndex = themeColorIndex,
                lightScheme = lightScheme,
                darkScheme = darkScheme,
                uiFontFamily = uiFontFamily,
                uiFontSize = uiFontSize,
                uiModeVersion = uiModeVersion.intValue,
            ) {
                // 通过 CompositionLocal 向下传播语言版本号，
                // 任何读取 LocalLocaleVersion.current 的 Composable 都会在语言切换时被强制重组
                CompositionLocalProvider(LanguageHelper.LocalLocaleVersion provides localeVersion.intValue) {
                    when (showSplash) {
                        true -> {
                            val darkTheme = when (currentThemeMode) {
                                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                                ThemeMode.LIGHT -> false
                                ThemeMode.DARK -> true
                            }
                            SplashAnimation(
                                isDarkTheme = darkTheme,
                                reduceMotion = reduceMotion,
                                onAnimationFinish = { showSplash = false },
                            )
                        }
                        false -> {
                            // 使用 Compose state 跟踪权限状态，在生命周期变化时自动更新
                            var hasPermission by remember { mutableStateOf(hasAllFilesAccess(this@MainActivity)) }
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        hasPermission = hasAllFilesAccess(this@MainActivity)
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }

                            // 未完成引导 或 权限未授予时，显示引导页
                            if (!onboardingCompleted || !hasPermission) {
                                val scope = rememberCoroutineScope()
                                OnboardingScreen(
                                    onComplete = {
                                        scope.launch {
                                            settingsRepository.setOnboardingCompleted(true)
                                        }
                                    },
                                    startAtPermissionPage = onboardingCompleted && !hasPermission,
                                )
                            } else {
                                val navController = rememberNavController()
                                MdcitoScaffold(navController = navController) {
                                    MdcitoNavGraph(navController = navController)
                                }
                                androidx.compose.runtime.LaunchedEffect(pendingDeepLink) {
                                    pendingDeepLink?.let { route ->
                                        navController.navigate(route)
                                        pendingDeepLink = null
                                    }
                                }
                            }
                        }
                        null -> {
                            // 等待设置加载完成，显示空白或加载状态
                            // 实际上 LaunchedEffect 会很快执行，用户几乎看不到这个状态
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode 变化时递增版本号，触发 Compose 重组以更新深色/浅色主题
        uiModeVersion.intValue++
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        when (uri.host) {
            "file" -> {
                val fileId = uri.lastPathSegment?.toLongOrNull()
                if (fileId != null) {
                    pendingDeepLink = "editor/$fileId"
                }
            }
            "new" -> {
                pendingDeepLink = "editor/new"
            }
            "open" -> {
                val fileId = uri.getQueryParameter("id")?.toLongOrNull()
                if (fileId != null) {
                    pendingDeepLink = "editor/$fileId"
                }
            }
            "cloud-sync" -> {
                val code = uri.getQueryParameter("code")
                val state = uri.getQueryParameter("state")
                if (code != null) {
                    pendingDeepLink = Route.CloudSyncSettings.createRoute(code, state)
                }
            }
        }
    }
}
