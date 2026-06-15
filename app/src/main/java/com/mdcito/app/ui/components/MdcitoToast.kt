package com.mdcito.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

data class ToastState(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3000L,
)

@Composable
fun MdcitoToastHost(
    toastState: ToastState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(toastState) {
        if (toastState != null) {
            visible = true
            delay(toastState.durationMs)
            visible = false
            delay(300)
            onDismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            toastState?.let { toast ->
                MdcitoToastContent(toast)
            }
        }
    }
}

@Composable
private fun MdcitoToastContent(toast: ToastState) {
    val (icon, bgColor, contentColor) = when (toast.type) {
        ToastType.SUCCESS -> Triple(
            Icons.Outlined.CheckCircle,
            Color(0xFF2E7D32),
            Color.White,
        )
        ToastType.ERROR -> Triple(
            Icons.Outlined.Error,
            Color(0xFFC62828),
            Color.White,
        )
        ToastType.WARNING -> Triple(
            Icons.Outlined.Warning,
            Color(0xFFF57F17),
            Color.White,
        )
        ToastType.INFO -> Triple(
            Icons.Outlined.Info,
            Color(0xFF1565C0),
            Color.White,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 60.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = toast.message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W500,
        )
    }
}
