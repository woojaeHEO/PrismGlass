package io.github.woojaeheo.prismglass

import org.junit.Assert.assertEquals
import org.junit.Test

class PrismGlassNavigationTest {
    @Test
    fun `선택 항목의 위치를 반환한다`() {
        assertEquals(2, listOf("a", "b", "c").resolvedSelectedIndex("c"))
    }

    @Test
    fun `없는 항목은 첫 위치로 보정한다`() {
        assertEquals(0, listOf("a", "b").resolvedSelectedIndex("c"))
    }

    @Test
    fun `드래그 위치를 탭 위치로 변환한다`() {
        assertEquals(0, resolvedIndexForPosition(0f, 400f, 4, rightToLeft = false))
        assertEquals(2, resolvedIndexForPosition(260f, 400f, 4, rightToLeft = false))
        assertEquals(3, resolvedIndexForPosition(400f, 400f, 4, rightToLeft = false))
    }

    @Test
    fun `오른쪽에서 왼쪽 배치의 드래그 위치를 반전한다`() {
        assertEquals(3, resolvedIndexForPosition(0f, 400f, 4, rightToLeft = true))
        assertEquals(1, resolvedIndexForPosition(260f, 400f, 4, rightToLeft = true))
        assertEquals(0, resolvedIndexForPosition(400f, 400f, 4, rightToLeft = true))
    }

    @Test
    fun `드래그 속도에 따라 렌즈가 제한 범위 안에서 늘어난다`() {
        assertEquals(1f, stretchForVelocity(0f, 100f), .001f)
        assertEquals(1.2f, stretchForVelocity(150f, 100f), .001f)
        assertEquals(1.52f, stretchForVelocity(10_000f, 100f), .001f)
    }

    @Test
    fun `잘못된 크기와 속도는 늘림 없이 보정한다`() {
        assertEquals(1f, stretchForVelocity(Float.NaN, 100f), .001f)
        assertEquals(1f, stretchForVelocity(100f, 0f), .001f)
    }
}
