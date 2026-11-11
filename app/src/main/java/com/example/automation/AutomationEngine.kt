package com.example.automation

import android.content.Context
import com.example.ai.ToolDefinition
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolRegistry
import com.example.data.repository.MemoryRepository
import com.example.data.repository.SchedulerRepository
import com.example.data.repository.TaskRepository
import com.example.files.FileManager
import com.example.files.FileOrganizer
import com.example.notifications.NotificationController

class AutomationEngine(
    private val context: Context,
    val appController: AppController,
    val accessibilityController: AccessibilityController,
    val fileManager: FileManager,
    val fileOrganizer: FileOrganizer,
    val browserController: BrowserController,
    val notificationController: NotificationController,
    val communicationController: CommunicationController,
    val termuxController: TermuxController,
    val memoryRepository: MemoryRepository,
    val schedulerRepository: SchedulerRepository,
    val taskRepository: TaskRepository
) {

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any>,
        isConfirmedByUser: Boolean = false
    ): ToolExecutionResult {
        val tool = ToolRegistry.getTool(toolName)
            ?: return ToolExecutionResult(
                isSuccess = false,
                message = "Tool '$toolName' is not registered in Needle Tool Registry.",
                error = "ToolNotFound"
            )

        // Validate required arguments
        for (param in tool.parameters) {
            if (param.required && !arguments.containsKey(param.name) && param.defaultValue == null) {
                return ToolExecutionResult(
                    isSuccess = false,
                    message = "Missing required argument '${param.name}' for tool '$toolName'.",
                    error = "MissingArgument"
                )
            }
        }

        // Check accessibility requirement
        if (tool.requiresAccessibility && !accessibilityController.isServiceEnabled()) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Tool '$toolName' requires Android Accessibility Service enabled.",
                error = "AccessibilityRequired"
            )
        }

        // Check notification listener requirement
        if (tool.requiresNotificationListener && !notificationController.isPermissionGranted()) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Tool '$toolName' requires Notification Access enabled.",
                error = "NotificationAccessRequired"
            )
        }

        val result = dispatchToolExecution(tool, arguments)

        // Log to audit database
        try {
            taskRepository.logAudit(
                toolName = toolName,
                argumentsJson = arguments.toString(),
                isSensitive = tool.isSensitive,
                confirmedByUser = isConfirmedByUser,
                status = if (result.isSuccess) "SUCCESS" else "FAILED",
                verificationResult = result.message
            )
        } catch (_: Exception) {}

        return result
    }

    private suspend fun dispatchToolExecution(tool: ToolDefinition, args: Map<String, Any>): ToolExecutionResult {
        return when (tool.name) {
            // Module A: Apps
            "open_application" -> {
                val appName = args["app_name"]?.toString() ?: ""
                appController.openApplication(appName)
            }
            "list_installed_apps" -> {
                val filter = args["filter"]?.toString()
                appController.listInstalledApps(filter)
            }
            "open_settings_screen" -> {
                val screen = args["screen"]?.toString() ?: "general"
                appController.openSettingsScreen(screen)
            }

            // Module B: Screen / Accessibility
            "click_screen_element" -> {
                val text = args["target_text"]?.toString() ?: ""
                accessibilityController.clickElement(text)
            }
            "type_text_into_element" -> {
                val text = args["text_to_type"]?.toString() ?: ""
                val field = args["target_field"]?.toString()
                accessibilityController.typeText(text, field)
            }
            "press_global_key" -> {
                val action = args["action"]?.toString() ?: "back"
                accessibilityController.triggerGlobalAction(action)
            }
            "scroll_screen" -> {
                val dir = args["direction"]?.toString() ?: "forward"
                accessibilityController.scroll(dir.equals("forward", true))
            }
            "read_screen_content" -> {
                accessibilityController.readScreenContent()
            }

            // Module C: Files
            "search_files" -> {
                val ext = args["extension"]?.toString()
                val query = args["query"]?.toString()
                val dir = args["directory"]?.toString() ?: "downloads"
                fileManager.searchFiles(ext, query, dir)
            }
            "calculate_storage_usage" -> {
                val ext = args["extension"]?.toString() ?: "all"
                val dir = args["directory"]?.toString() ?: "downloads"
                fileManager.calculateStorageUsage(ext, dir)
            }
            "organize_files_by_type" -> {
                val ext = args["extension"]?.toString() ?: "apk"
                val target = args["target_folder_name"]?.toString() ?: "Applications"
                val source = args["source_directory"]?.toString() ?: "downloads"
                fileOrganizer.organizeFiles(ext, target, source)
            }
            "find_duplicate_files" -> {
                val dir = args["directory"]?.toString() ?: "downloads"
                fileManager.findDuplicateFiles(dir)
            }
            "create_folder" -> {
                val folder = args["folder_name"]?.toString() ?: "NewFolder"
                val parent = args["parent_directory"]?.toString() ?: "downloads"
                fileManager.createFolder(folder, parent)
            }
            "delete_file" -> {
                val path = args["file_path"]?.toString() ?: ""
                fileManager.deleteFile(path)
            }

            // Module D: Browser
            "open_url" -> {
                val url = args["url"]?.toString() ?: ""
                browserController.openUrl(url)
            }
            "web_search" -> {
                val q = args["query"]?.toString() ?: ""
                browserController.webSearch(q)
            }

            // Module E: Notifications
            "get_recent_notifications" -> {
                val filter = args["filter_app"]?.toString()
                notificationController.getRecentNotifications(filter)
            }
            "summarize_notifications" -> {
                notificationController.summarizeNotifications()
            }

            // Module F: Communication
            "draft_sms" -> {
                val phone = args["phone_number"]?.toString() ?: ""
                val msg = args["message"]?.toString() ?: ""
                communicationController.draftSms(phone, msg)
            }
            "dial_phone_number" -> {
                val phone = args["phone_number"]?.toString() ?: ""
                communicationController.dialPhoneNumber(phone)
            }
            "search_contacts" -> {
                val q = args["query"]?.toString() ?: ""
                communicationController.searchContacts(q)
            }
            "open_whatsapp_chat" -> {
                val phone = args["phone_number"]?.toString()
                val msg = args["message"]?.toString()
                communicationController.openWhatsAppChat(phone, msg)
            }
            "open_telegram_chat" -> {
                val msg = args["message"]?.toString()
                communicationController.openTelegramChat(msg)
            }

            // Module G: Termux
            "run_termux_command" -> {
                val cmdId = args["command_id"]?.toString() ?: "device_uptime"
                val extraArgs = args["arguments"]?.toString()
                termuxController.executeApprovedWorkflow(cmdId, extraArgs)
            }

            // Module H: Memory
            "save_user_memory" -> {
                val k = args["key"]?.toString() ?: "Note"
                val cat = args["category"]?.toString() ?: "Preference"
                val content = args["content"]?.toString() ?: ""
                val id = memoryRepository.saveMemory(k, cat, content)
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Saved memory '$k' (ID #$id) to persistent storage.",
                    data = mapOf("id" to id)
                )
            }
            "recall_user_memories" -> {
                val q = args["query"]?.toString() ?: ""
                val list = if (q.isBlank()) memoryRepository.getAllMemories() else memoryRepository.searchMemories(q)
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Recalled ${list.size} stored memory item(s).",
                    data = mapOf("count" to list.size),
                    rawOutput = list.joinToString("\n\n") { "• [${it.category}] ${it.key}: ${it.content}" }
                )
            }

            // Module I: Scheduler
            "schedule_automation_task" -> {
                val title = args["title"]?.toString() ?: "Scheduled Task"
                val hour = (args["hour"] as? Number)?.toInt() ?: 8
                val min = (args["minute"] as? Number)?.toInt() ?: 0
                val cmd = args["command"]?.toString() ?: ""
                val schedType = args["schedule_type"]?.toString() ?: "DAILY"

                val autoId = schedulerRepository.insert(
                    com.example.data.entities.ScheduledAutomationEntity(
                        title = title,
                        scheduleType = schedType,
                        hour = hour,
                        minute = min,
                        naturalCommand = cmd,
                        isEnabled = true
                    )
                )
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Created scheduled automation '$title' at %02d:%02d (%s).".format(hour, min, schedType),
                    data = mapOf("id" to autoId)
                )
            }

            else -> ToolExecutionResult(
                isSuccess = false,
                message = "Handler not implemented for tool '${tool.name}'.",
                error = "UnimplementedHandler"
            )
        }
    }
}
