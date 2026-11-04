package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.KivuluTheme
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.KivuluViewModel
import com.example.ui.viewmodel.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KivuluApp(
    viewModel: KivuluViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    KivuluTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "Kivulu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                color = com.example.ui.theme.AmberAccent
                            ) {
                                Text(
                                    text = "Gig Marketplace",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TealPrimary,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.EXPLORE,
                        onClick = { viewModel.selectTab(NavigationTab.EXPLORE) },
                        icon = { Icon(imageVector = Icons.Filled.Store, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_explore")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.ASSISTANT,
                        onClick = { viewModel.selectTab(NavigationTab.ASSISTANT) },
                        icon = { Icon(imageVector = Icons.Filled.SmartToy, contentDescription = "AI Agent") },
                        label = { Text("AI Agent") },
                        modifier = Modifier.testTag("nav_assistant")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.MY_TASKS,
                        onClick = { viewModel.selectTab(NavigationTab.MY_TASKS) },
                        icon = { Icon(imageVector = Icons.Filled.Task, contentDescription = "My Tasks") },
                        label = { Text("My Tasks") },
                        modifier = Modifier.testTag("nav_my_tasks")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.PROFILE,
                        onClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                        icon = { Icon(imageVector = Icons.Filled.Badge, contentDescription = "Vetting") },
                        label = { Text("Vetting") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedTab) {
                    NavigationTab.EXPLORE -> {
                        HomeScreen(
                            uiState = uiState,
                            onRoleSelect = { viewModel.selectRole(it) },
                            onSearchChange = { viewModel.updateSearchQuery(it) },
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onWorkerClick = { viewModel.openWorkerDetails(it) },
                            onTaskClick = { viewModel.openTaskDetails(it) },
                            onPostTaskClick = { viewModel.setPostTaskDialogOpen(true) }
                        )
                    }
                    NavigationTab.ASSISTANT -> {
                        AssistantScreen(
                            uiState = uiState,
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onRoleSelect = { viewModel.selectRole(it) },
                            onWorkerClick = { viewModel.openWorkerDetails(it) }
                        )
                    }
                    NavigationTab.MY_TASKS -> {
                        MyTasksScreen(
                            uiState = uiState,
                            onTaskClick = { viewModel.openTaskDetails(it) },
                            onPostTaskClick = { viewModel.setPostTaskDialogOpen(true) }
                        )
                    }
                    NavigationTab.PROFILE -> {
                        WorkerProfileScreen(
                            uiState = uiState,
                            onStartInterview = { viewModel.startVettingInterview(it) },
                            onSubmitAnswer = { qIdx, optIdx -> viewModel.submitInterviewAnswer(qIdx, optIdx) },
                            onRegisterWorker = { name, email, phone, loc, cat, skills, eq, rate ->
                                viewModel.registerNewGigWorker(name, email, phone, loc, cat, skills, eq, rate)
                            }
                        )
                    }
                }

                // Dialogs & Sheets
                if (uiState.isPostTaskDialogOpen) {
                    PostTaskDialog(
                        onDismiss = { viewModel.setPostTaskDialogOpen(false) },
                        onPostTask = { title, desc, cat, loc, budget, deadline, name, email ->
                            viewModel.createAndPostTask(title, desc, cat, loc, budget, deadline, name, email)
                        }
                    )
                }

                uiState.selectedWorkerForDetails?.let { worker ->
                    WorkerDetailSheet(
                        worker = worker,
                        clientTasks = uiState.tasks,
                        onDismiss = { viewModel.closeWorkerDetails() },
                        onScheduleWithTask = { task, w, time ->
                            viewModel.matchAndScheduleWorker(task, w, time)
                            viewModel.closeWorkerDetails()
                        }
                    )
                }

                uiState.selectedTaskForDetails?.let { task ->
                    TaskDetailDialog(
                        task = task,
                        workers = uiState.workers,
                        onDismiss = { viewModel.closeTaskDetails() },
                        onMatchWorker = { t, w ->
                            viewModel.matchAndScheduleWorker(t, w)
                            viewModel.closeTaskDetails()
                        },
                        onRateTask = { t, r, rev ->
                            viewModel.rateAndCompleteTask(t, r, rev)
                            viewModel.closeTaskDetails()
                        }
                    )
                }
            }
        }
    }
}
