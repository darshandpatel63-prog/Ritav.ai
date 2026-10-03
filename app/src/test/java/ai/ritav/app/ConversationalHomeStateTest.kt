package ai.ritav.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationalHomeStateTest {

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
}
