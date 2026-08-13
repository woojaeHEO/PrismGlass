package io.github.woojaeheo.prismglass

import android.os.Build
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** 글라스 렌더링 성능 단계 */
enum class PrismGlassQuality {
    Automatic,
    Translucent,
    Blur,
    Refractive,
}

/** 유리 표면 장식 설정 */
@Immutable
data class PrismGlassDecoration(
    val topHighlightAlpha: Float = .24f,
    val tintStartAlpha: Float = .16f,
    val tintEndAlpha: Float = .07f,
    val centerGlowAlpha: Float = .17f,
    val ambientShadowAlpha: Float = .18f,
    val spotShadowAlpha: Float = .28f,
    val highlightEdgeAlpha: Float = .72f,
    val primaryEdgeAlpha: Float = .62f,
    val secondaryEdgeAlpha: Float = .54f,
    val spotShadowColor: Color = Color.Black,
) {
    /** 안전한 장식 설정 */
    fun normalized(): PrismGlassDecoration = copy(
        topHighlightAlpha = topHighlightAlpha.safeAlpha(),
        tintStartAlpha = tintStartAlpha.safeAlpha(),
        tintEndAlpha = tintEndAlpha.safeAlpha(),
        centerGlowAlpha = centerGlowAlpha.safeAlpha(),
        ambientShadowAlpha = ambientShadowAlpha.safeAlpha(),
        spotShadowAlpha = spotShadowAlpha.safeAlpha(),
        highlightEdgeAlpha = highlightEdgeAlpha.safeAlpha(),
        primaryEdgeAlpha = primaryEdgeAlpha.safeAlpha(),
        secondaryEdgeAlpha = secondaryEdgeAlpha.safeAlpha(),
    )
}

/** 글라스 광학 설정 */
@Immutable
data class PrismGlassOptics(
    val blurRadius: Dp = 18.dp,
    val refraction: Float = .16f,
    val quality: PrismGlassQuality = PrismGlassQuality.Automatic,
) {
    /** 안전한 광학 설정 */
    fun normalized(): PrismGlassOptics = copy(
        blurRadius = blurRadius.coerceAtLeast(0.dp),
        refraction = refraction.takeIf(Float::isFinite)?.coerceIn(0f, MAX_REFRACTION) ?: 0f,
    )
}

/** 네비게이션 모션 설정 */
@Immutable
data class PrismGlassMotionSpec(
    val pressedScale: Float = 78f / 56f,
    val positionSpring: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1_000f,
        visibilityThreshold = .001f,
    ),
    val velocitySpring: SpringSpec<Float> = spring(
        dampingRatio = .5f,
        stiffness = 300f,
        visibilityThreshold = .01f,
    ),
    val horizontalScaleSpring: SpringSpec<Float> = spring(
        dampingRatio = .6f,
        stiffness = 250f,
        visibilityThreshold = .001f,
    ),
    val verticalScaleSpring: SpringSpec<Float> = spring(
        dampingRatio = .7f,
        stiffness = 250f,
        visibilityThreshold = .001f,
    ),
) {
    /** 안전한 눌림 크기 */
    val safePressedScale: Float
        get() = pressedScale.takeIf(Float::isFinite)?.coerceIn(1f, MAX_PRESSED_SCALE) ?: 1f
}

/** 상호작용 표면 모션 설정 */
@Immutable
data class PrismGlassPressSpec(
    val pressedScale: Float = .965f,
    val spring: SpringSpec<Float> = spring(),
) {
    /** 안전한 눌림 크기 */
    val safePressedScale: Float
        get() = pressedScale.takeIf(Float::isFinite)?.coerceIn(MIN_SURFACE_SCALE, 1f) ?: 1f
}

/** 선택 위치 결정 정책 */
fun interface PrismGlassSelectionPolicy {
    fun targetIndex(position: Float, itemCount: Int): Int

    companion object {
        /** 가장 가까운 항목 선택 */
        val Nearest = PrismGlassSelectionPolicy { position, itemCount ->
            if (itemCount <= 0 || !position.isFinite()) 0 else position.roundToInt().coerceIn(0, itemCount - 1)
        }
    }
}

/** 네비게이션 관찰 상태 */
@Stable
class PrismGlassNavigationState internal constructor(initialIndex: Int) {
    var currentIndex by mutableIntStateOf(initialIndex.coerceAtLeast(0))
        internal set
    var position by mutableFloatStateOf(initialIndex.coerceAtLeast(0).toFloat())
        internal set
    var velocity by mutableFloatStateOf(0f)
        internal set
    var isDragging by androidx.compose.runtime.mutableStateOf(false)
        internal set
}

/** 네비게이션 상태 생성 */
@Composable
fun rememberPrismGlassNavigationState(initialIndex: Int = 0): PrismGlassNavigationState =
    remember { PrismGlassNavigationState(initialIndex) }

internal data class ResolvedOptics(
    val blurRadius: Dp,
    val refraction: Float,
)

internal fun PrismGlassOptics.resolve(sdkInt: Int = Build.VERSION.SDK_INT): ResolvedOptics {
    val safe = normalized()
    return when (quality) {
        PrismGlassQuality.Translucent -> ResolvedOptics(0.dp, 0f)
        PrismGlassQuality.Blur -> ResolvedOptics(safe.blurRadius, 0f)
        PrismGlassQuality.Refractive -> if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            ResolvedOptics(safe.blurRadius, safe.refraction)
        } else {
            ResolvedOptics(safe.blurRadius, 0f)
        }
        PrismGlassQuality.Automatic -> when {
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> ResolvedOptics(safe.blurRadius, safe.refraction)
            sdkInt >= Build.VERSION_CODES.S -> ResolvedOptics(safe.blurRadius, 0f)
            else -> ResolvedOptics(0.dp, 0f)
        }
    }
}

private const val MAX_REFRACTION = .35f
private const val MAX_PRESSED_SCALE = 1.8f
private const val MIN_SURFACE_SCALE = .8f

private fun Float.safeAlpha(): Float = takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
