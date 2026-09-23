package ai.ritav.app.platform

import android.view.accessibility.AccessibilityNodeInfo
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.UntrustedContent

/**
 * Bounded text-only extraction from a user-enabled AccessibilityService.
 * Output remains untrusted app content and must pass AgentRequest's
 * ModelContextBoundary before model/agent reasoning.
 */
internal class RitavAccessibilityContextExtractor {
    fun extract(root: AccessibilityNodeInfo, packageName: String): UntrustedContent? {
        if (!isValidPackageName(packageName)) return null
        if (root.packageName?.toString() != packageName) return null

        val builder = StringBuilder()
        val visited = IdentityHashSet<AccessibilityNodeInfo>()

        return runCatching {
            visit(root, packageName, 0, builder, visited)
            if (builder.isEmpty()) null else UntrustedContent(
                text = builder.toString(),
                source = "android-accessibility:" + packageName,
                trustLevel = ContentTrustLevel.APP_CONTENT
            )
        }.getOrNull()
    }

    private fun visit(
        node: AccessibilityNodeInfo,
        packageName: String,
        depth: Int,
        builder: StringBuilder,
        visited: IdentityHashSet<AccessibilityNodeInfo>
    ) {
        if (depth > MAX_DEPTH || visited.size >= MAX_NODES || builder.length >= MAX_TEXT_LENGTH) return
        if (!visited.add(node)) return
        if (node.packageName?.toString() != packageName) return

        append(node.text?.toString(), builder)
        append(node.contentDescription?.toString(), builder)

        val childCount = node.childCount.coerceAtMost(MAX_CHILDREN)
        for (index in 0 until childCount) {
            val child = node.getChild(index) ?: continue
            visit(child, packageName, depth + 1, builder, visited)
        }
    }

    private fun append(value: String?, builder: StringBuilder) {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return
        val remaining = MAX_TEXT_LENGTH - builder.length
        if (remaining <= 0) return
        if (builder.isNotEmpty()) builder.append('\n')
        builder.append(text.take(remaining))
    }

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH

    private companion object {
        const val MAX_DEPTH = 12
        const val MAX_NODES = 128
        const val MAX_CHILDREN = 32
        const val MAX_TEXT_LENGTH = 8_192
        const val MAX_PACKAGE_NAME_LENGTH = 256
    }

    private class IdentityHashSet<T> {
        private val backing = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<T, Boolean>()
        )
        val size: Int get() = backing.size
        fun add(value: T): Boolean = backing.add(value)
    }
}
