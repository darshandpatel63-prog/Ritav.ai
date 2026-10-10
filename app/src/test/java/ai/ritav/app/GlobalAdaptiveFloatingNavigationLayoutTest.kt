package ai.ritav.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalAdaptiveFloatingNavigationLayoutTest {
    @Test
    fun vertical_stack_capacity_uses_height_not_width() {
        assertEquals(
            3,
            linearMenuAxisCapacity(
                horizontalLinear = true,
                width = 360f,
                height = 240f,
                itemPx = 48f,
                margin = 12f
            )
        )
    }

    @Test
    fun horizontal_row_capacity_uses_width_not_height() {
        assertEquals(
            3,
            linearMenuAxisCapacity(
                horizontalLinear = false,
                width = 240f,
                height = 360f,
                itemPx = 48f,
                margin = 12f
            )
        )
    }

    @Test
    fun minimum_viable_axis_keeps_one_item_slot() {
        assertEquals(
            1,
            linearMenuAxisCapacity(
                horizontalLinear = false,
                width = 80f,
                height = 180f,
                itemPx = 48f,
                margin = 12f
            )
        )
    }
}
