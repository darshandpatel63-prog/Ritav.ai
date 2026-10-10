package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRuntimeStateTest {
    @Test fun listenerReceivesInitialAndUpdatedSnapshotUntilClosed() {
        val store = TaskRuntimeStateStore { 1234L }
        val observed = mutableListOf<TaskRuntimeSnapshot>()
        val subscription = store.observe { observed += it }

        store.update(
            state = TaskRuntimeState.EXECUTING,
            taskId = "task-1",
            taskName = "APP_LAUNCH: open",
            currentStep = "Dispatch",
            summary = "Dispatching"
        )

        assertEquals(2, observed.size)
        assertEquals(TaskRuntimeState.IDLE, observed[0].state)
        assertEquals(TaskRuntimeState.EXECUTING, observed[1].state)
        assertEquals(1234L, observed[1].updatedAtEpochMillis)

        subscription.close()
        store.update(state = TaskRuntimeState.VERIFYING)

        assertEquals(2, observed.size)
    }

    @Test fun stateTextIsBoundedBeforeExposure() {
        val store = TaskRuntimeStateStore { 42L }
        store.update(
            state = TaskRuntimeState.BLOCKED,
            taskId = "x".repeat(300),
            taskName = "n".repeat(700),
            currentStep = "s".repeat(700),
            summary = "m".repeat(700)
        )

        val snapshot = store.snapshot()
        assertTrue(snapshot.taskId!!.length <= 256)
        assertTrue(snapshot.taskName!!.length <= 512)
        assertTrue(snapshot.currentStep!!.length <= 512)
        assertTrue(snapshot.summary!!.length <= 512)
    }
}
