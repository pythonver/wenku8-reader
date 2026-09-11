package com.wenku8.reader.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wenku8.reader.core.data.model.ReleaseInfo
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("发现新版本 ${release.tag}") },
        text = {
            Column {
                Text(
                    release.notes.ifBlank { "暂无更新说明" },
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLater) { Text("稍后再说") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onUpdate) {
                    Text("立即更新", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("跳过此版本") }
        },
    )
}

@Composable
fun DownloadProgressDialog(
    phase: UpdatePhase.Downloading,
    onCancel: () -> Unit,
) {
    val percent = if (phase.total > 0) (phase.progress * 100).toInt() else null
    AlertDialog(
        onDismissRequest = { /* block dismissal while downloading */ },
        title = { Text("正在下载 ${phase.release.tag}") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (phase.total > 0) {
                    LinearProgressIndicator(
                        progress = { phase.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append(formatBytes(phase.downloaded))
                        if (phase.total > 0) {
                            append(" / ${formatBytes(phase.total)}")
                            if (percent != null) append("（$percent%）")
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text("取消下载") }
        },
    )
}

@Composable
fun InstallPermissionDialog(
    onSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("需要安装权限") },
        text = {
            Text("安装更新需要允许本应用安装未知来源应用。点击「去设置」开启后将自动继续安装，安装完成后可随时在系统设置中关闭。")
        },
        confirmButton = { TextButton(onClick = onSettings) { Text("去设置") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } },
    )
}

@Composable
fun UpdateErrorDialog(
    message: String,
    retry: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新失败") },
        text = { Text(message) },
        confirmButton = {
            if (retry != null) TextButton(onClick = retry) { Text("重试") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroup)
    return String.format("%.1f %s", value, units[digitGroup])
}
