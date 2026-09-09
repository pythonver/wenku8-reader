package com.wenku8.reader.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fades + slides a child in on first composition, staggered by [index]
 * (60ms per sibling). Used for list content so rows enter with a light
 * cascade — part of the app's motion language.
 */
@Composable
fun StaggeredItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(28f) }
    LaunchedEffect(index) {
        delay(index * 60L)
        launch { alpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        },
    ) { content() }
}
