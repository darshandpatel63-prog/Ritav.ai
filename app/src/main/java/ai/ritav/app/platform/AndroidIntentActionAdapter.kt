package ai.ritav.app.platform

import android.content.Context
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AndroidActionAdapter
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ExecutionResult

/**
 * Minimal production Android action adapter.
 *
 * It deliberately exposes only APP_LAUNCH + "open" and requires a deterministic
 * dispatch-state expectation. The common security boundary remains authoritative.
 */
class AndroidIntentActionAdapter(
    dispatcher: AndroidAppLaunchDispatcher
) : AndroidActionAdapter {

    constructor(context: Context) : this(
        ContextAndroidAppLaunchDispatcher(context.applicationContext)
    )

    override fun execute(plan: ActionPlan): ExecutionResult {
        if (!plan.isValid()) {
            return ExecutionResult(false, false, "Action plan is malformed")
        }
        if (plan.capability != Capability.APP_LAUNCH || plan.action != OPEN_ACTION) {
            return ExecutionResult(false, false, "Android adapter does not support this action")
        }
        if (plan.expectedState != LAUNCH_DISPATCHED_STATE) {
            return ExecutionResult(false, false, "Launch action requires dispatch-state verification")
        }

        val dispatched = runCatching {
            dispatcher.dispatchLaunch(plan.appId)
        }.getOrDefault(false)

        return if (dispatched) {
            ExecutionResult(
                success = true,
                verified = false,
                message = "Android launch request dispatched; final UI state is not independently observed",
                observedState = LAUNCH_DISPATCHED_STATE
            )
        } else {
            ExecutionResult(
                success = false,
                verified = false,
                message = "Android launch request could not be dispatched"
            )
        }
    }

    private companion object {
        const val OPEN_ACTION = "open"
        const val LAUNCH_DISPATCHED_STATE = "LAUNCH_DISPATCHED"
    }
}
