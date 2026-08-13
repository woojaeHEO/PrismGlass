package io.github.woojaeheo.prismglass

import android.os.Build
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PrismGlassConfigurationTest {
    @Test
    fun `잘못된 광학 값은 안전한 범위로 보정한다`() {
        val negative = PrismGlassOptics((-4).dp, Float.NaN).normalized()
        val excessive = PrismGlassOptics(12.dp, 4f).normalized()

        assertEquals(0.dp, negative.blurRadius)
        assertEquals(0f, negative.refraction)
        assertEquals(.35f, excessive.refraction)
    }

    @Test
    fun `자동 품질은 플랫폼 기능에 맞는 효과를 선택한다`() {
        val optics = PrismGlassOptics(12.dp, .2f)

        assertEquals(ResolvedOptics(0.dp, 0f), optics.resolve(Build.VERSION_CODES.R))
        assertEquals(ResolvedOptics(12.dp, 0f), optics.resolve(Build.VERSION_CODES.S))
        assertEquals(ResolvedOptics(12.dp, .2f), optics.resolve(Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    fun `강제 굴절은 미지원 플랫폼에서 블러로 낮춘다`() {
        val optics = PrismGlassOptics(9.dp, .3f, PrismGlassQuality.Refractive)

        assertEquals(ResolvedOptics(9.dp, 0f), optics.resolve(Build.VERSION_CODES.S))
    }

    @Test
    fun `모션 크기는 안전한 범위를 유지한다`() {
        assertEquals(1f, PrismGlassMotionSpec(pressedScale = Float.NaN).safePressedScale)
        assertEquals(1.8f, PrismGlassMotionSpec(pressedScale = 8f).safePressedScale)
        assertEquals(.8f, PrismGlassPressSpec(pressedScale = .2f).safePressedScale)
    }

    @Test
    fun `장식 투명도는 유효 범위로 보정한다`() {
        val decoration = PrismGlassDecoration(
            topHighlightAlpha = -1f,
            tintStartAlpha = 3f,
            tintEndAlpha = Float.NaN,
        ).normalized()

        assertEquals(0f, decoration.topHighlightAlpha)
        assertEquals(1f, decoration.tintStartAlpha)
        assertEquals(0f, decoration.tintEndAlpha)
    }
}
