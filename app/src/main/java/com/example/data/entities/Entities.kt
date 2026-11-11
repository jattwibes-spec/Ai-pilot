package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val category: String, // "Preference", "AppShortcut", "CustomInstruction", "Workflow", "Fact"
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: String,
    val prompt: String,
    val status: String, // "COMPLETED", "FAILED", "CANCELLED", "PARTIAL"
    val totalSteps: Int,
    val completedSteps: Int,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val resultSummary: String,
    val stepsJson: String = ""
)

@Entity(tableName = "scheduled_automations")
data class ScheduledAutomationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scheduleType: String, // "DAILY", "WEEKLY", "ONE_TIME"
    val hour: Int,
    val minute: Int,
    val naturalCommand: String,
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long = 0L,
    val nextRunTimestamp: Long = 0L,
    val runCount: Int = 0
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val argumentsJson: String,
    val isSensitive: Boolean,
    val confirmedByUser: Boolean,
    val status: String, // "SUCCESS", "FAILED", "DENIED"
    val verificationResult: String
)
