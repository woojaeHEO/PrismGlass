package io.github.woojaeheo.prismglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 유리 네비게이션 스타일 */
@Immutable
data class PrismGlassNavigationStyle(
    val surface: PrismGlassStyle,
    val indicator: PrismGlassStyle,
    val height: Dp = 68.dp,
    val indicatorPadding: Dp = 4.dp,
    val stretch: Float = 1.20f,
    val surfaceFill: Color = Color.Black.copy(alpha = .18f),
    val indicatorFill: Color = Color.Black.copy(alpha = .26f),
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
    backdropState: PrismGlassBackdropState? = null,
    indicatorContent: (@Composable BoxScope.(T) -> Unit)? = null,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    val selectedIndex = items.resolvedSelectedIndex(selectedItem)
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)
    val animationScope = androidx.compose.runtime.rememberCoroutineScope()
    var visualIndex by remember { mutableIntStateOf(selectedIndex) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragTargetValue by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var previousDragTimeMillis by remember { mutableStateOf<Long?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val positionAnimation = remember { Animatable(selectedIndex.toFloat(), .001f) }
    val velocityAnimation = remember { Animatable(0f, .01f) }
    val scaleXAnimation = remember { Animatable(1f, .001f) }
    val scaleYAnimation = remember { Animatable(1f, .001f) }
    val navigationItemsBackdrop = rememberPrismGlassBackdropState()
    LaunchedEffect(selectedIndex) {
        if (dragging) return@LaunchedEffect
        visualIndex = selectedIndex
        if (reducedMotion) {
            positionAnimation.snapTo(selectedIndex.toFloat())
            velocityAnimation.snapTo(0f)
            scaleXAnimation.snapTo(1f)
            scaleYAnimation.snapTo(1f)
            return@LaunchedEffect
        }
        launch { scaleXAnimation.animateTo(PRESSED_SCALE, PRESS_X_SPRING) }
        launch { scaleYAnimation.animateTo(PRESSED_SCALE, PRESS_Y_SPRING) }
        positionAnimation.animateTo(selectedIndex.toFloat(), POSITION_SPRING)
        launch { velocityAnimation.animateTo(0f, VELOCITY_SPRING) }
        launch { scaleXAnimation.animateTo(1f, PRESS_X_SPRING) }
        launch { scaleYAnimation.animateTo(1f, PRESS_Y_SPRING) }
    }

    BoxWithConstraints(
        modifier.fillMaxWidth().height(style.height)
            .background(style.surfaceFill, style.surface.shape)
            .prismGlass(style.surface)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(
                items,
                selectedIndex,
                enabled,
                dragEnabled,
                reducedMotion,
                layoutDirection,
                containerWidthPx,
            ) {
                if (!enabled || !dragEnabled || containerWidthPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        dragTargetValue = positionAnimation.value
                        previousDragTimeMillis = null
                        if (!reducedMotion) {
                            animationScope.launch { scaleXAnimation.animateTo(PRESSED_SCALE, PRESS_X_SPRING) }
                            animationScope.launch { scaleYAnimation.animateTo(PRESSED_SCALE, PRESS_Y_SPRING) }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        previousDragTimeMillis = null
                        visualIndex = selectedIndex
                        animationScope.launch {
                            positionAnimation.animateTo(selectedIndex.toFloat(), POSITION_SPRING)
                        }
                        animationScope.launch { velocityAnimation.animateTo(0f, VELOCITY_SPRING) }
                        animationScope.launch { scaleXAnimation.animateTo(1f, PRESS_X_SPRING) }
                        animationScope.launch { scaleYAnimation.animateTo(1f, PRESS_Y_SPRING) }
                    },
                    onDragEnd = {
                        val targetIndex = dragTargetValue.roundToInt().coerceIn(items.indices)
                        visualIndex = targetIndex
                        dragging = false
                        previousDragTimeMillis = null
                        animationScope.launch {
                            positionAnimation.animateTo(targetIndex.toFloat(), POSITION_SPRING)
                            launch { scaleXAnimation.animateTo(1f, PRESS_X_SPRING) }
                            launch { scaleYAnimation.animateTo(1f, PRESS_Y_SPRING) }
                        }
                        animationScope.launch { velocityAnimation.animateTo(0f, VELOCITY_SPRING) }
                        currentOnItemSelected(items[targetIndex])
                    },
                ) { change, dragAmount ->
                    val itemWidthPx = containerWidthPx / items.size
                    val logicalDirection = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                    dragTargetValue = (dragTargetValue + dragAmount / itemWidthPx * logicalDirection)
                        .coerceIn(0f, items.lastIndex.toFloat())
                    visualIndex = dragTargetValue.roundToInt().coerceIn(items.indices)
                    val elapsedMillis = previousDragTimeMillis
                        ?.let { (change.uptimeMillis - it).coerceAtLeast(1L) }
                    val instantVelocity = elapsedMillis?.let { dragAmount / it * 1_000f } ?: 0f
                    previousDragTimeMillis = change.uptimeMillis
                    if (reducedMotion) {
                        animationScope.launch { positionAnimation.snapTo(dragTargetValue) }
                    } else {
                        val range = items.lastIndex.coerceAtLeast(1).toFloat()
                        val normalizedVelocity = instantVelocity / itemWidthPx / range * logicalDirection
                        animationScope.launch {
                            positionAnimation.animateTo(dragTargetValue, POSITION_SPRING)
                        }
                        animationScope.launch {
                            velocityAnimation.animateTo(normalizedVelocity, VELOCITY_SPRING)
                        }
                    }
                    change.consume()
                }
            },
    ) {
        val itemWidth = maxWidth / items.size
        val containerWidth = maxWidth
        val indicatorDiameter = (style.height - style.indicatorPadding * 2).coerceAtMost(itemWidth - style.indicatorPadding * 2)
        val logicalPosition = positionAnimation.value.coerceIn(0f, items.lastIndex.toFloat())
        val physicalPosition = if (layoutDirection == LayoutDirection.Ltr) {
            logicalPosition
        } else {
            items.lastIndex - logicalPosition
        }
        val activeOffset = itemWidth * physicalPosition + (itemWidth - indicatorDiameter) / 2
        val activeIndex = logicalPosition.roundToInt().coerceIn(items.indices)
        NavigationItemsRow(
            items = items,
            visualIndex = activeIndex,
            enabled = enabled,
            itemLabel = itemLabel,
            backdropState = navigationItemsBackdrop,
            renderSelection = indicatorContent != null,
            onItemSelected = { index, item ->
                visualIndex = index
                currentOnItemSelected(item)
            },
            itemContent = itemContent,
        )
        PrismGlassBackdropSurface(
            state = backdropState ?: navigationItemsBackdrop,
            modifier = Modifier.offset(x = activeOffset)
                .width(indicatorDiameter).height(indicatorDiameter).align(Alignment.CenterStart)
                .graphicsLayer {
                    val velocity = (velocityAnimation.value / 10f)
                    scaleX = scaleXAnimation.value /
                        (1f - (velocity * .75f).coerceIn(-.2f, .2f))
                    scaleY = scaleYAnimation.value *
                        (1f - (velocity * .25f).coerceIn(-.2f, .2f))
                },
            style = style.indicator,
            blurRadius = 7.dp,
            refraction = .35f,
            velocity = (velocityAnimation.value / 3f).coerceIn(-1f, 1f),
        ) {
            Box(Modifier.fillMaxSize().background(style.indicatorFill, style.indicator.shape))
            LensLightingOverlay(style.indicator.shape)
            if (indicatorContent == null) {
                SelectedItemsOverlayRow(
                    items = items,
                    itemWidth = itemWidth,
                    containerWidth = containerWidth,
                    indicatorDiameter = indicatorDiameter,
                    indicatorOffset = activeOffset,
                    modifier = Modifier.align(Alignment.CenterStart),
                    itemContent = itemContent,
                )
            } else {
                indicatorContent(items[activeIndex])
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 2.dp).width(30.dp).height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = .72f), Color.Transparent),
                        ),
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}

