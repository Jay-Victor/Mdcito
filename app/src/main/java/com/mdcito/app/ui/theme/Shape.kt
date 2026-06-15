package com.mdcito.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MdcitoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object MdcitoCorner {
    val tag = 4.dp
    val inputField = 8.dp
    val iconButton = 10.dp
    val card = 12.dp
    val listContainer = 12.dp
    val pill = 16.dp
    val searchBar = 28.dp
    val settingsGroup = 14.dp
    val textStyleCard = 24.dp
}
