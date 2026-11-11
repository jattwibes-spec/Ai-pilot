package com.example

import android.app.Application
import com.example.ai.NeedleEngine
import com.example.ai.TaskPlanner
import com.example.automation.AccessibilityController
import com.example.automation.AppController
import com.example.automation.AutomationEngine
import com.example.automation.BrowserController
import com.example.automation.CommunicationController
import com.example.automation.TermuxController
import com.example.data.AppDatabase
import com.example.data.repository.MemoryRepository
import com.example.data.repository.SchedulerRepository
import com.example.data.repository.TaskRepository
import com.example.files.FileManager
import com.example.files.FileOrganizer
import com.example.notifications.NotificationController
import com.example.scheduler.AutomationScheduler
import com.example.security.PermissionManager
import com.example.voice.SpeechInput
import com.example.voice.SpeechOutput

class WibesApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var memoryRepository: MemoryRepository
        private set
    lateinit var taskRepository: TaskRepository
        private set
    lateinit var schedulerRepository: SchedulerRepository
        private set

    lateinit var appController: AppController
        private set
    lateinit var accessibilityController: AccessibilityController
        private set
    lateinit var fileManager: FileManager
        private set
    lateinit var fileOrganizer: FileOrganizer
        private set
    lateinit var browserController: BrowserController
        private set
    lateinit var notificationController: NotificationController
        private set
    lateinit var communicationController: CommunicationController
        private set
    lateinit var termuxController: TermuxController
        private set

    lateinit var automationEngine: AutomationEngine
        private set
    lateinit var needleEngine: NeedleEngine
        private set
    lateinit var taskPlanner: TaskPlanner
        private set

    lateinit var speechInput: SpeechInput
        private set
    lateinit var speechOutput: SpeechOutput
        private set
    lateinit var scheduler: AutomationScheduler
        private set
    lateinit var permissionManager: PermissionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        memoryRepository = MemoryRepository(database.memoryDao())
        taskRepository = TaskRepository(database.taskHistoryDao(), database.auditLogDao())
        schedulerRepository = SchedulerRepository(database.scheduledAutomationDao())

        appController = AppController(this)
        accessibilityController = AccessibilityController(this)
        fileManager = FileManager(this)
        fileOrganizer = FileOrganizer(this, fileManager)
        browserController = BrowserController(this)
        notificationController = NotificationController(this)
        communicationController = CommunicationController(this)
        termuxController = TermuxController(this)

        automationEngine = AutomationEngine(
            context = this,
            appController = appController,
            accessibilityController = accessibilityController,
            fileManager = fileManager,
            fileOrganizer = fileOrganizer,
            browserController = browserController,
            notificationController = notificationController,
            communicationController = communicationController,
            termuxController = termuxController,
            memoryRepository = memoryRepository,
            schedulerRepository = schedulerRepository,
            taskRepository = taskRepository
        )

        needleEngine = NeedleEngine(this)
        taskPlanner = TaskPlanner(needleEngine, automationEngine, taskRepository)

        speechInput = SpeechInput(this)
        speechOutput = SpeechOutput(this)
        scheduler = AutomationScheduler(this)
        permissionManager = PermissionManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        speechOutput.shutdown()
    }

    companion object {
        lateinit var instance: WibesApplication
            private set
    }
}
