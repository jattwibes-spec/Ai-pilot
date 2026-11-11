package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.MemoryEntity
import com.example.data.entities.ScheduledAutomationEntity
import com.example.data.entities.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoriesByCategoryFlow(category: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}

@Dao
interface TaskHistoryDao {
    @Query("SELECT * FROM task_history ORDER BY timestamp DESC")
    fun getAllTaskHistoryFlow(): Flow<List<TaskHistoryEntity>>

    @Query("SELECT * FROM task_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTasks(limit: Int): List<TaskHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskHistory(task: TaskHistoryEntity): Long

    @Query("DELETE FROM task_history")
    suspend fun clearHistory()
}

@Dao
interface ScheduledAutomationDao {
    @Query("SELECT * FROM scheduled_automations ORDER BY hour ASC, minute ASC")
    fun getAllAutomationsFlow(): Flow<List<ScheduledAutomationEntity>>

    @Query("SELECT * FROM scheduled_automations WHERE isEnabled = 1")
    suspend fun getEnabledAutomations(): List<ScheduledAutomationEntity>

    @Query("SELECT * FROM scheduled_automations WHERE id = :id")
    suspend fun getAutomationById(id: Long): ScheduledAutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: ScheduledAutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: ScheduledAutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: ScheduledAutomationEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogsFlow(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}
