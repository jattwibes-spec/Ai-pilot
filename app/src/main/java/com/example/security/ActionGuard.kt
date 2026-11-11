package com.example.security

data class SensitiveActionPrompt(
    val toolName: String,
    val title: String,
    val description: String,
    val riskLevel: String, // "HIGH", "MEDIUM"
    val parametersSummary: String
)

object ActionGuard {

    private val sensitiveTools = mapOf(
        "delete_file" to Pair("Delete File", "Permanently deletes a file from device storage."),
        "dial_phone_number" to Pair("Phone Call", "Initiates an outgoing phone call via Android dialer."),
        "draft_sms" to Pair("Send SMS", "Prepares an SMS text message to an external recipient."),
        "open_whatsapp_chat" to Pair("WhatsApp Communication", "Prepares a chat message via WhatsApp."),
        "open_telegram_chat" to Pair("Telegram Communication", "Prepares a chat message via Telegram."),
        "run_termux_command" to Pair("Termux Workflow", "Executes an approved shell automation script via Termux.")
    )

    fun isActionSensitive(toolName: String): Boolean = sensitiveTools.containsKey(toolName)

    fun getPromptForTool(toolName: String, arguments: Map<String, Any>): SensitiveActionPrompt {
        val info = sensitiveTools[toolName] ?: Pair("Sensitive Operation", "Action requires explicit user confirmation.")
        val risk = if (toolName == "delete_file" || toolName == "run_termux_command") "HIGH" else "MEDIUM"
        val paramsText = arguments.entries.joinToString(", ") { "${it.key} = ${it.value}" }

        return SensitiveActionPrompt(
            toolName = toolName,
            title = info.first,
            description = info.second,
            riskLevel = risk,
            parametersSummary = paramsText
        )
    }
}
