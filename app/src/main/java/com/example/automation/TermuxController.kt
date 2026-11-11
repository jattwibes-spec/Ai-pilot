package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.ai.ToolExecutionResult

class TermuxController(private val context: Context) {

    private val allowlistCommands = mapOf(
        "device_uptime" to "uptime",
        "disk_usage" to "df -h",
        "network_ping" to "ping -c 3 8.8.8.8",
        "system_date" to "date"
    )

    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun executeApprovedWorkflow(commandId: String, arguments: String?): ToolExecutionResult {
        if (!isTermuxInstalled()) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Termux (com.termux) is not installed on this device. Install Termux to run local shell workflows.",
                error = "TermuxNotInstalled"
            )
        }

        val baseCommand = allowlistCommands[commandId]
        if (baseCommand == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Command '$commandId' is not in the approved Termux allowlist. For security, arbitrary shell execution is blocked.",
                error = "DisallowedCommand"
            )
        }

        return try {
            val fullCommand = if (!arguments.isNullOrBlank()) "$baseCommand $arguments" else baseCommand
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra("com.termux.execute.command", fullCommand)
                context.startActivity(launchIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Dispatched approved Termux workflow: '$fullCommand'.",
                    data = mapOf("command" to fullCommand, "status" to "dispatched")
                )
            } else {
                ToolExecutionResult(
                    isSuccess = false,
                    message = "Could not initialize Termux execution channel.",
                    error = "LaunchFailed"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Termux workflow error: ${e.message}",
                error = e.localizedMessage
            )
        }
    }
}
