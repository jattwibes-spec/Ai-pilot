package com.example.security

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.automation.WibesAccessibilityService

data class PermissionStatus(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isCritical: Boolean,
    val actionLabel: String
)

class PermissionManager(private val context: Context) {

    fun getComprehensivePermissionReport(): List<PermissionStatus> {
        return listOf(
            PermissionStatus(
                id = "accessibility",
                title = "Accessibility Automation",
                description = "Required to inspect UI nodes, click screen elements, type text, and execute gestures.",
                isGranted = isAccessibilityEnabled(),
                isCritical = true,
                actionLabel = "Enable in Accessibility"
            ),
            PermissionStatus(
                id = "notifications",
                title = "Notification Listener",
                description = "Enables WIBES to read, categorize, and summarize system notifications on-device.",
                isGranted = isNotificationAccessGranted(),
                isCritical = false,
                actionLabel = "Grant Notification Access"
            ),
            PermissionStatus(
                id = "microphone",
                title = "Voice Input (Microphone)",
                description = "Required for hands-free natural-language voice commands and speech recognition.",
                isGranted = isAudioPermissionGranted(),
                isCritical = false,
                actionLabel = "Grant Microphone Permission"
            ),
            PermissionStatus(
                id = "contacts",
                title = "Contacts Access",
                description = "Allows searching phone contacts for quick dialing and messaging workflows.",
                isGranted = isContactsPermissionGranted(),
                isCritical = false,
                actionLabel = "Grant Contacts Permission"
            )
        )
    }

    fun isAccessibilityEnabled(): Boolean = WibesAccessibilityService.isConnected()

    fun isNotificationAccessGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun isAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun isContactsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
