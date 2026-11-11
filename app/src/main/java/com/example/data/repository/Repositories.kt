package com.example.data.repository

import com.example.data.dao.AuditLogDao
import com.example.data.dao.MemoryDao
import com.example.data.dao.ScheduledAutomationDao
import com.example.data.dao.TaskHistoryDao
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.MemoryEntity
import com.example.data.entities.ScheduledAutomationEntity
import com.example.data.entities.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class MemoryRepository(private val memoryDao: MemoryDao) {
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()

    suspend fun getAllMemories(): List<MemoryEntity> = memoryDao.getAllMemories()

    suspend fun searchMemories(query: String): List<MemoryEntity> = memoryDao.searchMemories(query)

    suspend fun saveMemory(key: String, category: String, content: String): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                key = key.trim(),
                category = category,
                content = content.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        memoryDao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }

    suspend fun exportToJson(): String {
        val memories = memoryDao.getAllMemories()
        val array = JSONArray()
        for (m in memories) {
            val obj = JSONObject()
            obj.put("key", m.key)
            obj.put("category", m.category)
            obj.put("content", m.content)
            obj.put("createdAt", m.createdAt)
            array.put(obj)
        }
        return array.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Int {
        val array = JSONArray(jsonString)
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val key = obj.optString("key", "")
            val category = obj.optString("category", "Preference")
            val content = obj.optString("content", "")
            if (key.isNotEmpty() && content.isNotEmpty()) {
                saveMemory(key, category, content)
                count++
            }
        }
        return count
    }
}

class TaskRepository(
    private val taskHistoryDao: TaskHistoryDao,
    private val auditLogDao: AuditLogDao
) {
    fun getAllHistoryFlow(): Flow<List<TaskHistoryEntity>> = taskHistoryDao.getAllTaskHistoryFlow()

    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> = auditLogDao.getRecentAuditLogsFlow()

    suspend fun recordTask(
        taskId: String,
        prompt: String,
        status: String,
        totalSteps: Int,
        completedSteps: Int,
        executionTimeMs: Long,
        resultSummary: String,
        stepsJson: String = ""
    ): Long {
        return taskHistoryDao.insertTaskHistory(
            TaskHistoryEntity(
                taskId = taskId,
                prompt = prompt,
                status = status,
                totalSteps = totalSteps,
                completedSteps = completedSteps,
                executionTimeMs = executionTimeMs,
                resultSummary = resultSummary,
                stepsJson = stepsJson
            )
        )
    }

    suspend fun logAudit(
        toolName: String,
        argumentsJson: String,
        isSensitive: Boolean,
        confirmedByUser: Boolean,
        status: String,
        verificationResult: String
    ): Long {
        return auditLogDao.insertAuditLog(
            AuditLogEntity(
                toolName = toolName,
                argumentsJson = argumentsJson,
                isSensitive = isSensitive,
                confirmedByUser = confirmedByUser,
                status = status,
                verificationResult = verificationResult
            )
        )
    }

    suspend fun clearHistory() {
        taskHistoryDao.clearHistory()
        auditLogDao.clearLogs()
    }
}

class SchedulerRepository(private val dao: ScheduledAutomationDao) {
    fun getAllAutomationsFlow(): Flow<List<ScheduledAutomationEntity>> = dao.getAllAutomationsFlow()

    suspend fun getEnabledAutomations(): List<ScheduledAutomationEntity> = dao.getEnabledAutomations()

    suspend fun insert(automation: ScheduledAutomationEntity): Long = dao.insertAutomation(automation)

    suspend fun update(automation: ScheduledAutomationEntity) = dao.updateAutomation(automation)

    suspend fun delete(automation: ScheduledAutomationEntity) = dao.deleteAutomation(automation)
}
