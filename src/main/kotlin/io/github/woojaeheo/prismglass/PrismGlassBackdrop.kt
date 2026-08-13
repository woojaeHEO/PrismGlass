package io.github.woojaeheo.prismglass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** 배경 캡처 상태 */
@Stable
class PrismGlassBackdropState internal constructor(
    internal val layer: GraphicsLayer,
) {
    internal var sourceCoordinates by mutableStateOf<LayoutCoordinates?>(null)
}

/** 배경 캡처 상태 생성 */
@Composable
fun rememberPrismGlassBackdropState(): PrismGlassBackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { PrismGlassBackdropState(layer) }
}

/** 임의의 컴포넌트를 유리 효과의 배경 소스로 등록 */
fun Modifier.prismGlassBackdropSource(state: PrismGlassBackdropState): Modifier = this
    .prismGlassBackdropSource(state, drawSource = true)

/** 화면에는 숨기고 유리 효과에만 제공하는 배경 소스 */
fun Modifier.prismGlassHiddenBackdropSource(state: PrismGlassBackdropState): Modifier = this
    .prismGlassBackdropSource(state, drawSource = false)

private fun Modifier.prismGlassBackdropSource(
    state: PrismGlassBackdropState,
    drawSource: Boolean,
): Modifier = this
    .onGloballyPositioned { state.sourceCoordinates = it }
    .drawWithContent {
        if (size.width <= 0f || size.height <= 0f) {
            drawContent()
            return@drawWithContent
        }
        state.layer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
            this@drawWithContent.drawContent()
        }
        if (drawSource) drawLayer(state.layer)
    }

/** 배경과 유리 컴포넌트의 캡처 순서를 안전하게 분리하는 호스트 */
@Composable
fun PrismGlassBackdropHost(
    modifier: Modifier = Modifier,
    state: PrismGlassBackdropState = rememberPrismGlassBackdropState(),
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.(PrismGlassBackdropState) -> Unit,
) {
    Box(modifier) {
        Box(Modifier.matchParentSize().prismGlassBackdropSource(state), content = background)
        content(state)
    }
}

/** 모든 슬롯 컴포넌트에 적용할 수 있는 배경 블러 유리 표면 */
@Composable
fun PrismGlassBackdropSurface(
    state: PrismGlassBackdropState,
    modifier: Modifier = Modifier,
    style: PrismGlassStyle = PrismGlassDefaults.surfaceStyle(),
    blurRadius: Dp = 18.dp,
    refraction: Float = .16f,
    velocity: Float = 0f,
    clipContent: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val effect = prismBackdropEffect(
        width = measuredSize.width.toFloat(),
        height = measuredSize.height.toFloat(),
        blurRadius = with(density) { blurRadius.toPx() },
        refraction = refraction.coerceIn(0f, .35f),
        velocity = velocity.coerceIn(-1f, 1f),
    )
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .onGloballyPositioned { coordinates = it }
                .onSizeChanged { measuredSize = it }
                .clip(style.shape)
                .graphicsLayer { renderEffect = effect }
                .drawWithContent {
                    val source = state.sourceCoordinates ?: return@drawWithContent
                    val target = coordinates ?: return@drawWithContent
                    val offset = runCatching { source.localPositionOf(target) }.getOrNull()
                        ?: return@drawWithContent
                    translate(-offset.x, -offset.y) {
                        drawLayer(state.layer)
                    }
                },
        )
        if (clipContent) {
            Box(Modifier.matchParentSize().prismGlass(style), content = content)
        } else {
            Box(Modifier.matchParentSize().prismGlass(style))
            content()
        }
    }
}

@Composable
private fun prismBackdropEffect(
    width: Float,
    height: Float,
    blurRadius: Float,
    refraction: Float,
    velocity: Float,
): androidx.compose.ui.graphics.RenderEffect? {
    if (width <= 0f || height <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val safeBlurRadius = blurRadius.coerceAtLeast(0f)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && refraction > 0f) {
        rememberAgslBackdropEffect(width, height, safeBlurRadius, refraction, velocity)
    } else if (safeBlurRadius > 0f) {
        remember(safeBlurRadius) {
            AndroidRenderEffect.createBlurEffect(
                safeBlurRadius,
                safeBlurRadius,
                Shader.TileMode.CLAMP,
            ).asComposeRenderEffect()
        }
    } else {
        null
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun rememberAgslBackdropEffect(
    width: Float,
    height: Float,
    blurRadius: Float,
    refraction: Float,
    velocity: Float,
): androidx.compose.ui.graphics.RenderEffect {
    val shader = remember(width, height, refraction) {
        RuntimeShader(BACKDROP_SHADER).apply {
            setFloatUniform("size", width, height)
            setFloatUniform("refraction", refraction)
        }
    }
    shader.setFloatUniform("velocity", velocity)
    return remember(shader, blurRadius) {
        val lens = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
        if (blurRadius > 0f) {
            val blur = AndroidRenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.CLAMP,
            )
            AndroidRenderEffect.createChainEffect(lens, blur).asComposeRenderEffect()
        } else {
            lens.asComposeRenderEffect()
        }
    }
}

private const val BACKDROP_SHADER = """
    uniform shader content;
    uniform float2 size;
    uniform float refraction;
    uniform float velocity;

    half4 main(float2 position) {
        float2 center = size * 0.5;
        float2 halfSize = max(center, float2(1.0));
        float2 normalized = (position - center) / halfSize;
        float edge = clamp(max(abs(normalized.x), abs(normalized.y)), 0.0, 1.0);
        float lens = smoothstep(0.18, 1.0, edge);
        float speed = min(abs(velocity), 1.0);
        float2 samplePosition = center + (position - center) * (1.0 - refraction * lens * lens);
        samplePosition.x -= sign(velocity) * speed * lens * lens * halfSize.x * 0.18;
        samplePosition.y += sin(normalized.x * 3.14159) * speed * lens * halfSize.y * 0.08;
        samplePosition = clamp(samplePosition, float2(0.0), size - float2(1.0));
        half4 color = content.eval(samplePosition);
        half highlight = half(pow(edge, 7.0) * 0.08);
        return color + half4(highlight, highlight, highlight, 0.0);
    }
"""
