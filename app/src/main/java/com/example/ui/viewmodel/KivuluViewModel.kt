package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.KivuluAiService
import com.example.data.model.*
import com.example.data.repository.KivuluRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    CLIENT,
    GIG_WORKER
}

enum class NavigationTab {
    EXPLORE,
    ASSISTANT,
    MY_TASKS,
    PROFILE
}

data class KivuluUiState(
    val currentRole: UserRole = UserRole.CLIENT,
    val selectedTab: NavigationTab = NavigationTab.EXPLORE,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val workers: List<GigWorker> = emptyList(),
    val tasks: List<ClientTask> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAiThinking: Boolean = false,
    val selectedWorkerForDetails: GigWorker? = null,
    val selectedTaskForDetails: ClientTask? = null,
    val activeInterview: VettingInterview? = null,
    val myWorkerProfile: GigWorker? = null,
    val isPostTaskDialogOpen: Boolean = false
)

class KivuluViewModel(
    private val repository: KivuluRepository = KivuluRepository(),
    private val aiService: KivuluAiService = KivuluAiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(KivuluUiState())
    val uiState: StateFlow<KivuluUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.workers.collect { workerList ->
                _uiState.update { 
                    it.copy(
                        workers = workerList,
                        myWorkerProfile = workerList.find { w -> w.id == "w1" } ?: workerList.firstOrNull()
                    ) 
                }
            }
        }
        viewModelScope.launch {
            repository.tasks.collect { taskList ->
                _uiState.update { it.copy(tasks = taskList) }
            }
        }
        viewModelScope.launch {
            repository.activeInterview.collect { interview ->
                _uiState.update { it.copy(activeInterview = interview) }
            }
        }

        // Initialize welcome message
        val welcomeMsg = ChatMessage(
            id = "welcome",
            sender = ChatSender.CLIENT_AGENT,
            senderTitle = "Kivulu Agent",
            message = "Welcome to Kivulu! I am your AI Assistant. I help Clients match and schedule vetted professionals, and help Gig Workers complete AI skill verification & Google Calendar sync.",
            actionSuggestions = listOf(
                "Post a plumbing repair task",
                "Find certified electrician nearby",
                "Start Worker AI Vetting Interview"
            )
        )
        _uiState.update { it.copy(chatMessages = listOf(welcomeMsg)) }
    }

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(currentRole = role) }
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun openWorkerDetails(worker: GigWorker) {
        _uiState.update { it.copy(selectedWorkerForDetails = worker) }
    }

    fun closeWorkerDetails() {
        _uiState.update { it.copy(selectedWorkerForDetails = null) }
    }

    fun openTaskDetails(task: ClientTask) {
        _uiState.update { it.copy(selectedTaskForDetails = task) }
    }

    fun closeTaskDetails() {
        _uiState.update { it.copy(selectedTaskForDetails = null) }
    }

    fun setPostTaskDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isPostTaskDialogOpen = open) }
    }

    fun createAndPostTask(
        title: String,
        description: String,
        category: String,
        location: String,
        budget: String,
        deadline: String,
        clientName: String,
        clientEmail: String
    ) {
        val newTask = ClientTask(
            id = "t_${System.currentTimeMillis()}",
            clientName = clientName.ifBlank { "Client User" },
            clientEmail = clientEmail.ifBlank { "client@kivulu.com" },
            title = title,
            description = description,
            category = category,
            location = location,
            deadline = deadline,
            budget = budget,
            status = TaskStatus.OPEN,
            postedAt = "Just now"
        )
        val created = repository.addTask(newTask)

        // Notify chat
        val chatNotif = ChatMessage(
            id = "task_created_${created.id}",
            sender = ChatSender.CLIENT_AGENT,
            senderTitle = "gigClientAgent",
            message = "Task successfully posted! Title: '${created.title}'. Budget: ${created.budget}. I am matching nearby vetted ${created.category} workers now.",
            createdTask = created,
            actionSuggestions = listOf("View Task in My Tasks", "Schedule Calendar Invite")
        )

        _uiState.update { state ->
            state.copy(
                chatMessages = state.chatMessages + chatNotif,
                isPostTaskDialogOpen = false
            )
        }
    }

    fun matchAndScheduleWorker(task: ClientTask, worker: GigWorker, scheduledTime: String = "2025-11-08T10:00:00") {
        val matchedTask = repository.matchWorkerToTask(task.id, worker, scheduledTime)

        val confirmChat = ChatMessage(
            id = "schedule_${task.id}",
            sender = ChatSender.CLIENT_AGENT,
            senderTitle = "gigClientAgent",
            message = "Task '${task.title}' matched with ${worker.name}! Scheduled on Google Calendar for $scheduledTime. Location: ${task.location}.",
            recommendedWorker = worker,
            createdTask = matchedTask,
            actionSuggestions = listOf("View Calendar Link", "Open My Tasks")
        )

        _uiState.update { state ->
            state.copy(
                chatMessages = state.chatMessages + confirmChat,
                selectedTaskForDetails = matchedTask
            )
        }
    }

    fun rateAndCompleteTask(task: ClientTask, rating: Int, reviewText: String) {
        repository.completeTaskAndRate(task.id, rating, reviewText)
        _uiState.update { state ->
            state.copy(selectedTaskForDetails = null)
        }
    }

    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val userChat = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            senderTitle = "You",
            message = userMessage
        )

        _uiState.update { state ->
            state.copy(
                chatMessages = state.chatMessages + userChat,
                isAiThinking = true
            )
        }

        viewModelScope.launch {
            val isWorkerRole = _uiState.value.currentRole == UserRole.GIG_WORKER
            val aiResponse = if (isWorkerRole) {
                aiService.processWorkerAgentQuery(userMessage, _uiState.value.myWorkerProfile)
            } else {
                aiService.processClientAgentQuery(
                    userPrompt = userMessage,
                    workers = _uiState.value.workers,
                    existingTasks = _uiState.value.tasks
                )
            }

            _uiState.update { state ->
                state.copy(
                    chatMessages = state.chatMessages + aiResponse,
                    isAiThinking = false
                )
            }
        }
    }

    fun startVettingInterview(category: String = "Electrical") {
        val workerId = _uiState.value.myWorkerProfile?.id ?: "w1"
        repository.startVettingInterview(workerId, category)
        selectTab(NavigationTab.PROFILE)
    }

    fun submitInterviewAnswer(questionIndex: Int, optionIndex: Int) {
        repository.submitAnswer(questionIndex, optionIndex)
    }

    fun registerNewGigWorker(
        name: String,
        email: String,
        phone: String,
        location: String,
        category: String,
        skillsStr: String,
        equipmentsStr: String,
        rate: String
    ) {
        val skills = skillsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val equipments = equipmentsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }

        val newWorker = GigWorker(
            id = "w_${System.currentTimeMillis()}",
            name = name.ifBlank { "New Worker" },
            email = email.ifBlank { "worker@kivulu.com" },
            phone = phone.ifBlank { "+256 700 000 000" },
            location = location.ifBlank { "Kampala, Uganda" },
            category = category,
            skills = if (skills.isEmpty()) listOf("General Repairs", "Maintenance") else skills,
            equipments = if (equipments.isEmpty()) listOf("Basic Toolset") else equipments,
            experienceYears = 2,
            rating = 5.0,
            reviewCount = 0,
            completedTasksCount = 0,
            verificationStatus = VerificationStatus.INTERVIEW_PENDING,
            aiVettingSummary = "Registered on Kivulu. Ready for AI Vetting Interview.",
            hourlyRate = rate.ifBlank { "UGX 35,000 / hr" },
            bio = "Independent $category specialist ready to assist clients on Kivulu."
        )

        repository.addWorker(newWorker)
        _uiState.update { it.copy(myWorkerProfile = newWorker) }
        startVettingInterview(category)
    }
}
