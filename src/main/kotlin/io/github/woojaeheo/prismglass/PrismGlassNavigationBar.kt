package io.github.woojaeheo.prismglass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
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
import kotlin.math.max
import kotlin.math.sign

/** 유리 네비게이션 스타일 */
@Immutable
data class PrismGlassNavigationStyle(
    val surface: PrismGlassStyle,
    val indicator: PrismGlassStyle,
    val height: Dp = 68.dp,
    val indicatorPadding: Dp = 4.dp,
    val stretch: Float = 1.20f,
    val surfaceFill: Color = Color.Black.copy(alpha = .48f),
    val indicatorFill: Color = Color.Black.copy(alpha = .88f),
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
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)
    var visualIndex by remember { mutableIntStateOf(selectedIndex) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var direction by remember { mutableIntStateOf(1) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragPositionPx by remember { mutableFloatStateOf(0f) }
    var dragStretch by remember { mutableFloatStateOf(1f) }
    var dragVelocityPx by remember { mutableFloatStateOf(0f) }
    var previousDragTimeMillis by remember { mutableStateOf<Long?>(null) }
    var settlePulse by remember { mutableIntStateOf(0) }
    var releaseStretch by remember { mutableFloatStateOf(style.stretch) }
    var dragging by remember { mutableStateOf(false) }
    val stretch = remember { Animatable(1f) }
    val navigationItemsBackdrop = rememberPrismGlassBackdropState()
    LaunchedEffect(selectedIndex) {
        if (!dragging) visualIndex = selectedIndex
    }
    LaunchedEffect(visualIndex, settlePulse, reducedMotion, layoutDirection) {
        if (dragging) return@LaunchedEffect
        if (reducedMotion) {
            previousIndex = visualIndex
            stretch.snapTo(1f)
            return@LaunchedEffect
        }
        if (visualIndex != previousIndex || settlePulse > 0) {
            val previousPhysicalIndex = previousIndex.toPhysicalIndex(items.size, layoutDirection)
            val currentPhysicalIndex = visualIndex.toPhysicalIndex(items.size, layoutDirection)
            direction = if (currentPhysicalIndex > previousPhysicalIndex) 1 else -1
            previousIndex = visualIndex
            stretch.snapTo(max(style.stretch, releaseStretch))
            stretch.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = .58f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            releaseStretch = style.stretch
        }
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
                    onDragStart = { offset ->
                        dragging = true
                        dragPositionPx = offset.x.coerceIn(0f, containerWidthPx)
                        dragStretch = 1f
                        dragVelocityPx = 0f
                        previousDragTimeMillis = null
                    },
                    onDragCancel = {
                        dragging = false
                        dragStretch = 1f
                        dragVelocityPx = 0f
                        previousDragTimeMillis = null
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
                        releaseStretch = max(dragStretch, 1.44f)
                        dragging = false
                        settlePulse++
                        previousDragTimeMillis = null
                        currentOnItemSelected(items[targetIndex])
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
                    val elapsedMillis = previousDragTimeMillis
                        ?.let { (change.uptimeMillis - it).coerceAtLeast(1L) }
                    val instantVelocity = elapsedMillis?.let { dragAmount / it * 1_000f } ?: 0f
                    dragVelocityPx = dragVelocityPx * .58f + instantVelocity * .42f
                    previousDragTimeMillis = change.uptimeMillis
                    dragStretch = if (reducedMotion) {
                        1f
                    } else {
                        stretchForVelocity(dragVelocityPx, itemWidthPx)
                    }
                    change.consume()
                }
            },
    ) {
        val itemWidth = maxWidth / items.size
        val physicalIndex = visualIndex.toPhysicalIndex(items.size, layoutDirection)
        val indicatorDiameter = (style.height - style.indicatorPadding * 2).coerceAtMost(itemWidth - style.indicatorPadding * 2)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * physicalIndex + (itemWidth - indicatorDiameter) / 2,
            animationSpec = spring(
                dampingRatio = if (reducedMotion) 1f else Spring.DampingRatioMediumBouncy,
                stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow,
            ),
            label = "prism-glass-navigation-position",
        )
        val draggedOffset = with(density) {
            val diameterPx = indicatorDiameter.toPx()
            (dragPositionPx - diameterPx / 2f)
                .coerceIn(0f, (containerWidthPx - diameterPx).coerceAtLeast(0f))
                .toDp()
        }
        val activeOffset = if (dragging) draggedOffset else indicatorOffset
        val activeIndex = if (dragging) {
            resolvedIndexForPosition(
                position = dragPositionPx,
                width = containerWidthPx,
                itemCount = items.size,
                rightToLeft = layoutDirection == LayoutDirection.Rtl,
            )
        } else {
            visualIndex
        }
        val dragExpansion by animateFloatAsState(
            targetValue = if (dragging && !reducedMotion) 1f else 0f,
            animationSpec = spring(dampingRatio = .62f, stiffness = Spring.StiffnessMedium),
            label = "prism-glass-navigation-expansion",
        )
        NavigationItemsRow(
            items = items,
            visualIndex = activeIndex,
            enabled = enabled,
            itemLabel = itemLabel,
            backdropState = navigationItemsBackdrop,
            renderSelection = indicatorContent != null,
            onItemSelected = { index, item ->
                visualIndex = index
                releaseStretch = style.stretch
                currentOnItemSelected(item)
            },
            itemContent = itemContent,
        )
        PrismGlassBackdropSurface(
            state = navigationItemsBackdrop,
            modifier = Modifier.offset(x = activeOffset)
                .width(indicatorDiameter).height(indicatorDiameter).align(Alignment.CenterStart)
                .graphicsLayer {
                    if (dragging) {
                        scaleX = 1f + dragExpansion * .38f + (dragStretch - 1f) * .42f
                        scaleY = 1f + dragExpansion * .44f + (dragStretch - 1f) * .24f
                    } else {
                        scaleX = stretch.value
                        scaleY = 1f + (stretch.value - 1f) * .56f
                    }
                    transformOrigin = TransformOrigin(if (direction > 0) 1f else 0f, .5f)
                },
            style = style.indicator,
            blurRadius = 2.dp,
            refraction = .28f,
        ) {
            Box(Modifier.fillMaxSize().background(style.indicatorFill, style.indicator.shape))
            if (indicatorContent == null) {
                RefractedActiveItem(
                    item = items[activeIndex],
                    itemWidth = itemWidth,
                    indicatorDiameter = indicatorDiameter,
                    velocity = if (dragging && !reducedMotion) dragVelocityPx else 0f,
                    direction = direction,
                    dragging = dragging && !reducedMotion,
                    itemContent = itemContent,
                )
            } else {
                indicatorContent(items[activeIndex])
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 2.dp).width(28.dp).height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = .86f), Color.Transparent),
                        ),
                        RoundedCornerShape(50),
                    ),
            )
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

