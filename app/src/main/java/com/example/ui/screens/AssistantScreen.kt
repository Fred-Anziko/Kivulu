package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.model.GigWorker
import com.example.ui.components.ChatBubble
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.KivuluUiState
import com.example.ui.viewmodel.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    uiState: KivuluUiState,
    onSendMessage: (String) -> Unit,
    onRoleSelect: (UserRole) -> Unit,
    onWorkerClick: (GigWorker) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val agentTitle = if (uiState.currentRole == UserRole.CLIENT) "gigClientAgent" else "gigWorkerAgent"
    val agentSubtitle = if (uiState.currentRole == UserRole.CLIENT) "Posting tasks & scheduling vetted workers" else "Worker onboarding, vetting & calendar sync"

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Agent Header
        Surface(
            color = TealPrimary,
            contentColor = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Agent",
                        tint = AmberAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Kivulu AI • $agentTitle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = agentSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                FilterChip(
                    selected = true,
                    onClick = {
                        val nextRole = if (uiState.currentRole == UserRole.CLIENT) UserRole.GIG_WORKER else UserRole.CLIENT
                        onRoleSelect(nextRole)
                    },
                    label = {
                        Text(
                            text = if (uiState.currentRole == UserRole.CLIENT) "Client Mode" else "Worker Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.chatMessages) { chatMsg ->
                ChatBubble(
                    message = chatMsg,
                    onSuggestionClick = { suggestion ->
                        onSendMessage(suggestion)
                    },
                    onWorkerClick = onWorkerClick
                )
            }

            if (uiState.isAiThinking) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TealPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "$agentTitle is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Quick Action Chips bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (uiState.currentRole == UserRole.CLIENT) {
                SuggestionChip(
                    onClick = { onSendMessage("I need an emergency plumber to fix a pipe leak") },
                    label = { Text("Fix Plumbing Leak", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { onSendMessage("Schedule electrical breaker inspection with Fredrick") },
                    label = { Text("Schedule Electrical", fontSize = 11.sp) }
                )
            } else {
                SuggestionChip(
                    onClick = { onSendMessage("Start my AI skill assessment and vetting quiz") },
                    label = { Text("Take Vetting Quiz", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { onSendMessage("Link my Google Calendar for automatic booking") },
                    label = { Text("Link Google Calendar", fontSize = 11.sp) }
                )
            }
        }

        // Chat Input Box
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask $agentTitle...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val msg = inputText
                            inputText = ""
                            onSendMessage(msg)
                        }
                    },
                    modifier = Modifier
                        .testTag("send_chat_btn")
                        .background(TealPrimary, shape = RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
