package ai.ritav.app.core.security

/** Read-only product/UI status projection for the currently active or most recent execution task. */
interface TaskRuntimeStatePort {
    fun snapshot(): TaskRuntimeSnapshot
    fun observe(listener: (TaskRuntimeSnapshot) -> Unit): TaskRuntimeStateSubscription
}

fun interface TaskRuntimeStateSubscription {
    fun close()
}

enum class TaskRuntimeState {
    IDLE,
    BLOCKED,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED_SAFELY,
    STOPPED
}

data class TaskRuntimeSnapshot(
    val state: TaskRuntimeState = TaskRuntimeState.IDLE,
    val taskId: String? = null,
    val taskName: String? = null,
    val currentStep: String? = null,
    val summary: String? = null,
    val updatedAtEpochMillis: Long = 0L
)

internal class TaskRuntimeStateStore(
    private val clock: () -> Long = { System.currentTimeMillis() }
) : TaskRuntimeStatePort {
    private val lock = Any()
    private val listeners = LinkedHashSet<(TaskRuntimeSnapshot) -> Unit>()
    private var current = TaskRuntimeSnapshot()

    override fun snapshot(): TaskRuntimeSnapshot = synchronized(lock) { current }

    override fun observe(
        listener: (TaskRuntimeSnapshot) -> Unit
    ): TaskRuntimeStateSubscription {
        val initial = synchronized(lock) {
            listeners += listener
            current
        }
        runCatching { listener(initial) }
        return TaskRuntimeStateSubscription {
            synchronized(lock) {
                listeners.remove(listener)
            }
        }
    }

    internal fun update(
        state: TaskRuntimeState,
        taskId: String? = null,
        taskName: String? = null,
        currentStep: String? = null,
        summary: String? = null
    ) {
        val snapshot: TaskRuntimeSnapshot
        val observers: List<(TaskRuntimeSnapshot) -> Unit>
        val now = runCatching { clock() }.getOrElse { 0L }.coerceAtLeast(0L)

        synchronized(lock) {
            snapshot = TaskRuntimeSnapshot(
                state = state,
                taskId = taskId?.trim()?.take(MAX_ID_LENGTH),
                taskName = taskName?.trim()?.take(MAX_TEXT_LENGTH),
                currentStep = currentStep?.trim()?.take(MAX_TEXT_LENGTH),
                summary = summary?.trim()?.take(MAX_TEXT_LENGTH),
                updatedAtEpochMillis = now
            )
            current = snapshot
            observers = listeners.toList()
        }

        observers.forEach { observer ->
            runCatching { observer(snapshot) }
        }
    }

    internal fun clear() {
        update(TaskRuntimeState.IDLE)
    }

    private companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_TEXT_LENGTH = 512
    }
}
