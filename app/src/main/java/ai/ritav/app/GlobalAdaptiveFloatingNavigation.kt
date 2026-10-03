package ai.ritav.app

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class GlobalNavItem(
    val label: String,
    val glyph: String,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GlobalAdaptiveFloatingNavigation(
    items: List<GlobalNavItem>,
    initialPosition: NavigationPositionPreference,
    motionPreference: UiMotionPreference,
    onPositionSettled: (xFraction: Float, yFraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val buttonSize = 58.dp
    val itemSize = 48.dp
    val margin = 12.dp
    val buttonPx = with(density) { buttonSize.toPx() }
    val itemPx = with(density) { itemSize.toPx() }
    val marginPx = with(density) { margin.toPx() }

    val systemAllowsMotion = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
    val animationsEnabled = when (motionPreference) {
        UiMotionPreference.AUTO -> systemAllowsMotion
        UiMotionPreference.ON -> true
        UiMotionPreference.OFF -> false
    }

    var xFraction by remember(initialPosition.xFraction, initialPosition.yFraction, initialPosition.fixed) {
        mutableFloatStateOf(initialPosition.xFraction.coerceIn(0f, 1f))
    }
    var yFraction by remember(initialPosition.xFraction, initialPosition.yFraction, initialPosition.fixed) {
        mutableFloatStateOf(initialPosition.yFraction.coerceIn(0f, 1f))
    }
    var expanded by remember { mutableStateOf(false) }
    var menuRotation by remember { mutableFloatStateOf(0f) }
    val menuFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val minX = marginPx
        val maxX = (widthPx - buttonPx - marginPx).coerceAtLeast(minX)
        val minY = marginPx
        val maxY = (heightPx - buttonPx - marginPx).coerceAtLeast(minY)

        val positionX = minX + ((maxX - minX) * xFraction)
        val positionY = minY + ((maxY - minY) * yFraction)

        val baseMenuRadius = maxOf(78f, itemPx * 1.55f)
        val availableRadialRadius = minOf(
            (positionX + buttonPx / 2f - marginPx - itemPx / 2f).coerceAtLeast(0f),
            (widthPx - (positionX + buttonPx / 2f) - marginPx - itemPx / 2f).coerceAtLeast(0f),
            (positionY + buttonPx / 2f - marginPx - itemPx / 2f).coerceAtLeast(0f),
            (heightPx - (positionY + buttonPx / 2f) - marginPx - itemPx / 2f).coerceAtLeast(0f)
        )
        val useLinear = availableRadialRadius < itemPx * 1.35f
        val horizontalLinear =
            maxOf(positionX + buttonPx / 2f, widthPx - (positionX + buttonPx / 2f)) >=
                maxOf(positionY + buttonPx / 2f, heightPx - (positionY + buttonPx / 2f))
        val linearAxisCapacity = if (horizontalLinear) {
            ((widthPx - (marginPx * 2f)) / (itemPx + 8f)).toInt()
        } else {
            ((heightPx - (marginPx * 2f)) / (itemPx + 8f)).toInt()
        }.coerceAtLeast(1)
        val visibleCount = minOf(
            items.size,
            8,
            if (useLinear) linearAxisCapacity else 8
        )
        val radialRadius = minOf(baseMenuRadius, availableRadialRadius)
        val rotationCount = if (items.size > visibleCount) items.size else 1
        val normalizedRotation = if (rotationCount == 1) {
            0f
        } else {
            ((menuRotation % rotationCount.toFloat()) + rotationCount.toFloat()) % rotationCount.toFloat()
        }
        val rotationIndex = normalizedRotation.toInt()
        val rotationFraction = normalizedRotation - rotationIndex
        val visibleItems = List(minOf(visibleCount, items.size)) { index ->
            items[(rotationIndex + index) % items.size]
        }

        LaunchedEffect(expanded, rotationIndex) {
            if (expanded && visibleItems.isNotEmpty()) {
                menuFocusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(positionX.roundToInt(), positionY.roundToInt()) }
                .size(buttonSize)
                .pointerInput(widthPx, heightPx, initialPosition.fixed) {
                    detectDragGestures(
                        onDragStart = { expanded = false },
                        onDrag = { change, dragAmount ->
                            if (initialPosition.fixed) return@detectDragGestures
                            change.consume()

                            val nextX = (positionX + dragAmount.x).coerceIn(minX, maxX)
                            val nextY = (positionY + dragAmount.y).coerceIn(minY, maxY)
                            xFraction = if (maxX > minX) {
                                ((nextX - minX) / (maxX - minX)).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            yFraction = if (maxY > minY) {
                                ((nextY - minY) / (maxY - minY)).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                        },
                        onDragEnd = {
                            if (!initialPosition.fixed) {
                                onPositionSettled(xFraction, yFraction)
                            }
                        },
                        onDragCancel = {
                            if (!initialPosition.fixed) {
                                onPositionSettled(xFraction, yFraction)
                            }
                        }
                    )
                }
        ) {
            Surface(
                onClick = {
                    if (!expanded) menuRotation = 0f
                    expanded = !expanded
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 5.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.semantics {
                    contentDescription =
                        if (initialPosition.fixed) {
                            "Ritav global navigation, fixed"
                        } else {
                            "Ritav global navigation, movable"
                        }
                    role = Role.Button
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "R",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (rotationCount > 1 && expanded) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(width = 30.dp, height = 22.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${rotationIndex + 1}/${items.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            visibleItems.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier
                        .offset {
                            val placement = if (useLinear) {
                                linearPlacement(
                                    index = index - rotationFraction,
                                    count = visibleItems.size,
                                    centerX = buttonPx / 2f,
                                    centerY = buttonPx / 2f,
                                    width = widthPx,
                                    height = heightPx,
                                    itemPx = itemPx,
                                    buttonX = positionX,
                                    buttonY = positionY,
                                    margin = marginPx
                                )
                            } else {
                                radialPlacement(
                                    index = index - rotationFraction,
                                    count = visibleItems.size,
                                    centerX = positionX + buttonPx / 2f,
                                    centerY = positionY + buttonPx / 2f,
                                    radius = radialRadius,
                                    itemPx = itemPx
                                )
                            }
                            IntOffset(
                                (placement.x - positionX).roundToInt(),
                                (placement.y - positionY).roundToInt()
                            )
                        },
                    enter = if (animationsEnabled) fadeIn() + scaleIn() else EnterTransition.None,
                    exit = if (animationsEnabled) fadeOut() + scaleOut() else ExitTransition.None
                ) {
                    val focusModifier =
                        if (index == 0) Modifier.focusRequester(menuFocusRequester) else Modifier
                    Surface(
                        onClick = {
                            expanded = false
                            item.onClick()
                        },
                        modifier = Modifier
                            .then(focusModifier)
                            .size(itemSize)
                            .focusable()
                            .pointerInput(rotationCount) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (rotationCount > 1 && event.type == PointerEventType.Scroll) {
                                            val scroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                            if (scroll != 0f) {
                                                menuRotation += if (scroll > 0f) 0.45f else -0.45f
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(
                                rotationCount,
                                itemPx,
                                useLinear,
                                index,
                                rotationFraction,
                                radialRadius
                            ) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        if (rotationCount <= 1) return@detectDragGestures
                                        change.consume()

                                        if (useLinear) {
                                            val axisDelta = if (abs(dragAmount.x) >= abs(dragAmount.y)) {
                                                dragAmount.x
                                            } else {
                                                dragAmount.y
                                            }
                                            menuRotation -= axisDelta / (itemPx + 8f)
                                        } else {
                                            val step = (Math.PI * 2.0 / visibleItems.size.coerceAtLeast(1)).toFloat()
                                            val angle = (-Math.PI / 2.0).toFloat() +
                                                (step * (index - rotationFraction))
                                            val tangentX = -sin(angle)
                                            val tangentY = cos(angle)
                                            val tangentialDelta =
                                                dragAmount.x * tangentX + dragAmount.y * tangentY
                                            val safeRadius = radialRadius.coerceAtLeast(1f)
                                            menuRotation -= tangentialDelta / (safeRadius * step.coerceAtLeast(0.001f))
                                        }
                                    }
                                )
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) {
                                    false
                                } else {
                                    when {
                                        event.key == Key.Escape -> {
                                            expanded = false
                                            true
                                        }
                                        (event.key == Key.PageDown || event.key == Key.DirectionRight || event.key == Key.DirectionDown) && rotationCount > 1 -> {
                                            menuRotation += 1f
                                            true
                                        }
                                        (event.key == Key.PageUp || event.key == Key.DirectionLeft || event.key == Key.DirectionUp) && rotationCount > 1 -> {
                                            menuRotation -= 1f
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }
                            .semantics {
                                contentDescription =
                                    if (item.selected) "${item.label}, selected"
                                    else item.label
                                selected = item.selected
                            },
                        shape = if (useLinear) RoundedCornerShape(16.dp) else CircleShape,
                        color = if (item.selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = if (item.selected) 5.dp else 3.dp,
                        shadowElevation = 3.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.glyph,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MenuPlacement(val x: Float, val y: Float)

private fun radialPlacement(
    index: Float,
    count: Int,
    centerX: Float,
    centerY: Float,
    radius: Float,
    itemPx: Float
): MenuPlacement {
    val start = -Math.PI / 2.0
    val step = if (count <= 1) 0.0 else (Math.PI * 2.0) / count
    val angle = start + (step * index)
    return MenuPlacement(
        x = centerX + cos(angle).toFloat() * radius - itemPx / 2f,
        y = centerY + sin(angle).toFloat() * radius - itemPx / 2f
    )
}

private fun linearPlacement(
    index: Float,
    count: Int,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    itemPx: Float,
    buttonX: Float,
    buttonY: Float,
    margin: Float
): MenuPlacement {
    val buttonCenterX = buttonX + centerX
    val buttonCenterY = buttonY + centerY
    val leftSpace = buttonCenterX
    val rightSpace = width - buttonCenterX
    val topSpace = buttonCenterY
    val bottomSpace = height - buttonCenterY

    val horizontal = maxOf(leftSpace, rightSpace) >= maxOf(topSpace, bottomSpace)
    if (horizontal) {
        val direction = if (rightSpace >= leftSpace) 1f else -1f
        val itemX = buttonCenterX + direction * (itemPx * 0.7f + 10f) - itemPx / 2f
        val total = (count - 1) * (itemPx + 8f)
        val startY = (buttonCenterY - total / 2f)
            .coerceIn(margin + itemPx / 2f, height - margin - itemPx / 2f)
        return MenuPlacement(
            x = itemX.coerceIn(margin, width - margin - itemPx),
            y = (startY + index * (itemPx + 8f))
                .coerceIn(margin, height - margin - itemPx)
        )
    }

    val direction = if (bottomSpace >= topSpace) 1f else -1f
    val itemY = buttonCenterY + direction * (itemPx * 0.7f + 10f) - itemPx / 2f
    val total = (count - 1) * (itemPx + 8f)
    val startX = (buttonCenterX - total / 2f)
        .coerceIn(margin, width - margin - itemPx)
    return MenuPlacement(
        x = (startX + index * (itemPx + 8f))
            .coerceIn(margin, width - margin - itemPx),
        y = itemY.coerceIn(margin, height - margin - itemPx)
    )
}
