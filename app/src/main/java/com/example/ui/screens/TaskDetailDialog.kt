package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientTask
import com.example.data.model.GigWorker
import com.example.data.model.TaskStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.GoldRating
import com.example.ui.theme.TealPrimary

@Composable
fun TaskDetailDialog(
    task: ClientTask,
    workers: List<GigWorker>,
    onDismiss: () -> Unit,
    onMatchWorker: (ClientTask, GigWorker) -> Unit,
    onRateTask: (ClientTask, Int, String) -> Unit
) {
    var selectedWorker by remember { mutableStateOf(workers.find { it.category == task.category } ?: workers.firstOrNull()) }
    var ratingValue by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("Excellent work done quickly and professionally!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Text(
                    text = "${task.category} • Budget: ${task.budget}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Description:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = task.description, style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Location", style = MaterialTheme.typography.labelMedium)
                        Text(text = task.location, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "Deadline", style = MaterialTheme.typography.labelMedium)
                        Text(text = task.deadline, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                Divider()

                if (task.status == TaskStatus.OPEN) {
                    Text(
                        text = "Match Vetted Worker:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )

                    workers.forEach { worker ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedWorker?.id == worker.id) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${worker.category} • ${worker.rating}★", fontSize = 12.sp)
                                }
                                RadioButton(
                                    selected = selectedWorker?.id == worker.id,
                                    onClick = { selectedWorker = worker }
                                )
                            }
                        }
                    }
                } else if (task.status == TaskStatus.MATCHED || task.status == TaskStatus.IN_PROGRESS) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TealPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Assigned Gig Worker: ${task.assignedWorkerName}",
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(text = "Scheduled Time: ${task.scheduledTime ?: "Confirmed"}", fontSize = 12.sp)
                            if (task.calendarEventLink != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Google Calendar Sync Active",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Complete Task & Add Rating:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { starIndex ->
                            IconButton(
                                onClick = { ratingValue = starIndex },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Star $starIndex",
                                    tint = if (starIndex <= ratingValue) GoldRating else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Client Review") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (task.status == TaskStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Task Completed & Reviewed",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(text = "Rating: ${task.rating ?: 5} ★", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Review: ${task.review ?: ""}", fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (task.status == TaskStatus.OPEN) {
                Button(
                    onClick = {
                        val worker = selectedWorker
                        if (worker != null) {
                            onMatchWorker(task, worker)
                        }
                    },
                    modifier = Modifier.testTag("match_worker_confirm_btn")
                ) {
                    Text("Schedule Task")
                }
            } else if (task.status == TaskStatus.MATCHED || task.status == TaskStatus.IN_PROGRESS) {
                Button(
                    onClick = {
                        onRateTask(task, ratingValue, reviewText)
                    },
                    modifier = Modifier.testTag("complete_task_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("Mark Task Completed")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
