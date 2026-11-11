package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.MemoryDao
import com.example.data.dao.ScheduledAutomationDao
import com.example.data.dao.TaskHistoryDao
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.MemoryEntity
import com.example.data.entities.ScheduledAutomationEntity
import com.example.data.entities.TaskHistoryEntity

@Database(
    entities = [
        MemoryEntity::class,
        TaskHistoryEntity::class,
        ScheduledAutomationEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun scheduledAutomationDao(): ScheduledAutomationDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wibes_autopilot.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
