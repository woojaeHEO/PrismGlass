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
}
