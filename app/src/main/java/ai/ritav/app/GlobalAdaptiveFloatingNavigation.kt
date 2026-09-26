package ai.ritav.app

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class GlobalNavItem(
    val label: String,
    val glyph: String,
    val onClick: () -> Unit
)

@Composable
internal fun GlobalAdaptiveFloatingNavigation(
    items: List<GlobalNavItem>,
    initialPosition: NavigationPositionPreference,
    onPositionSettled: (xFraction: Float, yFraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val buttonSize = 58.dp
    val itemSize = 48.dp
    val margin = 12.dp
    val buttonPx = with(density) { buttonSize.toPx() }
    val itemPx = with(density) { itemSize.toPx() }
    val marginPx = with(density) { margin.toPx() }

    var xFraction by remember(initialPosition.xFraction, initialPosition.yFraction, initialPosition.fixed) {
        mutableFloatStateOf(initialPosition.xFraction.coerceIn(0f, 1f))
    }
    var yFraction by remember(initialPosition.xFraction, initialPosition.yFraction, initialPosition.fixed) {
        mutableFloatStateOf(initialPosition.yFraction.coerceIn(0f, 1f))
    }
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val minX = marginPx
        val maxX = (widthPx - buttonPx - marginPx).coerceAtLeast(minX)
        val minY = marginPx
        val maxY = (heightPx - buttonPx - marginPx).coerceAtLeast(minY)

        val positionX = minX + ((maxX - minX) * xFraction)
        val positionY = minY + ((maxY - minY) * yFraction)

        val nearLeft = positionX < widthPx * 0.28f
        val nearRight = positionX > widthPx * 0.72f
        val nearTop = positionY < heightPx * 0.28f
        val nearBottom = positionY > heightPx * 0.72f
        val useLinear = nearLeft || nearRight || nearTop || nearBottom

        val menuRadius = maxOf(78f, itemPx * 1.55f)
        val visibleCount = minOf(items.size, 8)

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
                .semantics {
                    contentDescription =
                        if (initialPosition.fixed) {
                            "Ritav global navigation, fixed"
                        } else {
                            "Ritav global navigation, movable"
                        }
                }
        ) {
            Surface(
                onClick = { expanded = !expanded },
                enabled = true,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 5.dp,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "R",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (expanded) {
                items.take(visibleCount).forEachIndexed { index, item ->
                    val placement = if (useLinear) {
                        linearPlacement(
                            index = index,
                            count = visibleCount,
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
                            index = index,
                            count = visibleCount,
                            centerX = positionX + buttonPx / 2f,
                            centerY = positionY + buttonPx / 2f,
                            radius = menuRadius,
                            itemPx = itemPx
                        )
                    }

                    Surface(
                        onClick = {
                            expanded = false
                            item.onClick()
                        },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (placement.x - positionX).roundToInt(),
                                    (placement.y - positionY).roundToInt()
                                )
                            }
                            .size(itemSize)
                            .semantics { contentDescription = item.label },
                        shape = if (useLinear) RoundedCornerShape(16.dp) else CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
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
    index: Int,
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
    index: Int,
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
