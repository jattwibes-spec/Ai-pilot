package com.example.ai

enum class StepStatus {
    PENDING,
    RUNNING,
    CONFIRMATION_REQUIRED,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    CANCELLED
}

data class ToolParameter(
    val name: String,
    val type: String, // "string", "int", "boolean", "array"
    val description: String,
    val required: Boolean = true,
    val defaultValue: Any? = null
)

data class ToolDefinition(
    val name: String,
    val module: String, // "APP_CONTROL", "ACCESSIBILITY", "FILE_SYSTEM", "BROWSER", "NOTIFICATIONS", "COMMUNICATION", "TERMUX", "MEMORY", "SCHEDULER"
    val description: String,
    val parameters: List<ToolParameter>,
    val requiredPermissions: List<String> = emptyList(),
    val requiresAccessibility: Boolean = false,
    val requiresNotificationListener: Boolean = false,
    val isSensitive: Boolean = false
)

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val rawOutput: String = "",
    val error: String? = null
)

data class PlanStep(
    val id: Int,
    val toolName: String,
    val description: String,
    val arguments: Map<String, Any>,
    var status: StepStatus = StepStatus.PENDING,
    var resultMessage: String? = null,
    var executionDurationMs: Long = 0L,
    var isVerified: Boolean = false
)

data class ExecutionPlan(
    val id: String,
    val userGoal: String,
    val steps: MutableList<PlanStep>,
    val explanation: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class NeedleParsedIntent(
    val goal: String,
    val primaryModule: String,
    val steps: List<PlanStep>,
    val explanation: String
)
