package com.wenku8.reader.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.feature.CoverViewModel

/**
 * Async novel cover (POST `book&do=cover` → ImageBitmap), cached in memory.
 * Shows a book placeholder while loading / on failure.
 */
@Composable
fun NovelCoverImage(
    aid: Int,
    modifier: Modifier,
    viewModel: CoverViewModel = hiltViewModel(),
) {
    val bitmap by viewModel.cover(aid).collectAsStateWithLifecycle()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val bmp: ImageBitmap? = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
