package ai.ritav.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun vertical_stack_positions_stay_in_bounds_and_do_not_overlap_near_center() {
        val items = (0..2).map { index ->
            linearPlacement(
                index = index.toFloat(),
                count = 3,
                centerX = 29f,
                centerY = 29f,
                width = 360f,
                height = 360f,
                itemPx = 48f,
                buttonX = 150f,
                buttonY = 150f,
                margin = 12f
            )
        }

        assertTrue(items.all { it.x >= 12f && it.x <= 300f })
        assertTrue(items.all { it.y >= 12f && it.y <= 300f })
        assertTrue(items.zipWithNext().all { (first, second) -> second.y - first.y >= 56f })
    }

    @Test
    fun horizontal_row_positions_stay_in_bounds_and_do_not_overlap_on_short_viewport() {
        val items = (0..2).map { index ->
            linearPlacement(
                index = index.toFloat(),
                count = 3,
                centerX = 29f,
                centerY = 29f,
                width = 360f,
                height = 360f,
                itemPx = 48f,
                buttonX = 150f,
                buttonY = 5f,
                margin = 12f
            )
        }

        assertTrue(items.all { it.x >= 12f && it.x <= 300f })
        assertTrue(items.all { it.y >= 12f && it.y <= 300f })
        assertTrue(items.zipWithNext().all { (first, second) -> second.x - first.x >= 56f })
    }
}
