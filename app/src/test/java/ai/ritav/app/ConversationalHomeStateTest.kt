package ai.ritav.app

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalHomeStateTest {

    @Test
    fun compact_home_layout_activates_only_below_the_height_threshold() {
        assertTrue(isCompactHomeViewport(300.dp))
        assertTrue(isCompactHomeViewport((COMPACT_HOME_VIEWPORT_HEIGHT_DP - 1).dp))
        assertFalse(isCompactHomeViewport(COMPACT_HOME_VIEWPORT_HEIGHT_DP.dp))
        assertFalse(isCompactHomeViewport(800.dp))
    }

    @Test
    fun appendConversationMessages_keeps_history_bounded() {
        val messages = mutableListOf<ConversationMessage>()

        repeat(MAX_CONVERSATION_MESSAGES) { index ->
            appendConversationMessages(
                messages = messages,
                userText = "user-$index",
                assistantText = "assistant-$index"
            )
        }

        assertEquals(MAX_CONVERSATION_MESSAGES, messages.size)
        assertEquals("user-50", messages.first().text)
        assertEquals("assistant-99", messages.last().text)
    }

    @Test
    fun appendConversationMessages_preserves_user_then_assistant_order() {
        val messages = mutableListOf<ConversationMessage>()

        appendConversationMessages(messages, "hello", "runtime unavailable")

        assertEquals(
            listOf(
                ConversationMessage("hello", true),
                ConversationMessage("runtime unavailable", false)
            ),
            messages
        )
    }

    @Test
    fun appendConversationMessages_bounds_each_message() {
        val messages = mutableListOf<ConversationMessage>()
        val oversized = "x".repeat(MAX_CONVERSATION_MESSAGE_LENGTH + 100)

        appendConversationMessages(messages, oversized, oversized)

        assertEquals(MAX_CONVERSATION_MESSAGE_LENGTH, messages[0].text.length)
        assertEquals(MAX_CONVERSATION_MESSAGE_LENGTH, messages[1].text.length)
    }
}
