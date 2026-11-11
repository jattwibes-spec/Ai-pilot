package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.entities.ScheduledAutomationEntity
import java.util.Calendar

class AutomationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleAutomation(automation: ScheduledAutomationEntity) {
        if (!automation.isEnabled || alarmManager == null) return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, automation.hour)
            set(Calendar.MINUTE, automation.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
            putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, automation.id)
            putExtra(AutomationAlarmReceiver.EXTRA_COMMAND, automation.naturalCommand)
            putExtra(AutomationAlarmReceiver.EXTRA_TITLE, automation.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            automation.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.i("AutomationScheduler", "Scheduled '${automation.title}' for ${calendar.time}")
        } catch (e: Exception) {
            Log.e("AutomationScheduler", "Scheduling failed: ${e.message}")
        }
    }

    fun cancelAutomation(automationId: Long) {
        val intent = Intent(context, AutomationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            automationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
