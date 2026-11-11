package com.example.ai

import android.util.Log
import com.example.automation.AutomationEngine
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PlannerState {
    object Idle : PlannerState()
    data class Planning(val userGoal: String) : PlannerState()
    data class Executing(val plan: ExecutionPlan, val currentStep: Int) : PlannerState()
    data class WaitingConfirmation(val plan: ExecutionPlan, val step: PlanStep) : PlannerState()
    data class Completed(val plan: ExecutionPlan, val summary: String) : PlannerState()
    data class Failed(val plan: ExecutionPlan, val failedStep: PlanStep, val error: String) : PlannerState()
    data class Cancelled(val plan: ExecutionPlan) : PlannerState()
}

class TaskPlanner(
    private val needleEngine: NeedleEngine,
    private val automationEngine: AutomationEngine,
    private val taskRepository: TaskRepository
) {

    private val tag = "TaskPlanner"
    private var executionJob: Job? = null

    private val _plannerState = MutableStateFlow<PlannerState>(PlannerState.Idle)
    val plannerState: StateFlow<PlannerState> = _plannerState.asStateFlow()

    private val _activePlan = MutableStateFlow<ExecutionPlan?>(null)
    val activePlan: StateFlow<ExecutionPlan?> = _activePlan.asStateFlow()

    private var confirmationCallback: ((Boolean) -> Unit)? = null

    fun executeGoal(userGoal: String, scope: CoroutineScope) {
        cancelCurrentTask()

        executionJob = scope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            _plannerState.value = PlannerState.Planning(userGoal)

            val plan = try {
                needleEngine.planTask(userGoal)
            } catch (e: Exception) {
                Log.e(tag, "Planning error: ${e.message}", e)
                val dummyPlan = ExecutionPlan(
                    id = "err",
                    userGoal = userGoal,
                    steps = mutableListOf(),
                    explanation = "Failed to decompose plan: ${e.message}"
                )
                _plannerState.value = PlannerState.Failed(
                    dummyPlan,
                    PlanStep(0, "planner", "Error", emptyMap()),
                    e.localizedMessage ?: "Unknown error"
                )
                return@launch
            }

            _activePlan.value = plan
            var completedSteps = 0
            var finalStatus = "COMPLETED"
            var failureMessage = ""

            for (i in plan.steps.indices) {
                val step = plan.steps[i]
                _plannerState.value = PlannerState.Executing(plan, i + 1)
                step.status = StepStatus.RUNNING

                // Check sensitive action confirmation
                val toolDef = ToolRegistry.getTool(step.toolName)
                if (toolDef?.isSensitive == true) {
                    step.status = StepStatus.CONFIRMATION_REQUIRED
                    _plannerState.value = PlannerState.WaitingConfirmation(plan, step)

                    val confirmed = awaitUserConfirmation()
                    if (!confirmed) {
                        step.status = StepStatus.CANCELLED
                        step.resultMessage = "User declined confirmation for sensitive action."
                        finalStatus = "CANCELLED"
                        break
                    }
                    step.status = StepStatus.RUNNING
                    _plannerState.value = PlannerState.Executing(plan, i + 1)
                }

                val stepStart = System.currentTimeMillis()
                val result = try {
                    automationEngine.executeTool(
                        step.toolName,
                        step.arguments,
                        isConfirmedByUser = toolDef?.isSensitive == true
                    )
                } catch (e: CancellationException) {
                    step.status = StepStatus.CANCELLED
                    finalStatus = "CANCELLED"
                    throw e
                } catch (e: Exception) {
                    com.example.ai.ToolExecutionResult(
                        isSuccess = false,
                        message = "Execution failed: ${e.message}",
                        error = e.localizedMessage
                    )
                }

                step.executionDurationMs = System.currentTimeMillis() - stepStart
                step.resultMessage = result.message

                if (result.isSuccess) {
                    step.status = StepStatus.SUCCEEDED
                    step.isVerified = true
                    completedSteps++
                    // Small breathing pause between multi-step automation
                    delay(300)
                } else {
                    step.status = StepStatus.FAILED
                    finalStatus = "FAILED"
                    failureMessage = result.message
                    _plannerState.value = PlannerState.Failed(plan, step, failureMessage)
                    break
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            val summary = if (finalStatus == "COMPLETED") {
                "Completed ${plan.steps.size} step(s) successfully in ${totalDuration}ms. ${plan.steps.lastOrNull()?.resultMessage ?: ""}"
            } else if (finalStatus == "CANCELLED") {
                "Task execution was cancelled by user."
            } else {
                "Task halted at step ${completedSteps + 1}/${plan.steps.size}: $failureMessage"
            }

            // Record to persistent database
            try {
                taskRepository.recordTask(
                    taskId = plan.id,
                    prompt = userGoal,
                    status = finalStatus,
                    totalSteps = plan.steps.size,
                    completedSteps = completedSteps,
                    executionTimeMs = totalDuration,
                    resultSummary = summary
                )
            } catch (_: Exception) {}

            if (finalStatus == "COMPLETED") {
                _plannerState.value = PlannerState.Completed(plan, summary)
            } else if (finalStatus == "CANCELLED") {
                _plannerState.value = PlannerState.Cancelled(plan)
            }
        }
    }

    fun submitUserConfirmation(approved: Boolean) {
        confirmationCallback?.invoke(approved)
        confirmationCallback = null
    }

    private suspend fun awaitUserConfirmation(): Boolean = withContext(Dispatchers.Default) {
        kotlin.coroutines.suspendCoroutine { cont ->
            confirmationCallback = { approved ->
                cont.resumeWith(Result.success(approved))
            }
        }
    }

    fun cancelCurrentTask() {
        executionJob?.cancel()
        executionJob = null
        confirmationCallback?.invoke(false)
        confirmationCallback = null
        val currentPlan = _activePlan.value
        if (currentPlan != null && _plannerState.value is PlannerState.Executing) {
            _plannerState.value = PlannerState.Cancelled(currentPlan)
        } else {
            _plannerState.value = PlannerState.Idle
        }
    }

    fun reset() {
        cancelCurrentTask()
        _plannerState.value = PlannerState.Idle
        _activePlan.value = null
    }
}
