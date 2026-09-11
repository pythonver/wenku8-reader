package com.wenku8.reader.feature.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.core.data.ReadingProgressEntity
import com.wenku8.reader.core.designsystem.components.NovelCoverImage
import com.wenku8.reader.core.designsystem.components.StaggeredItem
import com.wenku8.reader.core.designsystem.components.SwipeRevealAction
import com.wenku8.reader.core.designsystem.theme.Spacing
import com.wenku8.reader.core.util.AppVersion
import com.wenku8.reader.ui.AppBottomBar

@Composable
fun HomeScreen(
    currentRoute: String?,
    onNavigateTab: (String) -> Unit,
    onSearchClick: () -> Unit,
    onOpenDetail: (aid: Int) -> Unit,
    onOpenDevLog: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recentReads by viewModel.recentReads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<ReadingProgressEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomBar(currentRoute, onNavigateTab) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page),
        ) {
            // Google-style hero: brand + search entry
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = "轻小说文库",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onOpenDevLog() })
                    },
            )
            Text(
                text = "v${AppVersion.installed(context)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(Spacing.lg))
            SearchBarHero(onClick = onSearchClick)
            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "最近阅读",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))

            if (recentReads.isEmpty()) {
                EmptyRecentHint()
            } else {
                recentReads.forEachIndexed { index, progress ->
                    StaggeredItem(index = index) {
                        SwipeRevealAction(
                            actionIcon = Icons.Outlined.Delete,
                            actionContentDescription = "删除阅读记录",
                            onAction = { pendingDelete = progress },
                            onContentClick = { onOpenDetail(progress.aid) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xxs),
                        ) {
                            RecentReadCard(progress = progress)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    pendingDelete?.let { progress ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除阅读记录") },
            text = { Text("确定删除《${progress.title.ifEmpty { "小说 #${progress.aid}" }}》的最近阅读记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeRecent(progress.aid)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SearchBarHero(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "搜索小说 / 作者",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentReadCard(progress: ReadingProgressEntity) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.sm),
        ) {
            NovelCoverImage(
                aid = progress.aid,
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = progress.title.ifEmpty { "小说 #${progress.aid}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "继续阅读",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyRecentHint() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "还没有阅读记录。搜索一本书，开始你的第一次阅读吧。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
