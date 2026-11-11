package com.example

import com.example.ai.ToolRegistry
import com.example.security.ActionGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testToolRegistryIntegrity() {
        val tools = ToolRegistry.getAllTools()
        assertTrue("Tool registry should have multiple registered tools", tools.size >= 15)

        assertNotNull("open_application should be registered", ToolRegistry.getTool("open_application"))
        assertNotNull("search_files should be registered", ToolRegistry.getTool("search_files"))
        assertNotNull("calculate_storage_usage should be registered", ToolRegistry.getTool("calculate_storage_usage"))
        assertNotNull("organize_files_by_type should be registered", ToolRegistry.getTool("organize_files_by_type"))
        assertNotNull("find_duplicate_files should be registered", ToolRegistry.getTool("find_duplicate_files"))
        assertNotNull("click_screen_element should be registered", ToolRegistry.getTool("click_screen_element"))
        assertNotNull("summarize_notifications should be registered", ToolRegistry.getTool("summarize_notifications"))
    }

    @Test
    fun testActionGuardSensitivities() {
        assertTrue("delete_file must be sensitive", ActionGuard.isActionSensitive("delete_file"))
        assertTrue("dial_phone_number must be sensitive", ActionGuard.isActionSensitive("dial_phone_number"))
        assertTrue("draft_sms must be sensitive", ActionGuard.isActionSensitive("draft_sms"))
        assertFalse("search_files should not be sensitive", ActionGuard.isActionSensitive("search_files"))
        assertFalse("calculate_storage_usage should not be sensitive", ActionGuard.isActionSensitive("calculate_storage_usage"))
    }
}
