package com.wenku8.reader.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.core.data.FavoriteNovelEntity
import com.wenku8.reader.core.designsystem.components.NovelCoverImage
import com.wenku8.reader.core.designsystem.components.StaggeredItem
import com.wenku8.reader.core.designsystem.components.SwipeRevealAction
import com.wenku8.reader.core.designsystem.theme.Spacing
import com.wenku8.reader.ui.AppBottomBar

@Composable
fun LibraryScreen(
    currentRoute: String?,
    onNavigateTab: (String) -> Unit,
    onOpenDetail: (aid: Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var pendingRemove by remember { mutableStateOf<FavoriteNovelEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomBar(currentRoute, onNavigateTab) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "我的小说",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.md),
            )

            if (favorites.isEmpty()) {
                EmptyLibrary()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.xs),
                ) {
                    items(favorites, key = { it.aid }) { favorite ->
                        val index = favorites.indexOf(favorite).coerceAtLeast(0)
                        StaggeredItem(index = index) {
                            SwipeRevealAction(
                                actionIcon = Icons.Outlined.BookmarkRemove,
                                actionContentDescription = "取消收藏",
                                onAction = { pendingRemove = favorite },
                                onContentClick = { onOpenDetail(favorite.aid) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xxs),
                            ) {
                                FavoriteRow(favorite = favorite)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRemove?.let { favorite ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("取消收藏") },
            text = { Text("确定把《${favorite.title}》从「我的小说」中移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(favorite.aid)
                    pendingRemove = null
                }) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                "还没有收藏任何小说\n在小说详情页点「收藏」即可加入这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FavoriteRow(favorite: FavoriteNovelEntity) {
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
                aid = favorite.aid,
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    favorite.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        favorite.author.ifBlank { null },
                        favorite.lastUpdate.ifBlank { null },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
