package com.mdcito.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.mdcito.app.R

@Composable
fun MdcitoCenterModal(
    onDismissRequest: () -> Unit,
    visible: Boolean,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.W600,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    content()
                }
            }
        }
    }
}

@Composable
fun MdcitoBottomSheet(
    onDismissRequest: () -> Unit,
    visible: Boolean,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.W600,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun MdcitoConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String? = null,
    cancelText: String? = null,
    isDangerous: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedConfirmText = confirmText ?: stringResource(R.string.confirm)
    val resolvedCancelText = cancelText ?: stringResource(R.string.cancel)
    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = title,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text(resolvedCancelText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                colors = if (isDangerous) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    )
                },
            ) {
                Text(resolvedConfirmText)
            }
        }
    }
}

@Composable
fun MdcitoRenameDialog(
    visible: Boolean,
    currentName: String,
    existingNames: List<String> = emptyList(),
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val newNameState = remember { mutableStateOf(currentName) }
    val isErrorState = remember { mutableStateOf(false) }
    val errorTextState = remember { mutableStateOf<String?>(null) }
    val newName = newNameState.value
    val context = LocalContext.current

    LaunchedEffect(visible, currentName) {
        if (visible) {
            newNameState.value = currentName
            isErrorState.value = false
            errorTextState.value = null
        }
    }

    fun validate(name: String) {
        when {
            name.isEmpty() -> {
                isErrorState.value = true
                errorTextState.value = context.getString(R.string.error_name_empty)
            }
            name.contains(Regex("[/\\\\:*?\"<>|]")) -> {
                isErrorState.value = true
                errorTextState.value = context.getString(R.string.error_name_invalid)
            }
            name.startsWith('.') -> {
                isErrorState.value = true
                errorTextState.value = context.getString(R.string.error_name_dot)
            }
            name.length > 255 -> {
                isErrorState.value = true
                errorTextState.value = context.getString(R.string.error_name_too_long)
            }
            name in existingNames && name != currentName -> {
                isErrorState.value = true
                errorTextState.value = context.getString(R.string.error_name_exists)
            }
            else -> {
                isErrorState.value = false
                errorTextState.value = null
            }
        }
    }

    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.rename_title),
    ) {
        MdcitoTextField(
            value = newName,
            onValueChange = {
                newNameState.value = it
                validate(it)
            },
            placeholder = stringResource(R.string.rename_placeholder),
            isError = isErrorState.value,
            errorText = errorTextState.value,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    validate(newName)
                    if (!isErrorState.value && newName.isNotEmpty()) onRename(newName)
                },
                enabled = !isErrorState.value && newName.isNotEmpty(),
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}

@Composable
fun MdcitoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isError && errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun MdcitoActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}
