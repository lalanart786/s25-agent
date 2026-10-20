package com.agent.s25.domain.tools

enum class ExecutionTier {
    AUTO_EXECUTE,           // Read-only, safe
    REQUIRE_CONFIRMATION    // State-changing, write, network external
}

object SafetyGate {
    private val READ_ONLY_PATTERNS = listOf(
        "get_", "read_", "search_", "query_", "inspect_", "check_", "list_"
    )

    fun evaluate(toolName: String): ExecutionTier {
        val lower = toolName.lowercase()
        for (pattern in READ_ONLY_PATTERNS) {
            if (lower.startsWith(pattern)) {
                return ExecutionTier.AUTO_EXECUTE
            }
        }
        return ExecutionTier.REQUIRE_CONFIRMATION
    }
}
