package io.github.woojaeheo.prismglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor

/** 유리 네비게이션 스타일 */
@Immutable
data class PrismGlassNavigationStyle(
    val surface: PrismGlassStyle,
    val indicator: PrismGlassStyle,
    val height: Dp = 68.dp,
    val indicatorPadding: Dp = 4.dp,
    val stretch: Float = 1.20f,
)

/** 네비게이션 기본값 */
object PrismGlassNavigationDefaults {
    /** 기본 네비게이션 스타일 */
    @Composable
    fun style() = PrismGlassNavigationStyle(
        surface = PrismGlassDefaults.surfaceStyle(RoundedCornerShape(30.dp)),
        indicator = PrismGlassDefaults.surfaceStyle(
            shape = RoundedCornerShape(28.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

/** 범용 슬롯 네비게이션 */
@Composable
fun <T> PrismGlassNavigationBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dragEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    style: PrismGlassNavigationStyle = PrismGlassNavigationDefaults.style(),
    indicatorContent: (@Composable BoxScope.(T) -> Unit)? = null,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    val selectedIndex = items.resolvedSelectedIndex(selectedItem)
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    var visualIndex by remember { mutableIntStateOf(selectedIndex) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var direction by remember { mutableIntStateOf(1) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragPositionPx by remember { mutableFloatStateOf(0f) }
    var dragStretch by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    val stretch = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        if (!dragging) visualIndex = selectedIndex
    }
    LaunchedEffect(visualIndex, reducedMotion, layoutDirection) {
        if (reducedMotion) {
            previousIndex = visualIndex
            stretch.snapTo(1f)
            return@LaunchedEffect
        }
        if (visualIndex != previousIndex) {
            val previousPhysicalIndex = previousIndex.toPhysicalIndex(items.size, layoutDirection)
            val currentPhysicalIndex = visualIndex.toPhysicalIndex(items.size, layoutDirection)
            direction = if (currentPhysicalIndex > previousPhysicalIndex) 1 else -1
            previousIndex = visualIndex
            stretch.snapTo(style.stretch)
            stretch.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    BoxWithConstraints(
        modifier.fillMaxWidth().height(style.height).prismGlass(style.surface)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(items, enabled, dragEnabled, layoutDirection, containerWidthPx) {
                if (!enabled || !dragEnabled || containerWidthPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragPositionPx = offset.x.coerceIn(0f, containerWidthPx)
                        dragStretch = if (reducedMotion) 1f else 1.08f
                    },
                    onDragCancel = {
                        dragging = false
                        dragStretch = 1f
                        visualIndex = selectedIndex
                    },
                    onDragEnd = {
                        val targetIndex = resolvedIndexForPosition(
                            position = dragPositionPx,
                            width = containerWidthPx,
                            itemCount = items.size,
                            rightToLeft = layoutDirection == LayoutDirection.Rtl,
                        )
                        visualIndex = targetIndex
                        dragging = false
                        dragStretch = 1f
                        onItemSelected(items[targetIndex])
                    },
                ) { change, dragAmount ->
                    val previousPosition = dragPositionPx
                    dragPositionPx = (dragPositionPx + dragAmount).coerceIn(0f, containerWidthPx)
                    visualIndex = resolvedIndexForPosition(
                        position = dragPositionPx,
                        width = containerWidthPx,
                        itemCount = items.size,
                        rightToLeft = layoutDirection == LayoutDirection.Rtl,
                    )
                    direction = if (dragPositionPx >= previousPosition) 1 else -1
                    val itemWidthPx = containerWidthPx / items.size
                    dragStretch = if (reducedMotion) {
                        1f
                    } else {
                        1f + (abs(dragAmount) / itemWidthPx * 1.8f).coerceIn(.06f, .18f)
                    }
                    change.consume()
                }
            },
    ) {
        val itemWidth = maxWidth / items.size
        val physicalIndex = visualIndex.toPhysicalIndex(items.size, layoutDirection)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * physicalIndex,
            animationSpec = spring(
                dampingRatio = if (reducedMotion) 1f else Spring.DampingRatioMediumBouncy,
                stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow,
            ),
            label = "prism-glass-navigation-position",
        )
        val draggedOffset = with(density) {
            val itemWidthPx = containerWidthPx / items.size
            (dragPositionPx - itemWidthPx / 2f)
                .coerceIn(0f, (containerWidthPx - itemWidthPx).coerceAtLeast(0f))
                .toDp()
        }
        Box(
            Modifier.offset(x = if (dragging) draggedOffset else indicatorOffset)
                .width(itemWidth).fillMaxHeight().padding(style.indicatorPadding)
                .graphicsLayer {
                    scaleX = if (dragging) dragStretch else stretch.value
                    transformOrigin = TransformOrigin(if (direction > 0) 0f else 1f, .5f)
                }.prismGlass(style.indicator),
        ) {
            if (indicatorContent == null) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 3.dp).width(30.dp).height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = .82f), Color.Transparent),
                            ),
                            RoundedCornerShape(50),
                        ),
                )
            } else {
                indicatorContent(items[visualIndex])
            }
        }
        Row(Modifier.fillMaxSize().selectableGroup()) {
            items.forEachIndexed { index, item ->
                val selected = index == visualIndex
                val interactionSource = remember(item) { MutableInteractionSource() }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                        contentDescription = itemLabel(item)
                    }.selectable(
                        selected = selected,
                        enabled = enabled,
                        onClick = {
                            visualIndex = index
                            onItemSelected(item)
                        },
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = null,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    itemContent(item, selected)
                }
            }
        }
    }
}

internal fun <T> List<T>.resolvedSelectedIndex(selectedItem: T): Int =
    indexOf(selectedItem).coerceAtLeast(0)

internal fun resolvedIndexForPosition(
    position: Float,
    width: Float,
    itemCount: Int,
    rightToLeft: Boolean,
): Int {
    if (width <= 0f || itemCount <= 1) return 0
    val physicalIndex = floor(position.coerceIn(0f, width) / (width / itemCount))
        .toInt()
        .coerceIn(0, itemCount - 1)
    return if (rightToLeft) itemCount - physicalIndex - 1 else physicalIndex
}

private fun Int.toPhysicalIndex(itemCount: Int, layoutDirection: LayoutDirection): Int =
    if (layoutDirection == LayoutDirection.Rtl) itemCount - this - 1 else this
