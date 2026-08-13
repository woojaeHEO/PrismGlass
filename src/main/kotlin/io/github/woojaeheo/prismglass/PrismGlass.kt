package io.github.woojaeheo.prismglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 유리 표면 스타일 */
@Immutable
data class PrismGlassStyle(
    val shape: Shape,
    val tint: Color,
    val highlight: Color,
    val primaryEdge: Color,
    val secondaryEdge: Color,
    val shadowElevation: Dp,
    val borderWidth: Dp,
    val decoration: PrismGlassDecoration = PrismGlassDecoration(),
)

/** 유리 기본값 */
object PrismGlassDefaults {
    /** 기본 표면 스타일 */
    @Composable
    fun surfaceStyle(
        shape: Shape = RoundedCornerShape(28.dp),
        tint: Color = MaterialTheme.colorScheme.surface,
    ) = PrismGlassStyle(
        shape = shape,
        tint = tint,
        highlight = Color.White,
        primaryEdge = MaterialTheme.colorScheme.tertiary,
        secondaryEdge = MaterialTheme.colorScheme.secondary,
        shadowElevation = 18.dp,
        borderWidth = 1.dp,
    )
}

/** 임의의 컴포넌트에 유리 표면 적용 */
fun Modifier.prismGlass(style: PrismGlassStyle): Modifier {
    val decoration = style.decoration.normalized()
    return this
    .shadow(
        elevation = style.shadowElevation.coerceAtLeast(0.dp),
        shape = style.shape,
        ambientColor = style.primaryEdge.copy(alpha = decoration.ambientShadowAlpha),
        spotColor = decoration.spotShadowColor.copy(alpha = decoration.spotShadowAlpha),
    )
    .clip(style.shape)
    .background(
        Brush.linearGradient(
            listOf(
                style.highlight.copy(alpha = decoration.topHighlightAlpha),
                style.tint.copy(alpha = decoration.tintStartAlpha),
                style.tint.copy(alpha = decoration.tintEndAlpha),
            ),
        ),
    )
    .background(
        Brush.radialGradient(
            listOf(style.highlight.copy(alpha = decoration.centerGlowAlpha), Color.Transparent),
            radius = 720f,
        ),
    )
    .border(
        width = style.borderWidth.coerceAtLeast(0.dp),
        brush = Brush.sweepGradient(
            listOf(
                style.highlight.copy(alpha = decoration.highlightEdgeAlpha),
                style.primaryEdge.copy(alpha = decoration.primaryEdgeAlpha),
                Color.Transparent,
                style.secondaryEdge.copy(alpha = decoration.secondaryEdgeAlpha),
                style.highlight.copy(alpha = decoration.highlightEdgeAlpha),
            ),
        ),
        shape = style.shape,
    )
}

/** 슬롯 기반 유리 표면 */
@Composable
fun PrismGlassSurface(
    modifier: Modifier = Modifier,
    style: PrismGlassStyle = PrismGlassDefaults.surfaceStyle(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.prismGlass(style), content = content)
}

/** 눌림 반응이 있는 유리 표면 */
@Composable
fun PrismGlassInteractiveSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reducedMotion: Boolean = false,
    role: Role? = null,
    style: PrismGlassStyle = PrismGlassDefaults.surfaceStyle(),
    pressSpec: PrismGlassPressSpec = PrismGlassPressSpec(),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) pressSpec.safePressedScale else 1f,
        animationSpec = pressSpec.spring,
        label = "prism-glass-press",
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.prismGlass(style).clickable(
            enabled = enabled,
            role = role,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        content = content,
    )
}
