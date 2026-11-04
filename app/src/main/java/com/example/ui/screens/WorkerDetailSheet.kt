package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientTask
import com.example.data.model.GigWorker
import com.example.data.model.VerificationStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.GoldRating
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailSheet(
    worker: GigWorker,
    clientTasks: List<ClientTask>,
    onDismiss: () -> Unit,
    onScheduleWithTask: (ClientTask, GigWorker, String) -> Unit
) {
    var selectedTaskToMatch by remember { mutableStateOf(clientTasks.firstOrNull()) }
    var scheduledTimeText by remember { mutableStateOf("Tomorrow at 10:00 AM") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("worker_detail_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                ) {
                    Text(
                        text = worker.name.take(1).uppercase(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (worker.verificationStatus == VerificationStatus.VETTED_APPROVED) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Vetted",
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "${worker.category} • ${worker.hourlyRate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = GoldRating,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${worker.rating} (${worker.reviewCount} reviews) • ${worker.completedTasksCount} Gigs Done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Vetting Summary Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TealPrimary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "AI Vetting",
                        tint = TealPrimary
                    )
                    Column {
                        Text(
                            text = "Kivulu AI Vetting Certificate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TealPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = worker.aiVettingSummary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "About Worker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = worker.bio, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Verified Skills & Equipment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                worker.skills.forEach { skill ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(skill, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Schedule Task with ${worker.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (clientTasks.isEmpty()) {
                Text(
                    text = "You don't have any open tasks yet. Create a task first to schedule with this worker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Text(text = "Select Task:", style = MaterialTheme.typography.labelLarge)
                clientTasks.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedTaskToMatch?.id == task.id,
                            onClick = { selectedTaskToMatch = task }
                        )
                        Text(text = "${task.title} (${task.budget})", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = scheduledTimeText,
                    onValueChange = { scheduledTimeText = it },
                    label = { Text("Proposed Date & Time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val task = selectedTaskToMatch
                        if (task != null) {
                            onScheduleWithTask(task, worker, scheduledTimeText)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_schedule_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Calendar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm & Sync Google Calendar", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
