package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.TaskStatus
import com.example.ui.components.TaskCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.KivuluUiState

@Composable
fun MyTasksScreen(
    uiState: KivuluUiState,
    onTaskClick: (ClientTask) -> Unit,
    onPostTaskClick: () -> Unit
) {
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Completed

    val activeTasks = uiState.tasks.filter { it.status == TaskStatus.OPEN || it.status == TaskStatus.MATCHED || it.status == TaskStatus.IN_PROGRESS }
    val completedTasks = uiState.tasks.filter { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.CANCELLED }

    val displayedTasks = if (selectedFilterTab == 0) activeTasks else completedTasks

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Gigs & Schedules",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Text(
                    text = "Manage booked tasks & Google Calendar events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = onPostTaskClick,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Post")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Task", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(selectedTabIndex = selectedFilterTab) {
            Tab(
                selected = selectedFilterTab == 0,
                onClick = { selectedFilterTab = 0 },
                text = { Text("Active Tasks (${activeTasks.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedFilterTab == 1,
                onClick = { selectedFilterTab = 1 },
                text = { Text("Completed Gigs (${completedTasks.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (displayedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = "No tasks",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedFilterTab == 0) "No active tasks in progress." else "No completed gigs history.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(displayedTasks) { task ->
                    TaskCard(
                        task = task,
                        onTaskClick = onTaskClick,
                        onActionClick = onTaskClick
                    )
                }
            }
        }
    }
}
