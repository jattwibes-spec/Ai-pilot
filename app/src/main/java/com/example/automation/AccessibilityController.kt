package com.example.automation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.ai.ToolExecutionResult

class AccessibilityController(private val context: Context) {

    fun isServiceEnabled(): Boolean = WibesAccessibilityService.isConnected()

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun clickElement(targetText: String): ToolExecutionResult {
        val service = WibesAccessibilityService.instance
        if (service == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Accessibility Service is not enabled. Please enable 'WIBES AI AUTOPILOT' in Android Accessibility Settings.",
                error = "ServiceNotEnabled"
            )
        }

        val clicked = service.clickElementByText(targetText)
        return if (clicked) {
            ToolExecutionResult(
                isSuccess = true,
                message = "Successfully clicked element matching '$targetText'.",
                data = mapOf("target" to targetText, "clicked" to true)
            )
        } else {
            ToolExecutionResult(
                isSuccess = false,
                message = "Could not find a clickable element matching '$targetText' on current screen.",
                error = "ElementNotFoundOrNotClickable"
            )
        }
    }

    fun typeText(textToType: String, fieldHint: String?): ToolExecutionResult {
        val service = WibesAccessibilityService.instance
        if (service == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Accessibility Service is not enabled. Please enable 'WIBES AI AUTOPILOT' in Accessibility Settings.",
                error = "ServiceNotEnabled"
            )
        }

        val typed = service.typeTextIntoField(textToType, fieldHint)
        return if (typed) {
            ToolExecutionResult(
                isSuccess = true,
                message = "Successfully entered text into ${fieldHint ?: "active input field"}.",
                data = mapOf("text" to textToType, "field" to (fieldHint ?: "active"))
            )
        } else {
            ToolExecutionResult(
                isSuccess = false,
                message = "Could not find an editable input field to type into.",
                error = "EditableFieldNotFound"
            )
        }
    }

    fun triggerGlobalAction(actionName: String): ToolExecutionResult {
        val service = WibesAccessibilityService.instance
        if (service == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Accessibility Service is not enabled. Cannot trigger system gesture '$actionName'.",
                error = "ServiceNotEnabled"
            )
        }

        val performed = service.triggerGlobalAction(actionName)
        return if (performed) {
            ToolExecutionResult(
                isSuccess = true,
                message = "Executed global navigation gesture: $actionName.",
                data = mapOf("action" to actionName)
            )
        } else {
            ToolExecutionResult(
                isSuccess = false,
                message = "System declined global action '$actionName'.",
                error = "GestureFailed"
            )
        }
    }

    fun scroll(forward: Boolean): ToolExecutionResult {
        val service = WibesAccessibilityService.instance
        if (service == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Accessibility Service is not enabled.",
                error = "ServiceNotEnabled"
            )
        }

        val scrolled = service.scrollWindow(forward)
        return if (scrolled) {
            ToolExecutionResult(
                isSuccess = true,
                message = "Scrolled window ${if (forward) "forward (down)" else "backward (up)"}.",
                data = mapOf("forward" to forward)
            )
        } else {
            ToolExecutionResult(
                isSuccess = false,
                message = "Current foreground window does not contain a scrollable container.",
                error = "NoScrollableContainer"
            )
        }
    }

    fun readScreenContent(): ToolExecutionResult {
        val service = WibesAccessibilityService.instance
        if (service == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Accessibility Service is not enabled. Cannot inspect screen.",
                error = "ServiceNotEnabled"
            )
        }

        val lines = service.extractVisibleText()
        return if (lines.isNotEmpty()) {
            ToolExecutionResult(
                isSuccess = true,
                message = "Read ${lines.size} visible text element(s) from current window.",
                data = mapOf("elements_count" to lines.size),
                rawOutput = lines.joinToString("\n")
            )
        } else {
            ToolExecutionResult(
                isSuccess = true,
                message = "Active window is currently blank or protected from accessibility capture.",
                data = mapOf("elements_count" to 0),
                rawOutput = "[No accessible text detected]"
            )
        }
    }
}
