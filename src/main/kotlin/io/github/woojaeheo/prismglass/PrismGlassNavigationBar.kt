package io.github.woojaeheo.prismglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    reducedMotion: Boolean = false,
    style: PrismGlassNavigationStyle = PrismGlassNavigationDefaults.style(),
    indicatorContent: (@Composable BoxScope.(T) -> Unit)? = null,
    itemContent: @Composable ColumnScope.(item: T, selected: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    val selectedIndex = items.resolvedSelectedIndex(selectedItem)
    val effectiveSelectedItem = items[selectedIndex]
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var direction by remember { mutableIntStateOf(1) }
    val stretch = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex, reducedMotion) {
        if (reducedMotion) {
            previousIndex = selectedIndex
            stretch.snapTo(1f)
            return@LaunchedEffect
        }
        if (selectedIndex != previousIndex) {
            direction = if (selectedIndex > previousIndex) 1 else -1
            previousIndex = selectedIndex
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

    BoxWithConstraints(modifier.fillMaxWidth().height(style.height).prismGlass(style.surface)) {
        val itemWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = if (reducedMotion) 1f else Spring.DampingRatioMediumBouncy,
                stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow,
            ),
            label = "prism-glass-navigation-position",
        )
        Box(
            Modifier.offset(x = indicatorOffset).width(itemWidth).fillMaxHeight().padding(style.indicatorPadding)
                .graphicsLayer {
                    scaleX = stretch.value
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
                indicatorContent(items[selectedIndex])
            }
        }
        Row(Modifier.fillMaxSize().selectableGroup()) {
            items.forEach { item ->
                val selected = item == effectiveSelectedItem
                val interactionSource = remember(item) { MutableInteractionSource() }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                        contentDescription = itemLabel(item)
                    }.selectable(
                        selected = selected,
                        enabled = enabled,
                        onClick = { onItemSelected(item) },
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
