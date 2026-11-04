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
import com.example.data.model.InterviewStatus
import com.example.data.model.VerificationStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.GoldRating
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.KivuluUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    uiState: KivuluUiState,
    onStartInterview: (String) -> Unit,
    onSubmitAnswer: (Int, Int) -> Unit,
    onRegisterWorker: (
        name: String,
        email: String,
        phone: String,
        location: String,
        category: String,
        skillsStr: String,
        equipmentsStr: String,
        rate: String
    ) -> Unit
) {
    var isRegisteringNewWorker by remember { mutableStateOf(uiState.myWorkerProfile == null) }

    var newName by remember { mutableStateOf("Fredrick Anziko") }
    var newEmail by remember { mutableStateOf("anzikofred@gmail.com") }
    var newPhone by remember { mutableStateOf("+256 772 102 938") }
    var newLocation by remember { mutableStateOf("Kampala Central") }
    var newCategory by remember { mutableStateOf("Electrical") }
    var newSkills by remember { mutableStateOf("Solar Wiring, Panel Repair, CCTV") }
    var newEquipments by remember { mutableStateOf("Multimeter, Insulated Toolkit, Ladder") }
    var newRate by remember { mutableStateOf("UGX 45,000 / hr") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("worker_profile_screen")
    ) {
        Text(
            text = "Worker Vetting & Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TealPrimary
        )
        Text(
            text = "Assessing and verifying gig worker skills with AI",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val profile = uiState.myWorkerProfile

        if (profile != null) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TealPrimary)
                        ) {
                            Text(
                                text = profile.name.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${profile.category} • ${profile.hourlyRate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = profile.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Status Badge
                        val badgeColor = if (profile.verificationStatus == VerificationStatus.VETTED_APPROVED) Color(0xFF2E7D32) else AmberAccent
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (profile.verificationStatus == VerificationStatus.VETTED_APPROVED) Icons.Filled.Verified else Icons.Filled.HourglassTop,
                                    contentDescription = "Status",
                                    tint = badgeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (profile.verificationStatus == VerificationStatus.VETTED_APPROVED) "Vetted" else "Pending",
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AI Vetting Assessment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.aiVettingSummary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Calendar Sync",
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Google Calendar Linked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onStartInterview(profile.category) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                        ) {
                            Icon(imageVector = Icons.Filled.Quiz, contentDescription = "Take Quiz")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Skill Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Interactive Interview Card
            val interview = uiState.activeInterview
            if (interview != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_interview_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Vetting Quiz: ${interview.category}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )

                            if (interview.status == InterviewStatus.COMPLETED_PASSED) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9)) {
                                    Text(
                                        text = "PASSED (${interview.score}%)",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (interview.status == InterviewStatus.IN_PROGRESS || interview.status == InterviewStatus.NOT_STARTED) {
                            val currentQ = interview.questions.getOrNull(interview.currentQuestionIndex)
                            if (currentQ != null) {
                                Text(
                                    text = "Question ${interview.currentQuestionIndex + 1} of ${interview.questions.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentQ.questionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                currentQ.options.forEachIndexed { optIndex, optionText ->
                                    val isSelected = interview.userAnswers[interview.currentQuestionIndex] == optIndex
                                    ElevatedButton(
                                        onClick = {
                                            onSubmitAnswer(interview.currentQuestionIndex, optIndex)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Text(
                                            text = optionText,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Completed Summary
                            Text(
                                text = interview.aiAssessmentSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            // Register New Worker
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Onboard as Gig Worker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("WhatsApp Contact") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newLocation,
                        onValueChange = { newLocation = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Trade Category (e.g. Electrical, Plumbing)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newSkills,
                        onValueChange = { newSkills = it },
                        label = { Text("Skills (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEquipments,
                        onValueChange = { newEquipments = it },
                        label = { Text("Tools & Equipment") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newRate,
                        onValueChange = { newRate = it },
                        label = { Text("Hourly Rate") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onRegisterWorker(
                                newName,
                                newEmail,
                                newPhone,
                                newLocation,
                                newCategory,
                                newSkills,
                                newEquipments,
                                newRate
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_worker_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Save & Begin AI Vetting Interview", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
