package com.example.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.WibesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val automationId = intent.getLongExtra(EXTRA_AUTOMATION_ID, -1L)
        val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Task"

        Log.i("AutomationReceiver", "Alarm fired for '$title': $command")

        showTriggerNotification(context, title, command)

        // Trigger execution via application container
        val app = context.applicationContext as? WibesApplication
        if (app != null) {
            app.taskPlanner.executeGoal(command, CoroutineScope(Dispatchers.Default))

            // Reschedule next occurrence if daily
            CoroutineScope(Dispatchers.IO).launch {
                val entity = app.database.scheduledAutomationDao().getAutomationById(automationId)
                if (entity != null && entity.isEnabled && entity.scheduleType == "DAILY") {
                    app.scheduler.scheduleAutomation(
                        entity.copy(
                            lastRunTimestamp = System.currentTimeMillis(),
                            runCount = entity.runCount + 1
                        )
                    )
                    app.database.scheduledAutomationDao().updateAutomation(
                        entity.copy(
                            lastRunTimestamp = System.currentTimeMillis(),
                            runCount = entity.runCount + 1
                        )
                    )
                }
            }
        }
    }

    private fun showTriggerNotification(context: Context, title: String, command: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "wibes_scheduler_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WIBES Scheduled Automations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for background automated workflows"
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WIBES Autopilot: $title")
            .setContentText("Executing: $command")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    companion object {
        const val EXTRA_AUTOMATION_ID = "extra_automation_id"
        const val EXTRA_COMMAND = "extra_command"
        const val EXTRA_TITLE = "extra_title"
    }
}
