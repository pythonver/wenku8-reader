package com.wenku8.reader.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class RevealValue { Closed, Open }

/**
 * A row whose content can be dragged left (end-to-start) to reveal a single
 * action button on the trailing edge. The content settles open instead of
 * being dismissed; tapping the revealed action triggers [onAction] (and closes
 * the row), tapping the content while open closes it, otherwise it clicks
 * through via [onContentClick].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealAction(
    actionIcon: ImageVector,
    actionContentDescription: String,
    onAction: () -> Unit,
    onContentClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContainerColor: Color = MaterialTheme.colorScheme.errorContainer,
    actionContentColor: Color = MaterialTheme.colorScheme.onErrorContainer,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 76.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()

    val state = remember {
        val anchors = DraggableAnchors<RevealValue> {
            RevealValue.Closed at 0f
            RevealValue.Open at -actionWidthPx
        }
        AnchoredDraggableState(
            initialValue = RevealValue.Closed,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 120.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    Box(
        modifier
            .height(IntrinsicSize.Min)
            .anchoredDraggable(state, Orientation.Horizontal)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(actionContentDescription) {
                        onAction()
                        true
                    }
                )
            },
    ) {
        // Trailing action, revealed underneath the sliding content.
        Box(
            Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(actionContainerColor),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
                    .fillMaxHeight()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = actionContentDescription,
                    ) {
                        scope.launch { state.animateTo(RevealValue.Closed) }
                        onAction()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    actionIcon,
                    contentDescription = actionContentDescription,
                    tint = actionContentColor,
                )
            }
        }

        // Foreground content slides over the action.
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .clickable {
                    if (state.currentValue == RevealValue.Open) {
                        scope.launch { state.animateTo(RevealValue.Closed) }
                    } else {
                        onContentClick()
                    }
                },
        ) {
            content()
        }
    }
}
