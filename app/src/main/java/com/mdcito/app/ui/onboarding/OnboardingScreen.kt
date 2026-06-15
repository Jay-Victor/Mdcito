package com.mdcito.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mdcito.app.R
import com.mdcito.app.ui.theme.MdcitoColors
import timber.log.Timber

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val accentColor: androidx.compose.ui.graphics.Color,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Edit,
        titleRes = R.string.focus_writing,
        descriptionRes = R.string.focus_writing_desc,
        accentColor = MdcitoColors.ThemeColors.all[0],
    ),
    OnboardingPage(
        icon = Icons.Outlined.Palette,
        titleRes = R.string.japanese_aesthetics,
        descriptionRes = R.string.japanese_aesthetics_desc,
        accentColor = MdcitoColors.ThemeColors.all[1],
    ),
    OnboardingPage(
        icon = Icons.Outlined.CreateNewFolder,
        titleRes = R.string.secure_private,
        descriptionRes = R.string.secure_private_desc,
        accentColor = MdcitoColors.ThemeColors.all[2],
    ),
    OnboardingPage(
        icon = Icons.Outlined.FolderOpen,
        titleRes = R.string.file_permission_title,
        descriptionRes = R.string.file_permission_desc,
        accentColor = MdcitoColors.ThemeColors.all[3.coerceIn(0, MdcitoColors.ThemeColors.all.lastIndex)],
    ),
)

fun hasAllFilesAccess(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}

fun requestAllFilesAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    startAtPermissionPage: Boolean = false,
) {
    var currentPage by remember { mutableStateOf(if (startAtPermissionPage) pages.size - 1 else 0) }
    val context = LocalContext.current
    val isPermissionPage = currentPage == pages.size - 1

    var permissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Environment.isExternalStorageManager()
            else true
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val page = pages[currentPage]

    LaunchedEffect(currentPage) {
        Timber.tag("App").d("引导页切换：第${currentPage + 1}页")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(page.accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.accentColor,
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W700,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 1.6.em,
        )

        if (isPermissionPage) {
            Spacer(modifier = Modifier.height(24.dp))

            if (permissionGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.permission_granted),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.W600,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                Button(
                    onClick = { requestAllFilesAccess(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.go_authorize))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.return_after_authorize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            pages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 24.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (currentPage > 0) {
                OutlinedButton(onClick = { currentPage-- }) {
                    Text(stringResource(R.string.prev_step))
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (isPermissionPage && !permissionGranted) {
                Button(
                    onClick = { requestAllFilesAccess(context) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.go_authorize))
                }
            } else {
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            if (hasAllFilesAccess(context)) {
                                Timber.tag("App").i("引导页完成")
                                onComplete()
                            }
                        }
                    },
                ) {
                    Text(if (currentPage < pages.size - 1) stringResource(R.string.next_step) else stringResource(R.string.start_using))
                }
            }
        }

        if (isPermissionPage && !permissionGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.must_authorize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.permission_denied_guide),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
