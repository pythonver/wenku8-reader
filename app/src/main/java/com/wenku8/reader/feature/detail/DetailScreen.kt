package com.wenku8.reader.feature.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.core.data.model.NovelDetail
import com.wenku8.reader.core.data.model.NovelMeta
import com.wenku8.reader.core.data.model.VolumeInfo
import com.wenku8.reader.core.designsystem.theme.ReaderFontFamily
import com.wenku8.reader.core.designsystem.theme.Spacing

@Composable
fun DetailScreen(
    aid: Int,
    onBack: () -> Unit,
    onOpenNovel: (aid: Int, cid: Int?) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val cover by viewModel.cover.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val resumeHint by viewModel.resumeHint.collectAsStateWithLifecycle()

    // Provide onBackground as the default content color: the root is a plain
    // Box (not Surface), so without this unspecified title/icon colors default
    // to black and vanish in night mode.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when {
                loading && detail == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                detail == null -> ErrorState(onRetry = viewModel::load, modifier = Modifier.align(Alignment.Center))
                else -> DetailContent(
                    detail = detail!!,
                    cover = cover,
                    isFavorite = isFavorite,
                    resumeHint = resumeHint,
                    onBack = onBack,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRead = { onOpenNovel(aid, null) },
                    onOpenChapter = { cid -> onOpenNovel(aid, cid) },
                )
            }
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("加载失败，请检查网络", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.md))
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun DetailContent(
    detail: NovelDetail,
    cover: androidx.compose.ui.graphics.ImageBitmap?,
    isFavorite: Boolean,
    resumeHint: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRead: () -> Unit,
    onOpenChapter: (Int) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            DetailTopBar(title = detail.meta.title, onBack = onBack)
        }
        item {
            HeaderSection(
                detail = detail,
                cover = cover,
                isFavorite = isFavorite,
                resumeHint = resumeHint,
                onToggleFavorite = onToggleFavorite,
                onRead = onRead,
            )
        }
        item {
            SectionLabel("简介")
            Text(
                detail.intro.ifEmpty { "暂无简介" },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ReaderFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.page),
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        item {
            SectionLabel("目录 · 共 ${detail.volumes.sumOf { it.chapters.size }} 章")
        }
        detail.volumes.forEach { volume ->
            item {
                VolumeHeader(volume)
            }
            items(volume.chapters.size) { i ->
                val chapter = volume.chapters[i]
                ChapterRow(name = chapter.name, onClick = { onOpenChapter(chapter.cid) })
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.xs),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HeaderSection(
    detail: NovelDetail,
    cover: androidx.compose.ui.graphics.ImageBitmap?,
    isFavorite: Boolean,
    resumeHint: String?,
    onToggleFavorite: () -> Unit,
    onRead: () -> Unit,
) {
    val meta = detail.meta
    Column(Modifier.padding(horizontal = Spacing.page)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Cover 2:3
            Box(
                Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(meta.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.xs))
                Text(meta.author.ifEmpty { "作者未知" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                MetaLine(meta)
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(if (resumeHint != null) "继续阅读" else "开始阅读")
            }
            OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f)) {
                Icon(
                    if (isFavorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(if (isFavorite) "已收藏" else "收藏")
            }
        }
        if (resumeHint != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "上次读到「$resumeHint」",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(Spacing.lg))
}

@Composable
private fun MetaLine(meta: NovelMeta) {
    val status = meta.bookStatus
    val length = if (meta.bookLength > 0) "%.1f万字".format(meta.bookLength / 10000.0) else null
    val bits = listOfNotNull(status, length, meta.lastUpdate, meta.pressId)
    Text(
        bits.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.sm),
    )
}

@Composable
private fun VolumeHeader(volume: VolumeInfo) {
    Text(
        volume.name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Spacing.page, vertical = Spacing.xs),
    )
}

@Composable
private fun ChapterRow(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.page, vertical = 12.dp),
        )
    }
}
