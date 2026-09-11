package com.wenku8.reader.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Registry (per-reader-screen) of screen regions occupied by currently-failed
 * illustrations. The reader's tap overlay sits ABOVE the pager and consults
 * this map first so a tap on failed art triggers retry without the image
 * consuming the touch itself (which would break page turning).
 */
class IllustrationErrorRegions {
    private val regions = LinkedHashMap<Int, Rect>()

    fun register(key: Int, bounds: Rect) {
        regions[key] = bounds
    }

    fun unregister(key: Int) {
        regions.remove(key)
    }

    fun contains(x: Float, y: Float): Boolean = regions.values.any { it.contains(Offset(x, y)) }
}

/**
 * Chapter illustration loaded by Coil. wenku8 image URLs 301 across hosts
 * (pic.wenku8.cn → pic.777743.xyz, http→https); show a spinner while that
 * resolves and a tap-to-retry hint on failure. Failure regions are reported to
 * [errorRegions] because the reader's own tap layer covers this composable;
 * bumping [retryTick] relaunches the request.
 */
@Composable
fun NovelIllustration(
    url: String,
    modifier: Modifier = Modifier,
    errorRegions: IllustrationErrorRegions? = null,
    retryTick: Int = 0,
) {
    // Fragment changes Coil's model (cache key) to relaunch the request
    // without altering the URL sent to the server.
    val model = if (retryTick == 0) url else "$url#retry=$retryTick"
    val regionKey = remember(url) { url.hashCode() }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = "插图",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        error = {
            if (errorRegions != null) {
                DisposableEffect(regionKey) {
                    onDispose { errorRegions.unregister(regionKey) }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        errorRegions?.register(regionKey, coords.boundsInRoot())
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "点击重试",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "插图加载失败，点击重试",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