@Composable
private fun <T> RefractedActiveItem(
    item: T,
    itemWidth: Dp,
    indicatorDiameter: Dp,
    velocity: Float,
    direction: Int,
    dragging: Boolean,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val diameterPx = with(density) { indicatorDiameter.toPx() }
    val normalizedVelocity = with(density) {
        (velocity / (itemWidth.toPx() * 2.8f)).coerceIn(-1f, 1f)
    }
    val effectiveVelocity = if (dragging) {
        val signedDirection = if (normalizedVelocity == 0f) direction.toFloat() else normalizedVelocity.sign
        signedDirection * max(abs(normalizedVelocity), .42f)
    } else {
        0f
    }
    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberAgslLensEffect(
            width = diameterPx,
            height = diameterPx,
            centerX = diameterPx / 2f,
            centerY = diameterPx / 2f,
            radius = diameterPx / 2f,
            velocity = effectiveVelocity,
        )
    } else {
        null
    }
    Column(
        modifier = Modifier.fillMaxSize().graphicsLayer {
            renderEffect = effect
            if (effect == null && dragging) {
                scaleX = 1.10f
                scaleY = 1.10f
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        itemContent(item, true)
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun rememberAgslLensEffect(
    width: Float,
    height: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    velocity: Float,
): androidx.compose.ui.graphics.RenderEffect? {
    val shader = remember { RuntimeShader(LENS_SHADER) }
    shader.setFloatUniform("size", width, height)
    shader.setFloatUniform("center", centerX, centerY)
    shader.setFloatUniform("radius", radius)
    shader.setFloatUniform("velocity", velocity)
    return remember(shader) {
        AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
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

private fun Int.toPhysicalIndex(itemCount: Int, layoutDirection: LayoutDirection): Int =
    if (layoutDirection == LayoutDirection.Rtl) itemCount - this - 1 else this

private const val LENS_SHADER = """
    uniform shader content;
    uniform float2 size;
    uniform float2 center;
    uniform float radius;
    uniform float velocity;

    half4 main(float2 position) {
        float speed = min(abs(velocity), 1.0);
        float2 delta = position - center;
        float stretch = 1.0 + speed * 0.45;
        float distanceFromCenter = length(float2(delta.x / stretch, delta.y));
        float influence = clamp(1.0 - distanceFromCenter / radius, 0.0, 1.0);
        float magnification = speed * 0.42;
        float2 samplePosition = center + float2(
            delta.x * (1.0 - influence * influence * magnification),
            delta.y * (1.0 - influence * magnification * 0.62)
        );
        samplePosition.x -= sign(velocity) * influence * influence * speed * radius * 0.20;
        samplePosition.y += sin(delta.x / radius * 3.14159) * influence * speed * radius * 0.07;
        samplePosition = clamp(samplePosition, float2(0.0), size - float2(1.0));
        float blurRadius = speed * 3.0;
        half4 sharp = content.eval(samplePosition);
        half4 blurred = (
            content.eval(samplePosition + float2(blurRadius, 0.0)) +
            content.eval(samplePosition - float2(blurRadius, 0.0)) +
            content.eval(samplePosition + float2(0.0, blurRadius)) +
            content.eval(samplePosition - float2(0.0, blurRadius))
        ) * 0.25;
        half4 refracted = mix(sharp, blurred, speed * 0.34);
        half highlight = half(pow(influence, 4.0) * 0.06);
        return refracted + half4(highlight, highlight, highlight, 0.0);
    }
"""
