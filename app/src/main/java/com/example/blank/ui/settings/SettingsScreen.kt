package com.example.blank.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.blank.R

@Composable
fun SettingsScreen(
    isSyncEnabled: Boolean,
    isProEnabled: Boolean,
    isSignedIn: Boolean,
    signedInEmail: String?,
    onSyncToggle: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onPremiumClick: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyanAccent = Color(0xFF2AA79B)
    val canToggleSync = isProEnabled && isSignedIn
    val statusText = if (!isProEnabled) "需 Pro" else if (isSignedIn) "已连接" else "未连接"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PremiumBanner(
                isProEnabled = isProEnabled,
                onClick = onPremiumClick,
                accent = cyanAccent
            )
        }

        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, tint = Color(0xFF344054))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("同步", style = MaterialTheme.typography.titleMedium)
                            Text(statusText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                        }
                    }
                    Button(
                        onClick = if (isSignedIn) onSignOut else onSignIn,
                        enabled = isProEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667085))
                    ) {
                        Text(if (isSignedIn) "退出" else "登录")
                    }
                }
                if (!signedInEmail.isNullOrBlank()) {
                    Text(signedInEmail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                }
                HorizontalDivider(color = Color(0xFFE4E7EC))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("同步", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isSyncEnabled && canToggleSync,
                        onCheckedChange = onSyncToggle,
                        enabled = canToggleSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF667085),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD0D5DD)
                        )
                    )
                }
            }
        }

        item {
            SettingsCard {
                Text("数据", style = MaterialTheme.typography.titleMedium)
                ActionRow(
                    icon = { Icon(Icons.Outlined.Upload, contentDescription = null, tint = Color(0xFF475467)) },
                    label = "导出数据",
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = Color(0xFFE4E7EC))
                ActionRow(
                    icon = { Icon(Icons.Outlined.Download, contentDescription = null, tint = Color(0xFF475467)) },
                    label = "导入数据",
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = Color(0xFFE4E7EC))
                ActionRow(
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF475467)) },
                    label = "关于 Blank",
                    onClick = onOpenAbout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_blank_mark),
                    contentDescription = "Blank Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = buildAnnotatedString {
                        append("Made with ")
                        withStyle(SpanStyle(color = cyanAccent, fontWeight = FontWeight.SemiBold)) { append("❤") }
                        append(" from Blank")
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "为你留住每一次灵感",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF667085)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE4E7EC), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF98A2B3))
        }
    }
}

@Composable
private fun PremiumBanner(
    isProEnabled: Boolean,
    onClick: () -> Unit,
    accent: Color
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF6FBFB),
                            Color(0xFFE9F6F4),
                            Color(0xFFDDF0ED)
                        )
                    )
                )
                .border(1.dp, Color(0xCCFFFFFF), shape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x33FFFFFF), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = 190.dp, y = (-48).dp)
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .offset(x = (-26).dp, y = 52.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Blank ")
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append("Pro") }
                    },
                    color = Color(0xFF1D2939),
                    style = MaterialTheme.typography.titleLarge
                )
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProEnabled) accent else Color(0xFF667085),
                        contentColor = Color(0xFFF8FAFC)
                    )
                ) {
                    Text(if (isProEnabled) "已开通" else "开通")
                }
            }
        }
    }
}
