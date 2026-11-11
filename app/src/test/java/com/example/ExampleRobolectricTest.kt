package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.NeedleEngine
import com.example.ai.ToolRegistry
import com.example.security.ActionGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WIBES AI AUTOPILOT", appName)
    }

    @Test
    fun testNeedleMultiStepDecomposition() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = NeedleEngine(context)
        val input = "Find all APK files, organize them into a folder called Applications, and show me how much storage they occupy."
        val plan = engine.planTask(input)

        assertNotNull(plan)
        assertEquals(4, plan.steps.size)
        assertEquals("search_files", plan.steps[0].toolName)
        assertEquals("create_folder", plan.steps[1].toolName)
        assertEquals("organize_files_by_type", plan.steps[2].toolName)
        assertEquals("calculate_storage_usage", plan.steps[3].toolName)
    }

    @Test
    fun testNeedleScheduledTaskParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = NeedleEngine(context)
        val input = "Every morning at 8 AM, show my important notifications"
        val plan = engine.planTask(input)

        assertNotNull(plan)
        assertEquals(1, plan.steps.size)
        assertEquals("schedule_automation_task", plan.steps[0].toolName)
        assertEquals(8, plan.steps[0].arguments["hour"])
        assertEquals("DAILY", plan.steps[0].arguments["schedule_type"])
    }
}
