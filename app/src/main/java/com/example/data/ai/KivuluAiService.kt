package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.*
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KivuluAiService {

    private val generativeModel: GenerativeModel? by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )
        } else null
    }

    suspend fun processClientAgentQuery(
        userPrompt: String,
        workers: List<GigWorker>,
        existingTasks: List<ClientTask>
    ): ChatMessage = withContext(Dispatchers.IO) {
        val lower = userPrompt.lowercase()

        // Check if user wants to post a task or find a worker
        val matchedWorker = workers.firstOrNull { worker ->
            lower.contains(worker.category.lowercase()) ||
            lower.contains(worker.name.lowercase()) ||
            worker.skills.any { lower.contains(it.lowercase()) }
        } ?: workers.firstOrNull()

        var aiText = ""
        val model = generativeModel

        if (model != null) {
            try {
                val prompt = """
                    You are Kivulu Client Assistant (gigClientAgent). 
                    Your job is to help clients post tasks, discover nearby vetted gig workers, match skilled workers, and schedule calendar appointments.
                    Available Vetted Workers: ${workers.joinToString { "${it.name} (${it.category}, ${it.rating}★, ${it.location})" }}
                    
                    User message: "$userPrompt"
                    
                    Provide a concise, professional response. If the user wants to post or schedule a task, state the recommended worker and proposed time.
                """.trimIndent()
                val response = model.generateContent(prompt)
                aiText = response.text ?: ""
            } catch (e: Exception) {
                aiText = ""
            }
        }

        if (aiText.isBlank()) {
            // Intelligent fallback engine matching gigClientAgent rules
            aiText = when {
                lower.contains("plumb") || lower.contains("leak") || lower.contains("pipe") -> {
                    val worker = workers.find { it.category == "Plumbing" } ?: matchedWorker
                    "Hello! I am your Kivulu Client Assistant. For your plumbing task, I strongly recommend ${worker?.name} (${worker?.location}). They have a ${worker?.rating}★ rating and are vetted in leak repair & installation. Would you like me to schedule this task into their Google Calendar?"
                }
                lower.contains("electric") || lower.contains("power") || lower.contains("breaker") || lower.contains("wire") -> {
                    val worker = workers.find { it.category == "Electrical" } ?: matchedWorker
                    "Hello! I am Kivulu Client Assistant. I've matched your request with ${worker?.name} (${worker?.category}, ${worker?.rating}★). They carry certified testing equipment and have completed ${worker?.completedTasksCount} gigs. Shall we lock in a schedule for tomorrow at 10:00 AM?"
                }
                lower.contains("tech") || lower.contains("wifi") || lower.contains("network") || lower.contains("router") -> {
                    val worker = workers.find { it.category == "IT & Tech" } ?: matchedWorker
                    "I can certainly help you with IT infrastructure! ${worker?.name} is our top-rated tech technician in ${worker?.location}. Would you like to post this task with a budget of UGX 150,000?"
                }
                lower.contains("schedule") || lower.contains("calendar") || lower.contains("book") -> {
                    "Task scheduling initiated! I have prepared a Google Calendar invite for ${matchedWorker?.name}. Start time proposed: Tomorrow 10:00 AM - 12:00 PM. A calendar confirmation link will be attached to your task!"
                }
                else -> {
                    "Hello! I am Kivulu Client Assistant. Describe the task you need help with (e.g. electrical wiring, plumbing leak, router setup, deep cleaning), and I will match you with a vetted local professional and sync your Google Calendar."
                }
            }
        }

        ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            sender = ChatSender.CLIENT_AGENT,
            senderTitle = "gigClientAgent (Client Assistant)",
            message = aiText,
            recommendedWorker = matchedWorker,
            actionSuggestions = listOf(
                "Post New Task with AI",
                "Schedule with ${matchedWorker?.name ?: "Worker"}",
                "Browse All Vetted Workers"
            )
        )
    }

    suspend fun processWorkerAgentQuery(
        userPrompt: String,
        currentWorker: GigWorker?
    ): ChatMessage = withContext(Dispatchers.IO) {
        val lower = userPrompt.lowercase()
        var aiText = ""
        val model = generativeModel

        if (model != null) {
            try {
                val prompt = """
                    You are Kivulu Gig Worker Assistant (gigWorkerAgent).
                    Your job is to onboard gig workers, assess and verify their skills and reliability, conduct interactive vetting interviews, and assist with calendar synchronization.
                    Worker Name: ${currentWorker?.name ?: "New Worker"}
                    Verification Status: ${currentWorker?.verificationStatus ?: "UNVERIFIED"}
                    
                    User message: "$userPrompt"
                    
                    Provide a motivating, precise response guiding the worker through onboarding or skill vetting.
                """.trimIndent()
                val response = model.generateContent(prompt)
                aiText = response.text ?: ""
            } catch (e: Exception) {
                aiText = ""
            }
        }

        if (aiText.isBlank()) {
            aiText = when {
                lower.contains("vet") || lower.contains("quiz") || lower.contains("test") || lower.contains("interview") -> {
                    "Welcome to Kivulu Worker Vetting! I will conduct a 3-question interactive skill assessment for ${currentWorker?.category ?: "your trade"}. Upon scoring above 60%, you'll earn the 'Kivulu Vetted & Approved' badge and direct client booking access!"
                }
                lower.contains("calendar") || lower.contains("auth") || lower.contains("google") -> {
                    "Google Calendar synchronization enabled! When clients book your services, Kivulu automatically reserves time slots on your calendar to prevent double bookings."
                }
                lower.contains("skills") || lower.contains("equipment") || lower.contains("onboard") -> {
                    "Great to have you on Kivulu! Please list your primary trade skills (e.g. Solar Wiring, CCTV, Leak Repair) and equipment so we can match high-paying local tasks to your profile."
                }
                else -> {
                    "Hello ${currentWorker?.name ?: "Worker"}! I am your Kivulu Worker Assistant. I am here to help you get vetted, manage task interest, verify tools, and boost your earnings."
                }
            }
        }

        ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            sender = ChatSender.WORKER_AGENT,
            senderTitle = "gigWorkerAgent (Worker Assistant)",
            message = aiText,
            actionSuggestions = listOf(
                "Start AI Vetting Interview",
                "Link Google Calendar",
                "Update Skill Matrix"
            )
        )
    }
}
