package com.example.quickdraft.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    isSyncEnabled: Boolean,
    isSignedIn: Boolean,
    signedInEmail: String?,
    onSyncToggle: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    appVersion: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Text("Google Drive 授权状态：${if (isSignedIn) "已登录" else "未登录"}")
        if (!signedInEmail.isNullOrBlank()) {
            Text("账号：$signedInEmail")
        }
        Button(onClick = if (isSignedIn) onSignOut else onSignIn) {
            Text(if (isSignedIn) "退出 Google 账号" else "登录 Google 账号")
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("同步开关")
            Switch(checked = isSyncEnabled, onCheckedChange = onSyncToggle)
        }

        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Blank", style = MaterialTheme.typography.titleMedium)
            Text("一款极简、专注输入的灵感速记工具。", style = MaterialTheme.typography.bodyMedium)
            Text("版本 $appVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("开源协议 Apache-2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
