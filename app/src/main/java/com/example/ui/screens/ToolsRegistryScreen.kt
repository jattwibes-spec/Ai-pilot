package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ToolDefinition
import com.example.ai.ToolRegistry
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.DeepBlackSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ToolsRegistryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val modules = listOf(
        "ALL",
        "APP_CONTROL",
        "ACCESSIBILITY",
        "FILE_SYSTEM",
        "BROWSER",
        "NOTIFICATIONS",
        "COMMUNICATION",
        "TERMUX",
        "MEMORY",
        "SCHEDULER"
    )
    var selectedModule by remember { mutableStateOf("ALL") }
    var testingTool by remember { mutableStateOf<ToolDefinition?>(null) }
    val testResult by viewModel.toolTestingResult.collectAsState()

    val allTools = remember { ToolRegistry.getAllTools() }
    val filteredTools = if (selectedModule == "ALL") allTools else allTools.filter { it.module == selectedModule }

    // Test Tool Modal
    testingTool?.let { tool ->
        ToolExecutionDialog(
            tool = tool,
            onDismiss = {
                testingTool = null
                viewModel.clearToolTestResult()
            },
            onExecute = { args ->
                viewModel.testExecuteTool(tool, args)
            },
            result = testResult
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGraphite)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = ElectricVioletLight,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "TOOL REGISTRY",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${allTools.size} strict schema-constrained tools available to Needle 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Module filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modules.forEach { mod ->
                val isSelected = selectedModule == mod
                Surface(
                    color = if (isSelected) ElectricViolet else SurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ElectricViolet else SurfaceBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedModule = mod }
                ) {
                    Text(
                        text = mod.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Tools List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredTools) { tool ->
                ToolCard(
                    tool = tool,
                    onTestClick = {
                        viewModel.clearToolTestResult()
                        testingTool = tool
                    }
                )
            }
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolDefinition,
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepBlackSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tool.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricVioletLight,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MODULE: ${tool.module}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                if (tool.isSensitive) {
                    Surface(
                        color = AccentWarning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentWarning.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentWarning,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sensitive",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = AccentWarning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            if (tool.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PARAMETERS:",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                tool.parameters.forEach { param ->
                    Text(
                        text = "• ${param.name} (${param.type}${if (param.required) ", required" else ""}): ${param.description}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onTestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("test_tool_${tool.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Tool",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ToolExecutionDialog(
    tool: ToolDefinition,
    onDismiss: () -> Unit,
    onExecute: (Map<String, Any>) -> Unit,
    result: com.example.ai.ToolExecutionResult?
) {
    val inputValues = remember {
        mutableStateMapOf<String, String>().apply {
            tool.parameters.forEach { param ->
                this[param.name] = param.defaultValue?.toString() ?: ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepBlackSurface,
        title = {
            Column {
                Text(
                    text = "Test Tool Execution",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = ElectricVioletLight
                )
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                items(tool.parameters) { param ->
                    OutlinedTextField(
                        value = inputValues[param.name] ?: "",
                        onValueChange = { inputValues[param.name] = it },
                        label = { Text("${param.name} (${param.type})") },
                        placeholder = { Text(param.description) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                // Show Execution Result
                if (result != null) {
                    item {
                        Surface(
                            color = if (result.isSuccess) AccentSuccess.copy(alpha = 0.1f) else AccentError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (result.isSuccess) AccentSuccess.copy(alpha = 0.5f) else AccentError.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (result.isSuccess) AccentSuccess else AccentError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (result.isSuccess) "EXECUTION SUCCESS" else "EXECUTION FAILED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isSuccess) AccentSuccess else AccentError
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                if (result.rawOutput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = result.rawOutput,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val map = mutableMapOf<String, Any>()
                    tool.parameters.forEach { param ->
                        val text = inputValues[param.name] ?: ""
                        if (text.isNotBlank()) {
                            if (param.type == "int") {
                                map[param.name] = text.toIntOrNull() ?: 0
                            } else {
                                map[param.name] = text
                            }
                        } else if (param.defaultValue != null) {
                            map[param.name] = param.defaultValue
                        }
                    }
                    onExecute(map)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("execute_test_button")
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
