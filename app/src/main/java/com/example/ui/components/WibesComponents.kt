package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ExecutionPlan
import com.example.ai.PlanStep
import com.example.ai.PlannerState
import com.example.ai.StepStatus
import com.example.security.SensitiveActionPrompt
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentInfo
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
fun PlanExecutionCard(
    plan: ExecutionPlan,
    state: PlannerState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .testTag("plan_execution_card"),
        colors = CardDefaults.cardColors(containerColor = DeepBlackSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTONOMOUS PLAN: ${plan.id.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricVioletLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = plan.userGoal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }

                if (state is PlannerState.Executing || state is PlannerState.WaitingConfirmation) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentError.copy(alpha = 0.2f))
                            .testTag("cancel_execution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Execution",
                            tint = AccentError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation
            if (plan.explanation.isNotBlank()) {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = plan.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Steps timeline
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.steps.forEachIndexed { index, step ->
                    PlanStepRow(step = step, stepNumber = index + 1)
                }
            }
        }
    }
}

@Composable
fun PlanStepRow(step: PlanStep, stepNumber: Int) {
    val statusColor = when (step.status) {
        StepStatus.PENDING -> TextMuted
        StepStatus.RUNNING -> AccentWarning
        StepStatus.CONFIRMATION_REQUIRED -> AccentWarning
        StepStatus.SUCCEEDED -> AccentSuccess
        StepStatus.FAILED -> AccentError
        StepStatus.SKIPPED -> TextMuted
        StepStatus.CANCELLED -> AccentError
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = SurfaceElevated.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (step.status == StepStatus.RUNNING) AccentWarning else SurfaceBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = if (step.status == StepStatus.RUNNING) alphaAnim else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.PENDING -> Text(
                        text = "$stepNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    StepStatus.RUNNING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentWarning
                    )
                    StepStatus.SUCCEEDED -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = AccentSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.FAILED -> Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = AccentError,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.CONFIRMATION_REQUIRED -> Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Confirmation",
                        tint = AccentWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.CANCELLED -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelled",
                        tint = AccentError,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.SKIPPED -> Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Skipped",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = step.toolName,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricVioletLight,
                        fontWeight = FontWeight.Bold
                    )
                    if (step.executionDurationMs > 0) {
                        Text(
                            text = "${step.executionDurationMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                if (!step.resultMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.resultMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (step.status == StepStatus.FAILED) AccentError else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ActionConfirmationDialog(
    prompt: SensitiveActionPrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepBlackSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Alert",
                    tint = AccentWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Authorization Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricVioletLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prompt.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TOOL: ${prompt.toolName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                        Text(
                            text = "ARGUMENTS: ${prompt.parametersSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WIBES Action Guard strictly prevents unauthorized operations without user consent.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Approve & Execute")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text("Decline", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionChips(
    onSelectCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "Find all APK files, organize them into a folder called Applications, and show me how much storage they occupy." to "Organize APKs & Storage",
        "Summarize my active notifications and alerts" to "Notification Summary",
        "Find duplicate files in Downloads using hash verification" to "Find Duplicate Files",
        "Calculate storage occupied by all files in Downloads" to "Audit Storage",
        "List all installed launchable applications" to "List Installed Apps",
        "Read visible text on active screen via Accessibility" to "Inspect Screen Text"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "QUICK AUTOMATIONS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActions.forEach { (fullPrompt, label) ->
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectCommand(fullPrompt) }
                        .testTag("chip_${label.replace(' ', '_').lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
