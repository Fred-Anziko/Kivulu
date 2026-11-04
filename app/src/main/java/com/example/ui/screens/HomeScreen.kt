package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClientTask
import com.example.data.model.GigWorker
import com.example.ui.components.TaskCard
import com.example.ui.components.WorkerCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.KivuluUiState
import com.example.ui.viewmodel.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: KivuluUiState,
    onRoleSelect: (UserRole) -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onWorkerClick: (GigWorker) -> Unit,
    onTaskClick: (ClientTask) -> Unit,
    onPostTaskClick: () -> Unit
) {
    var selectedSectionTab by remember { mutableStateOf(0) } // 0 = Workers, 1 = Open Tasks
    val categories = listOf("All", "Electrical", "Plumbing", "IT & Tech", "Cleaning", "Carpentry", "Delivery")

    val filteredWorkers = uiState.workers.filter { worker ->
        (uiState.selectedCategory == "All" || worker.category.contains(uiState.selectedCategory, ignoreCase = true)) &&
        (uiState.searchQuery.isBlank() || worker.name.contains(uiState.searchQuery, ignoreCase = true) || worker.skills.any { it.contains(uiState.searchQuery, ignoreCase = true) })
    }

    val filteredTasks = uiState.tasks.filter { task ->
        (uiState.selectedCategory == "All" || task.category.contains(uiState.selectedCategory, ignoreCase = true)) &&
        (uiState.searchQuery.isBlank() || task.title.contains(uiState.searchQuery, ignoreCase = true) || task.description.contains(uiState.searchQuery, ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPostTaskClick,
                modifier = Modifier.testTag("fab_post_task"),
                containerColor = AmberAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Post Task")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Post Task", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    // Try loading generated hero banner drawable
                    val heroDrawableId = try {
                        val field = R.drawable::class.java.getDeclaredField("kivulu_hero_banner_1793802720667")
                        field.getInt(null)
                    } catch (e: Exception) {
                        0
                    }

                    if (heroDrawableId != 0) {
                        Image(
                            painter = painterResource(id = heroDrawableId),
                            contentDescription = "Kivulu Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TealPrimary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AmberAccent
                        ) {
                            Text(
                                text = "Kivulu Marketplace",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Find Vetted Gig Workers & Post Client Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Role Selector Switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "I am using Kivulu as:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = uiState.currentRole == UserRole.CLIENT,
                            onClick = { onRoleSelect(UserRole.CLIENT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Client", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = uiState.currentRole == UserRole.GIG_WORKER,
                            onClick = { onRoleSelect(UserRole.GIG_WORKER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Gig Worker", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by skill, name, category...") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("marketplace_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Category Chips
            item {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }
            }

            // Section Tabs: Vetted Workers vs Open Tasks
            item {
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedSectionTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Tab(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        text = { Text("Vetted Workers (${filteredWorkers.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        text = { Text("Client Tasks (${filteredTasks.size})", fontWeight = FontWeight.Bold) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 0: Vetted Workers
            if (selectedSectionTab == 0) {
                if (filteredWorkers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workers match your filter criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(filteredWorkers) { worker ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            WorkerCard(
                                worker = worker,
                                onWorkerClick = onWorkerClick,
                                onBookClick = onWorkerClick
                            )
                        }
                    }
                }
            } else {
                // Section 1: Client Tasks
                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No client tasks posted yet for this category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(filteredTasks) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
    }
}
