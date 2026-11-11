package com.example.ai

import android.Manifest

object ToolRegistry {

    private val tools = mutableMapOf<String, ToolDefinition>()

    init {
        registerTools()
    }

    private fun register(tool: ToolDefinition) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): ToolDefinition? = tools[name]

    fun getAllTools(): List<ToolDefinition> = tools.values.toList()

    fun getToolsByModule(module: String): List<ToolDefinition> =
        tools.values.filter { it.module == module }

    private fun registerTools() {
        // MODULE A: APPLICATION CONTROL
        register(
            ToolDefinition(
                name = "open_application",
                module = "APP_CONTROL",
                description = "Launch an installed Android application by name or package ID.",
                parameters = listOf(
                    ToolParameter("app_name", "string", "Display name or package name of the app (e.g., 'Chrome', 'WhatsApp', 'com.android.settings')")
                )
            )
        )
        register(
            ToolDefinition(
                name = "list_installed_apps",
                module = "APP_CONTROL",
                description = "Retrieve list of all launchable user and system applications installed on the device.",
                parameters = listOf(
                    ToolParameter("filter", "string", "Optional search keyword to filter app names", required = false)
                )
            )
        )
        register(
            ToolDefinition(
                name = "open_settings_screen",
                module = "APP_CONTROL",
                description = "Directly open specific Android system settings (e.g., 'wifi', 'bluetooth', 'accessibility', 'storage', 'notifications', 'apps').",
                parameters = listOf(
                    ToolParameter("screen", "string", "The settings screen identifier ('wifi', 'bluetooth', 'accessibility', 'notifications', 'battery', 'apps', 'storage', 'general')")
                )
            )
        )

        // MODULE B: SCREEN AUTOMATION (AccessibilityService)
        register(
            ToolDefinition(
                name = "click_screen_element",
                module = "ACCESSIBILITY",
                description = "Locate and click an interactive screen element by text label, content description, or view ID.",
                parameters = listOf(
                    ToolParameter("target_text", "string", "Exact or partial text shown on the element (e.g. 'Install', 'Done', 'Allow')")
                ),
                requiresAccessibility = true
            )
        )
        register(
            ToolDefinition(
                name = "type_text_into_element",
                module = "ACCESSIBILITY",
                description = "Focus on a text field and enter text.",
                parameters = listOf(
                    ToolParameter("target_field", "string", "Label or hint of the target text field", required = false),
                    ToolParameter("text_to_type", "string", "The text to input into the field")
                ),
                requiresAccessibility = true
            )
        )
        register(
            ToolDefinition(
                name = "press_global_key",
                module = "ACCESSIBILITY",
                description = "Trigger system navigation key (e.g., 'back', 'home', 'recents', 'notifications', 'quick_settings', 'lock_screen').",
                parameters = listOf(
                    ToolParameter("action", "string", "One of 'back', 'home', 'recents', 'notifications', 'quick_settings'")
                ),
                requiresAccessibility = true
            )
        )
        register(
            ToolDefinition(
                name = "scroll_screen",
                module = "ACCESSIBILITY",
                description = "Scroll the current active screen forward or backward.",
                parameters = listOf(
                    ToolParameter("direction", "string", "Scroll direction: 'forward' (down) or 'backward' (up)", defaultValue = "forward")
                ),
                requiresAccessibility = true
            )
        )
        register(
            ToolDefinition(
                name = "read_screen_content",
                module = "ACCESSIBILITY",
                description = "Inspect and extract all visible text nodes from the foreground active window.",
                parameters = emptyList(),
                requiresAccessibility = true
            )
        )

        // MODULE C: FILE MANAGEMENT
        register(
            ToolDefinition(
                name = "search_files",
                module = "FILE_SYSTEM",
                description = "Search files in device storage matching an extension, keyword, or path.",
                parameters = listOf(
                    ToolParameter("extension", "string", "File extension filter without dot (e.g. 'apk', 'pdf', 'jpg', 'zip', 'mp4')", required = false),
                    ToolParameter("query", "string", "Filename query string", required = false),
                    ToolParameter("directory", "string", "Directory category: 'downloads', 'documents', 'pictures', 'all'", defaultValue = "downloads")
                )
            )
        )
        register(
            ToolDefinition(
                name = "calculate_storage_usage",
                module = "FILE_SYSTEM",
                description = "Analyze and calculate storage space consumed by specific file types or folders.",
                parameters = listOf(
                    ToolParameter("extension", "string", "File extension to calculate (e.g. 'apk', 'pdf', 'mp4', 'all')", defaultValue = "all"),
                    ToolParameter("directory", "string", "Directory category ('downloads', 'documents', 'dcim', 'internal')", defaultValue = "downloads")
                )
            )
        )
        register(
            ToolDefinition(
                name = "organize_files_by_type",
                module = "FILE_SYSTEM",
                description = "Scan files of a certain extension and move or group them into a specified target folder (e.g. 'Applications', 'Documents').",
                parameters = listOf(
                    ToolParameter("extension", "string", "Extension of files to organize (e.g., 'apk')"),
                    ToolParameter("target_folder_name", "string", "Name of destination folder (e.g., 'Applications')"),
                    ToolParameter("source_directory", "string", "Source folder: 'downloads' or 'documents'", defaultValue = "downloads")
                )
            )
        )
        register(
            ToolDefinition(
                name = "find_duplicate_files",
                module = "FILE_SYSTEM",
                description = "Scan a folder and identify duplicate files using MD5/SHA-256 cryptographic hashes and identical byte lengths.",
                parameters = listOf(
                    ToolParameter("directory", "string", "Folder to scan: 'downloads', 'documents', 'pictures'", defaultValue = "downloads")
                )
            )
        )
        register(
            ToolDefinition(
                name = "create_folder",
                module = "FILE_SYSTEM",
                description = "Create a new subfolder in Downloads or Documents directory.",
                parameters = listOf(
                    ToolParameter("folder_name", "string", "Name of the new folder to create"),
                    ToolParameter("parent_directory", "string", "Parent location ('downloads', 'documents')", defaultValue = "downloads")
                )
            )
        )
        register(
            ToolDefinition(
                name = "delete_file",
                module = "FILE_SYSTEM",
                description = "Safely delete a specified file. Requires explicit user confirmation.",
                parameters = listOf(
                    ToolParameter("file_path", "string", "Exact file path or relative path to delete")
                ),
                isSensitive = true
            )
        )

        // MODULE D: BROWSER AUTOMATION
        register(
            ToolDefinition(
                name = "open_url",
                module = "BROWSER",
                description = "Open a specific web address (URL) in the default Android browser.",
                parameters = listOf(
                    ToolParameter("url", "string", "Full web URL (e.g., 'https://python.org')")
                )
            )
        )
        register(
            ToolDefinition(
                name = "web_search",
                module = "BROWSER",
                description = "Perform a web search query on the user's preferred search engine.",
                parameters = listOf(
                    ToolParameter("query", "string", "Search query terms (e.g. 'latest Python version download')")
                )
            )
        )

        // MODULE E: NOTIFICATION MANAGEMENT
        register(
            ToolDefinition(
                name = "get_recent_notifications",
                module = "NOTIFICATIONS",
                description = "Inspect accessible active notifications and recent alerts on device.",
                parameters = listOf(
                    ToolParameter("filter_app", "string", "Filter by app name or package", required = false)
                ),
                requiresNotificationListener = true
            )
        )
        register(
            ToolDefinition(
                name = "summarize_notifications",
                module = "NOTIFICATIONS",
                description = "Group and generate a clear structured summary of current active notifications.",
                parameters = emptyList(),
                requiresNotificationListener = true
            )
        )

        // MODULE F: COMMUNICATION
        register(
            ToolDefinition(
                name = "draft_sms",
                module = "COMMUNICATION",
                description = "Prepare an SMS message with recipient phone number and prefilled text.",
                parameters = listOf(
                    ToolParameter("phone_number", "string", "Recipient phone number"),
                    ToolParameter("message", "string", "Message content to send")
                ),
                isSensitive = true
            )
        )
        register(
            ToolDefinition(
                name = "dial_phone_number",
                module = "COMMUNICATION",
                description = "Open Android phone dialer with prefilled phone number. Requires confirmation.",
                parameters = listOf(
                    ToolParameter("phone_number", "string", "Phone number to dial")
                ),
                isSensitive = true
            )
        )
        register(
            ToolDefinition(
                name = "search_contacts",
                module = "COMMUNICATION",
                description = "Look up a contact's phone number or details from Android contacts.",
                parameters = listOf(
                    ToolParameter("query", "string", "Name or partial name of contact")
                ),
                requiredPermissions = listOf(Manifest.permission.READ_CONTACTS)
            )
        )
        register(
            ToolDefinition(
                name = "open_whatsapp_chat",
                module = "COMMUNICATION",
                description = "Open WhatsApp conversation or draft message to contact.",
                parameters = listOf(
                    ToolParameter("phone_number", "string", "Phone number with country code (e.g. +1...)", required = false),
                    ToolParameter("message", "string", "Message draft to send", required = false)
                ),
                isSensitive = true
            )
        )
        register(
            ToolDefinition(
                name = "open_telegram_chat",
                module = "COMMUNICATION",
                description = "Open Telegram conversation or share draft text.",
                parameters = listOf(
                    ToolParameter("message", "string", "Message draft to share")
                ),
                isSensitive = true
            )
        )

        // MODULE G: TERMUX INTEGRATION
        register(
            ToolDefinition(
                name = "run_termux_command",
                module = "TERMUX",
                description = "Execute an explicitly approved, allowlisted Termux automation workflow or script.",
                parameters = listOf(
                    ToolParameter("command_id", "string", "Approved command identifier ('disk_usage', 'network_ping', 'device_uptime', 'custom_approved')"),
                    ToolParameter("arguments", "string", "Optional safe arguments", required = false)
                ),
                isSensitive = true
            )
        )

        // MODULE H: PERSONAL MEMORY
        register(
            ToolDefinition(
                name = "save_user_memory",
                module = "MEMORY",
                description = "Store a preference, workflow, shortcut, or fact in local AI memory.",
                parameters = listOf(
                    ToolParameter("key", "string", "Subject or title of the memory"),
                    ToolParameter("category", "string", "Category: 'Preference', 'AppShortcut', 'CustomInstruction', 'Workflow', 'Fact'", defaultValue = "Preference"),
                    ToolParameter("content", "string", "The information to remember")
                )
            )
        )
        register(
            ToolDefinition(
                name = "recall_user_memories",
                module = "MEMORY",
                description = "Search and recall stored preferences or personal automation instructions.",
                parameters = listOf(
                    ToolParameter("query", "string", "Search keyword", required = false)
                )
            )
        )

        // MODULE I: TASK SCHEDULER
        register(
            ToolDefinition(
                name = "schedule_automation_task",
                module = "SCHEDULER",
                description = "Schedule a recurring or one-time automation to run at a specific hour and minute.",
                parameters = listOf(
                    ToolParameter("title", "string", "Descriptive title for schedule"),
                    ToolParameter("hour", "int", "Hour of day (0-23)"),
                    ToolParameter("minute", "int", "Minute of hour (0-59)"),
                    ToolParameter("command", "string", "Natural language automation command to execute"),
                    ToolParameter("schedule_type", "string", "Schedule frequency: 'DAILY', 'WEEKLY', 'ONE_TIME'", defaultValue = "DAILY")
                )
            )
        )
    }
}
