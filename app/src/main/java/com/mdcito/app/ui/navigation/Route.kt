package com.mdcito.app.ui.navigation

sealed class Route(val route: String) {

    data object Home : Route("home")

    data object Files : Route("files")

    data object History : Route("history")

    data object Settings : Route("settings")

    data object Editor : Route("editor/{fileId}") {
        fun createRoute(fileId: Long): String = "editor/$fileId"
    }

    data object NewFile : Route("editor/new")

    data object Folder : Route("folder/{folderId}") {
        fun createRoute(folderId: Long): String = "folder/$folderId"
    }

    data object AppearanceSettings : Route("settings/appearance")

    data object ThemeSettings : Route("settings/appearance/theme")

    data object FontSettings : Route("settings/appearance/font")

    data object BackgroundSettings : Route("settings/appearance/background")

    data object CardStyleSettings : Route("settings/appearance/card_style")

    data object EditorSettings : Route("settings/editor")

    data object StorageSettings : Route("settings/storage")

    data object CloudSyncSettings : Route("settings/cloud_sync?code={code}&state={state}") {
        fun createRoute(code: String? = null, state: String? = null): String {
            val params = mutableListOf<String>()
            if (code != null) params.add("code=$code")
            if (state != null) params.add("state=$state")
            return if (params.isEmpty()) "settings/cloud_sync"
            else "settings/cloud_sync?${params.joinToString("&")}"
        }
    }

    data object ImageHostSettings : Route("settings/image_host")

    data object AdvancedSettings : Route("settings/advanced")

    data object PerformanceSettings : Route("settings/advanced/performance")

    data object LogSettings : Route("settings/advanced/log")

    data object About : Route("settings/about")

    data object OpenSource : Route("settings/about/open_source")

    data object Changelog : Route("settings/about/changelog")

    data object VersionHistory : Route("version_history/{fileId}/{fileName}") {
        fun createRoute(fileId: Long, fileName: String): String =
            "version_history/$fileId/${java.net.URLEncoder.encode(fileName, "UTF-8")}"
    }

    data object Onboarding : Route("onboarding")
}

val bottomNavRoutes = listOf(
    Route.Home,
    Route.Files,
    Route.History,
    Route.Settings,
)