/** 렌즈의 상단 반사와 하단 집광을 분리해 표현 */
@Composable
private fun BoxScope.LensLightingOverlay(shape: androidx.compose.ui.graphics.Shape) {
    Box(
        Modifier.matchParentSize().background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = .18f),
                .28f to Color.Transparent,
                .68f to Color.Transparent,
                1f to Color.Black.copy(alpha = .24f),
            ),
            shape,
        ),
    )
    Box(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).width(28.dp).height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = .26f), Color.Transparent),
                ),
                RoundedCornerShape(50),
            ),
    )
}

@Composable
private fun <T> SelectedItemsOverlayRow(
    items: List<T>,
    itemWidth: Dp,
    containerWidth: Dp,
    indicatorDiameter: Dp,
    indicatorOffset: Dp,
    modifier: Modifier,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    Row(
        modifier.requiredWidth(containerWidth).fillMaxHeight().offset(
            x = -indicatorOffset + (containerWidth - indicatorDiameter) / 2,
        ),
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier.width(itemWidth).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                itemContent(item, true)
            }
        }
    }
}

@Composable
private fun <T> NavigationItemsRow(
    items: List<T>,
    visualIndex: Int,
    enabled: Boolean,
    itemLabel: (T) -> String,
    backdropState: PrismGlassBackdropState,
    renderSelection: Boolean,
    onItemSelected: (Int, T) -> Unit,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    Row(Modifier.fillMaxSize().prismGlassBackdropSource(backdropState).selectableGroup()) {
        items.forEachIndexed { index, item ->
            val selected = index == visualIndex
            val interactionSource = remember(item) { MutableInteractionSource() }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                    contentDescription = itemLabel(item)
                }.selectable(
                    selected = selected,
                    enabled = enabled,
                    onClick = { onItemSelected(index, item) },
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = null,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                itemContent(item, selected && renderSelection)
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

internal fun stretchForVelocity(velocity: Float, itemWidth: Float): Float {
    if (!velocity.isFinite() || itemWidth <= 0f) return 1f
    return 1f + (abs(velocity) / (itemWidth * 6.2f)).coerceIn(0f, .82f)
}

private const val PRESSED_SCALE = 78f / 56f
private val POSITION_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 1_000f, visibilityThreshold = .001f)
private val VELOCITY_SPRING = spring<Float>(dampingRatio = .5f, stiffness = 300f, visibilityThreshold = .01f)
private val PRESS_X_SPRING = spring<Float>(dampingRatio = .6f, stiffness = 250f, visibilityThreshold = .001f)
private val PRESS_Y_SPRING = spring<Float>(dampingRatio = .7f, stiffness = 250f, visibilityThreshold = .001f)
