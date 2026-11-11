package com.example.notifications

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.ai.ToolExecutionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WibesNotification(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val category: String
)

class WibesNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { addNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            synchronized(notificationBuffer) {
                notificationBuffer.removeAll { item -> item.id == sbn.id && item.packageName == sbn.packageName }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun syncActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            synchronized(notificationBuffer) {
                notificationBuffer.clear()
                active.forEach { sbn ->
                    val notification = sbn.notification
                    val extras = notification?.extras
                    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    val pm = packageManager
                    val appName = try {
                        val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        sbn.packageName
                    }
                    val cat = categorizeNotification(notification?.category, sbn.packageName, title, text)

                    if (title.isNotBlank() || text.isNotBlank()) {
                        notificationBuffer.add(
                            WibesNotification(
                                id = sbn.id,
                                packageName = sbn.packageName,
                                appName = appName,
                                title = title,
                                text = text,
                                timestamp = sbn.postTime,
                                category = cat
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun addNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val cat = categorizeNotification(notification.category, sbn.packageName, title, text)

        synchronized(notificationBuffer) {
            // Keep buffer capped at 50
            if (notificationBuffer.size > 50) {
                notificationBuffer.removeAt(0)
            }
            notificationBuffer.add(
                WibesNotification(
                    id = sbn.id,
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime,
                    category = cat
                )
            )
        }
    }

    private fun categorizeNotification(rawCategory: String?, pkg: String, title: String, text: String): String {
        val combined = "$pkg $title $text".lowercase()
        return when {
            rawCategory == Notification.CATEGORY_MESSAGE || combined.contains("message") || combined.contains("chat") || combined.contains("whatsapp") || combined.contains("telegram") -> "Communication"
            rawCategory == Notification.CATEGORY_SOCIAL || combined.contains("instagram") || combined.contains("twitter") || combined.contains("facebook") -> "Social"
            rawCategory == Notification.CATEGORY_ALARM || rawCategory == Notification.CATEGORY_REMINDER || combined.contains("alarm") || combined.contains("reminder") -> "Alert"
            rawCategory == Notification.CATEGORY_PROMO || combined.contains("offer") || combined.contains("discount") || combined.contains("sale") -> "Promotion"
            rawCategory == Notification.CATEGORY_SYSTEM || pkg.contains("android") || pkg.contains("system") -> "System"
            else -> "General"
        }
    }

    companion object {
        @Volatile
        var instance: WibesNotificationListenerService? = null
            private set

        val notificationBuffer = mutableListOf<WibesNotification>()

        fun isConnected(): Boolean = instance != null
    }
}

class NotificationController(private val context: Context) {

    fun isPermissionGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getRecentNotifications(filterApp: String?): ToolExecutionResult {
        if (!isPermissionGranted()) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Notification Listener access is not granted. Please allow notification access for WIBES AI AUTOPILOT in Settings.",
                error = "PermissionNotGranted"
            )
        }

        WibesNotificationListenerService.instance?.syncActiveNotifications()

        val list = synchronized(WibesNotificationListenerService.notificationBuffer) {
            WibesNotificationListenerService.notificationBuffer.toList()
        }

        val filtered = if (!filterApp.isNullOrBlank()) {
            list.filter { it.appName.contains(filterApp, ignoreCase = true) || it.packageName.contains(filterApp, ignoreCase = true) }
        } else {
            list
        }

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val output = filtered.joinToString("\n\n") { n ->
            "[${n.category}] ${n.appName} (${dateFormat.format(Date(n.timestamp))}):\n${n.title} - ${n.text}"
        }

        return ToolExecutionResult(
            isSuccess = true,
            message = "Retrieved ${filtered.size} notification(s).",
            data = mapOf("count" to filtered.size),
            rawOutput = if (output.isBlank()) "No active notifications present." else output
        )
    }

    fun summarizeNotifications(): ToolExecutionResult {
        if (!isPermissionGranted()) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Notification access required. Enable Notification Listener in Settings.",
                error = "PermissionNotGranted"
            )
        }

        WibesNotificationListenerService.instance?.syncActiveNotifications()

        val list = synchronized(WibesNotificationListenerService.notificationBuffer) {
            WibesNotificationListenerService.notificationBuffer.toList()
        }

        if (list.isEmpty()) {
            return ToolExecutionResult(
                isSuccess = true,
                message = "You have no active notifications right now. Everything is quiet.",
                rawOutput = "Zero active notifications."
            )
        }

        val byCategory = list.groupBy { it.category }
        val sb = StringBuilder()
        sb.append("Notification Briefing (${list.size} total active):\n")
        for ((cat, notifs) in byCategory) {
            sb.append("\n• $cat (${notifs.size}):\n")
            notifs.take(3).forEach { n ->
                sb.append("  - ${n.appName}: ${n.title.take(30)}${if (n.title.length > 30) "..." else ""}\n")
            }
        }

        return ToolExecutionResult(
            isSuccess = true,
            message = "Summarized ${list.size} active notifications across ${byCategory.size} categories.",
            data = mapOf(
                "total" to list.size,
                "categories" to byCategory.mapValues { it.value.size }
            ),
            rawOutput = sb.toString()
        )
    }
}
