package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WibesApplication
import com.example.ai.ExecutionPlan
import com.example.ai.PlanStep
import com.example.ai.PlannerState
import com.example.ai.ToolDefinition
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolRegistry
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.MemoryEntity
import com.example.data.entities.ScheduledAutomationEntity
import com.example.data.entities.TaskHistoryEntity
import com.example.security.ActionGuard
import com.example.security.PermissionStatus
import com.example.security.SensitiveActionPrompt
import com.example.voice.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WibesApplication

    // Command Input
    val commandInput = MutableStateFlow("")

    // Task Planner State
    val plannerState: StateFlow<PlannerState> = app.taskPlanner.plannerState
    val activePlan: StateFlow<ExecutionPlan?> = app.taskPlanner.activePlan

    // Voice
    val speechState: StateFlow<SpeechState> = app.speechInput.speechState
    val isSpeaking: StateFlow<Boolean> = app.speechOutput.isSpeaking

    // Database Flows
    val memories: StateFlow<List<MemoryEntity>> = app.memoryRepository.getAllMemoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskHistory: StateFlow<List<TaskHistoryEntity>> = app.taskRepository.getAllHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledAutomations: StateFlow<List<ScheduledAutomationEntity>> = app.schedulerRepository.getAllAutomationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = app.taskRepository.getAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permissions State
    private val _permissions = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissions: StateFlow<List<PermissionStatus>> = _permissions.asStateFlow()

    // Confirmation Modal for Sensitive Action
    private val _pendingConfirmation = MutableStateFlow<SensitiveActionPrompt?>(null)
    val pendingConfirmation: StateFlow<SensitiveActionPrompt?> = _pendingConfirmation.asStateFlow()

    // Tool Direct Execution Modal
    private val _toolTestingResult = MutableStateFlow<ToolExecutionResult?>(null)
    val toolTestingResult: StateFlow<ToolExecutionResult?> = _toolTestingResult.asStateFlow()

    // Engine settings
    val isRemoteEngineEnabled = MutableStateFlow(app.needleEngine.isRemoteEndpointEnabled)
    val engineEndpoint = MutableStateFlow(app.needleEngine.endpointUrl)
    val isVoiceFeedbackEnabled = MutableStateFlow(app.speechOutput.isVoiceOutputEnabled)
    val speechRate = MutableStateFlow(app.speechOutput.speechRate)

    // UI Feedback Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshPermissions()

        // Observe planner state to trigger TTS voice outcome and pending confirmation
        viewModelScope.launch {
            plannerState.collect { state ->
                when (state) {
                    is PlannerState.WaitingConfirmation -> {
                        val prompt = ActionGuard.getPromptForTool(state.step.toolName, state.step.arguments)
                        _pendingConfirmation.value = prompt
                    }
                    is PlannerState.Completed -> {
                        _pendingConfirmation.value = null
                        if (isVoiceFeedbackEnabled.value) {
                            app.speechOutput.speak(state.summary)
                        }
                    }
                    is PlannerState.Failed -> {
                        _pendingConfirmation.value = null
                        if (isVoiceFeedbackEnabled.value) {
                            app.speechOutput.speak("Automation error: ${state.error}")
                        }
                    }
                    else -> {
                        if (state !is PlannerState.WaitingConfirmation) {
                            _pendingConfirmation.value = null
                        }
                    }
                }
            }
        }
    }

    fun refreshPermissions() {
        _permissions.value = app.permissionManager.getComprehensivePermissionReport()
    }

    fun executeCommand(command: String? = null) {
        val target = command ?: commandInput.value
        if (target.isBlank()) return
        commandInput.value = ""
        app.taskPlanner.executeGoal(target, viewModelScope)
    }

    fun cancelExecution() {
        app.taskPlanner.cancelCurrentTask()
        app.speechOutput.stop()
    }

    fun resetPlanner() {
        app.taskPlanner.reset()
    }

    // Voice recognition
    fun toggleVoiceInput() {
        if (speechState.value is SpeechState.Listening) {
            app.speechInput.stopListening()
        } else {
            app.speechInput.startListening { text ->
                commandInput.value = text
                executeCommand(text)
            }
        }
    }

    fun stopSpeaking() {
        app.speechOutput.stop()
    }

    // Confirmation
    fun confirmSensitiveAction(approved: Boolean) {
        _pendingConfirmation.value = null
        app.taskPlanner.submitUserConfirmation(approved)
    }

    // Direct Tool Execution (for Tool Registry Screen)
    fun testExecuteTool(tool: ToolDefinition, arguments: Map<String, Any>) {
        viewModelScope.launch {
            _toolTestingResult.value = null
            val result = app.automationEngine.executeTool(
                toolName = tool.name,
                arguments = arguments,
                isConfirmedByUser = true
            )
            _toolTestingResult.value = result
        }
    }

    fun clearToolTestResult() {
        _toolTestingResult.value = null
    }

    // Memory CRUD
    fun addMemory(key: String, category: String, content: String) {
        viewModelScope.launch {
            app.memoryRepository.saveMemory(key, category, content)
            _userMessage.value = "Memory saved successfully."
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            app.memoryRepository.deleteMemory(memory)
            _userMessage.value = "Memory deleted."
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            app.memoryRepository.clearAll()
            _userMessage.value = "All memories cleared."
        }
    }

    // Scheduler
    fun addScheduledTask(title: String, hour: Int, minute: Int, command: String, type: String) {
        viewModelScope.launch {
            val entity = ScheduledAutomationEntity(
                title = title,
                hour = hour,
                minute = minute,
                naturalCommand = command,
                scheduleType = type,
                isEnabled = true
            )
            val id = app.schedulerRepository.insert(entity)
            app.scheduler.scheduleAutomation(entity.copy(id = id))
            _userMessage.value = "Scheduled automation created."
        }
    }

    fun toggleSchedule(item: ScheduledAutomationEntity) {
        viewModelScope.launch {
            val updated = item.copy(isEnabled = !item.isEnabled)
            app.schedulerRepository.update(updated)
            if (updated.isEnabled) {
                app.scheduler.scheduleAutomation(updated)
            } else {
                app.scheduler.cancelAutomation(updated.id)
            }
        }
    }

    fun deleteSchedule(item: ScheduledAutomationEntity) {
        viewModelScope.launch {
            app.scheduler.cancelAutomation(item.id)
            app.schedulerRepository.delete(item)
            _userMessage.value = "Schedule deleted."
        }
    }

    // Settings
    fun updateNeedleSettings(remoteEnabled: Boolean, endpoint: String) {
        isRemoteEngineEnabled.value = remoteEnabled
        engineEndpoint.value = endpoint
        app.needleEngine.isRemoteEndpointEnabled = remoteEnabled
        app.needleEngine.endpointUrl = endpoint
        _userMessage.value = "Needle 3 engine configuration saved."
    }

    fun updateVoiceSettings(voiceEnabled: Boolean, rate: Float) {
        isVoiceFeedbackEnabled.value = voiceEnabled
        speechRate.value = rate
        app.speechOutput.isVoiceOutputEnabled = voiceEnabled
        app.speechOutput.speechRate = rate
        app.speechOutput.updateSettings(rate, 1.0f)
    }

    fun clearHistory() {
        viewModelScope.launch {
            app.taskRepository.clearHistory()
            _userMessage.value = "Execution and audit history wiped."
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun openAccessibilitySettings() {
        app.permissionManager.openAccessibilitySettings()
    }

    fun openNotificationSettings() {
        app.permissionManager.openNotificationSettings()
    }

    fun openAppSettings() {
        app.permissionManager.openAppSettings()
    }
}
