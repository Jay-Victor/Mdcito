package com.mdcito.app.ui.theme

import androidx.compose.ui.graphics.Color

object MdcitoColors {

    object Light {
        val primary = Color(0xFF7C9A8E)
        val primaryLight = Color(0xFFA3BFB4)
        val primaryDark = Color(0xFF5A7A6E)
        val primarySurface = Color(0x147C9A8E)
        val background = Color(0xFFF5F3EF)
        val surface = Color(0xFFFFFFFF)
        val surfaceContainerHigh = Color(0xFFF0EDE8)
        val onBackground = Color(0xFF3A3632)
        val onSurface = Color(0xFF3A3632)
        val onSurfaceVariant = Color(0xFF8A847C)
        val outlineVariant = Color(0xFFB0A99F)
        val outline = Color(0xFFE5E0D8)
        val error = Color(0xFFC8796E)
        val warning = Color(0xFFC8B47E)
        val accent = Color(0xFF8B7EC8)
        val success = Color(0xFF6B9E7E)
        val info = Color(0xFF6B8E9E)
        val cardShadow = Color(0x0F3A3632)
        val elevatedShadow = Color(0x1F3A3632)
        val scrim = Color(0x663A3632)
    }

    object Dark {
        val primary = Color(0xFF8BB5A6)
        val background = Color(0xFF1A1816)
        val surface = Color(0xFF252220)
        val surfaceContainerHigh = Color(0xFF2E2B28)
        val onBackground = Color(0xFFE5E0D8)
        val onSurface = Color(0xFFE5E0D8)
        val onSurfaceVariant = Color(0xFF9A948C)
        val outlineVariant = Color(0xFF3A3632)
        val outline = Color(0xFF3A3632)
        val error = Color(0xFFE88A80)
        val warning = Color(0xFFD4C080)
        val success = Color(0xFF80B890)
        val info = Color(0xFF80A8B8)
        val cardShadow = Color(0x33000000)
        val scrim = Color(0x8C000000)
    }

    object ThemeColors {
        val green = Color(0xFF7C9A8E)
        val orange = Color(0xFFD4A574)
        val amber = Color(0xFFC8B47E)
        val yellowGreen = Color(0xFFA4B87E)
        val teal = Color(0xFF7EB8A4)
        val cyan = Color(0xFF7EA4B8)
        val blue = Color(0xFF7E8EB8)
        val indigo = Color(0xFF8E7EB8)
        val purple = Color(0xFFA47EB8)
        val magenta = Color(0xFFB87EA4)
        val rose = Color(0xFFB87E8E)
        val red = Color(0xFFB87E7E)

        val all = listOf(green, orange, amber, yellowGreen, teal, cyan, blue, indigo, purple, magenta, rose, red)
    }

    object Scheme {
        object Light {
            val warm = SchemeColors(Color(0xFFF5F3EF), Color(0xFFFFFFFF), Color(0xFFF0EDE8), Color(0xFFE5E0D8))
            val cool = SchemeColors(Color(0xFFF0F2F5), Color(0xFFFFFFFF), Color(0xFFE8ECF0), Color(0xFFDFE3E8))
            val paper = SchemeColors(Color(0xFFF8F5E9), Color(0xFFFEFDF8), Color(0xFFF0ECE0), Color(0xFFE0DBD0))
            val fresh = SchemeColors(Color(0xFFF0F5F2), Color(0xFFFFFFFF), Color(0xFFE8F0EC), Color(0xFFD8E5DE))
        }

        object Dark {
            val warmDark = SchemeColors(Color(0xFF1A1816), Color(0xFF252220), Color(0xFF2E2B28), Color(0xFF3A3632))
            val coolDark = SchemeColors(Color(0xFF16181A), Color(0xFF1E2022), Color(0xFF26282A), Color(0xFF36383A))
            val oled = SchemeColors(Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF141414), Color(0xFF1E1E1E))
            val midnight = SchemeColors(Color(0xFF0A1628), Color(0xFF142038), Color(0xFF1E2A48), Color(0xFF283A58))
        }
    }

    data class SchemeColors(
        val background: Color,
        val surface: Color,
        val surfaceContainerHigh: Color,
        val outline: Color,
        val onBackground: Color? = null,
        val onSurface: Color? = null,
        val onSurfaceVariant: Color? = null,
        val outlineVariant: Color? = null,
    )
}
