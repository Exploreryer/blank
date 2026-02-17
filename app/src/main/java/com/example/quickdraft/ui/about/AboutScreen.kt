package com.example.quickdraft.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(appVersion: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("关于", style = MaterialTheme.typography.headlineMedium)
        Text("Blank 是一款坚持极简主义的 Android 灵感速记工具。")
        Text("设计理念：克制、留白、输入即主角。")
        Text("版本：$appVersion")
        Text("开源协议：Apache-2.0")
        Text("鸣谢：Kotlin、Jetpack Compose、Material 3、Room、WorkManager。")
    }
}
