package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.ai.ToolExecutionResult

data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class AppController(private val context: Context) {

    fun listInstalledApps(filter: String? = null): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfos.map {
                val appName = it.loadLabel(pm).toString()
                val pkgName = it.activityInfo.packageName
                val isSystem = (it.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                AppInfo(appName, pkgName, isSystem)
            }.distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }

            val filtered = if (!filter.isNullOrBlank()) {
                apps.filter { it.name.contains(filter, ignoreCase = true) || it.packageName.contains(filter, ignoreCase = true) }
            } else {
                apps
            }

            val appListSummary = filtered.take(15).joinToString(", ") { "${it.name} (${it.packageName})" }
            ToolExecutionResult(
                isSuccess = true,
                message = "Discovered ${filtered.size} launchable application(s).",
                data = mapOf(
                    "count" to filtered.size,
                    "apps" to filtered.map { mapOf("name" to it.name, "package" to it.packageName) }
                ),
                rawOutput = appListSummary
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to query installed applications: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun openApplication(appNameOrPackage: String): ToolExecutionResult {
        val trimmed = appNameOrPackage.trim()
        val pm = context.packageManager

        // 1. Check if direct package name
        var targetPackage: String? = null
        if (trimmed.contains(".")) {
            try {
                pm.getPackageInfo(trimmed, 0)
                targetPackage = trimmed
            } catch (_: PackageManager.NameNotFoundException) {
                targetPackage = null
            }
        }

        // 2. Search by label
        if (targetPackage == null) {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val activities = pm.queryIntentActivities(launcherIntent, 0)
            val match = activities.firstOrNull {
                it.loadLabel(pm).toString().equals(trimmed, ignoreCase = true) ||
                    it.loadLabel(pm).toString().contains(trimmed, ignoreCase = true) ||
                    it.activityInfo.packageName.contains(trimmed, ignoreCase = true)
            }
            targetPackage = match?.activityInfo?.packageName
        }

        if (targetPackage == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Application '$appNameOrPackage' was not found on this device.",
                error = "PackageNotFound"
            )
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent == null) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Application '$targetPackage' is installed but cannot be launched directly.",
                error = "NoLauncherActivity"
            )
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Successfully launched $appNameOrPackage ($targetPackage).",
                data = mapOf("package" to targetPackage, "launched" to true)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to launch $targetPackage: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun openSettingsScreen(screen: String): ToolExecutionResult {
        val intentAction = when (screen.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "wireless" -> Settings.ACTION_WIRELESS_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened Android System Settings ($screen).",
                data = mapOf("screen" to screen)
            )
        } catch (e: Exception) {
            // Fallback to main settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Opened Android System Settings.",
                    data = mapOf("screen" to "general")
                )
            } catch (e2: Exception) {
                ToolExecutionResult(
                    isSuccess = false,
                    message = "Could not open settings: ${e2.message}",
                    error = e2.localizedMessage
                )
            }
        }
    }

    fun openAppDetailsSettings(packageName: String): ToolExecutionResult {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened app details settings for $packageName."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to open app details: ${e.message}",
                error = e.localizedMessage
            )
        }
    }
}
