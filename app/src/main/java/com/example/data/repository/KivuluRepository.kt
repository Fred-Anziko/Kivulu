package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class KivuluRepository {

    private val _workers = MutableStateFlow<List<GigWorker>>(initialWorkers())
    val workers: StateFlow<List<GigWorker>> = _workers.asStateFlow()

    private val _tasks = MutableStateFlow<List<ClientTask>>(initialTasks())
    val tasks: StateFlow<List<ClientTask>> = _tasks.asStateFlow()

    private val _activeInterview = MutableStateFlow<VettingInterview?>(null)
    val activeInterview: StateFlow<VettingInterview?> = _activeInterview.asStateFlow()

    fun addWorker(worker: GigWorker) {
        _workers.value = listOf(worker) + _workers.value
    }

    fun updateWorkerVerification(workerId: String, status: VerificationStatus, summary: String) {
        _workers.value = _workers.value.map { worker ->
            if (worker.id == workerId) {
                worker.copy(
                    verificationStatus = status,
                    aiVettingSummary = summary,
                    reviewCount = worker.reviewCount + 1
                )
            } else worker
        }
    }

    fun addTask(task: ClientTask): ClientTask {
        _tasks.value = listOf(task) + _tasks.value
        return task
    }

    fun matchWorkerToTask(taskId: String, worker: GigWorker, scheduledTime: String): ClientTask {
        val calendarLink = "https://calendar.google.com/calendar/r/eventedit?text=${taskTitleUrl(taskId)}&dates=20251110T100000Z/20251110T120000Z&details=Kivulu+Scheduled+Gig+Task+with+${worker.name}"
        var updatedTask: ClientTask? = null

        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                val t = task.copy(
                    status = TaskStatus.MATCHED,
                    assignedWorkerId = worker.id,
                    assignedWorkerName = worker.name,
                    scheduledTime = scheduledTime,
                    calendarEventLink = calendarLink
                )
                updatedTask = t
                t
            } else task
        }

        // Increment worker completed tasks
        _workers.value = _workers.value.map { w ->
            if (w.id == worker.id) {
                w.copy(completedTasksCount = w.completedTasksCount + 1)
            } else w
        }

        return updatedTask ?: _tasks.value.first()
    }

    fun completeTaskAndRate(taskId: String, rating: Int, reviewText: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    status = TaskStatus.COMPLETED,
                    rating = rating,
                    review = reviewText
                )
            } else task
        }
    }

    fun startVettingInterview(workerId: String, category: String): VettingInterview {
        val worker = _workers.value.find { it.id == workerId }
        val workerName = worker?.name ?: "Gig Worker"
        val questions = getQuestionsForCategory(category)
        
        val interview = VettingInterview(
            id = UUID.randomUUID().toString(),
            workerId = workerId,
            workerName = workerName,
            category = category,
            questions = questions,
            currentQuestionIndex = 0,
            userAnswers = emptyMap(),
            score = 0,
            status = InterviewStatus.IN_PROGRESS
        )
        _activeInterview.value = interview
        return interview
    }

    fun submitAnswer(questionIndex: Int, optionIndex: Int): VettingInterview? {
        val current = _activeInterview.value ?: return null
        val newAnswers = current.userAnswers.toMutableMap()
        newAnswers[questionIndex] = optionIndex

        val nextIndex = questionIndex + 1
        val isFinished = nextIndex >= current.questions.size

        var totalCorrect = 0
        newAnswers.forEach { (qIdx, selectedOpt) ->
            if (current.questions.getOrNull(qIdx)?.correctOptionIndex == selectedOpt) {
                totalCorrect++
            }
        }
        val computedScore = ((totalCorrect.toDouble() / current.questions.size) * 100).toInt()

        val updatedStatus = if (isFinished) {
            if (computedScore >= 60) InterviewStatus.COMPLETED_PASSED else InterviewStatus.NEEDS_REVIEW
        } else InterviewStatus.IN_PROGRESS

        val summary = if (isFinished) {
            if (computedScore >= 60) {
                "AI Vetting Passed (${computedScore}% score). Worker demonstrated strong competence in $current.category procedures, safety protocols, and domain troubleshooting."
            } else {
                "AI Vetting Score: ${computedScore}%. Recommended further practical assessment for $current.category."
            }
        } else ""

        val updatedInterview = current.copy(
            userAnswers = newAnswers,
            currentQuestionIndex = if (isFinished) questionIndex else nextIndex,
            score = computedScore,
            status = updatedStatus,
            aiAssessmentSummary = summary
        )

        _activeInterview.value = updatedInterview

        if (isFinished && computedScore >= 60) {
            updateWorkerVerification(current.workerId, VerificationStatus.VETTED_APPROVED, summary)
        }

        return updatedInterview
    }

    private fun taskTitleUrl(taskId: String): String {
        return "KivuluTask_${taskId.take(6)}"
    }

    private fun getQuestionsForCategory(category: String): List<InterviewQuestion> {
        return when (category.lowercase()) {
            "electrical" -> listOf(
                InterviewQuestion(
                    id = 1,
                    questionText = "Before troubleshooting a tripping main breaker in a commercial panel, what safety step is required first?",
                    options = listOf(
                        "Replace breaker immediately",
                        "Isolate power, lock out/tag out, and verify voltage with a calibrated multimeter",
                        "Increase breaker amperage rating",
                        "Bypass the surge protector"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Always de-energize and verify zero voltage prior to physical inspection."
                ),
                InterviewQuestion(
                    id = 2,
                    questionText = "What wire color is standard for equipment grounding in standard AC single-phase installations?",
                    options = listOf("Red or Brown", "Black", "Green or Bare Copper", "Blue"),
                    correctOptionIndex = 2,
                    explanation = "Green or bare copper wire is universally reserved for protective earth grounding."
                ),
                InterviewQuestion(
                    id = 3,
                    questionText = "What tool is essential to measure insulation resistance in electrical motors and cables?",
                    options = listOf("Clamp Meter", "Megohmmeter (Megger)", "Oscilloscope", "Wire Stripper"),
                    correctOptionIndex = 1,
                    explanation = "A Megohmmeter tests high-voltage insulation integrity."
                )
            )
            "plumbing" -> listOf(
                InterviewQuestion(
                    id = 1,
                    questionText = "How do you treat a high-pressure pipe leak prior to replacing a damaged PVC section?",
                    options = listOf(
                        "Shut off the main supply stop valve, drain residual line water, and dry the joint area",
                        "Wrap tightly with duct tape while pressurized",
                        "Pour cement over the wet pipe",
                        "Heat the pipe with a propane torch while running"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Isolation and pressure release are vital before pipe cutting and solvent bonding."
                ),
                InterviewQuestion(
                    id = 2,
                    questionText = "What sealant is applied to threaded metal pipe fittings to ensure a watertight seal?",
                    options = listOf("Hot Glue", "PTFE / Teflon Thread Tape or Pipe Dope", "Silicone Caulk", "Epoxy resin"),
                    correctOptionIndex = 1,
                    explanation = "PTFE thread sealant prevents fluid seepage across male/female threads."
                )
            )
            else -> listOf(
                InterviewQuestion(
                    id = 1,
                    questionText = "How do you ensure clear communication and job site safety with a new client?",
                    options = listOf(
                        "Proceed without confirmation",
                        "Conduct a pre-task inspection, confirm requirements in writing, and use required PPE",
                        "Let client handle safety hazards",
                        "Complete the task in total silence"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Clear requirements agreement and safety protocols guarantee high customer satisfaction."
                ),
                InterviewQuestion(
                    id = 2,
                    questionText = "What step follows task completion to build long-term reputation on Kivulu?",
                    options = listOf(
                        "Leave without notifying",
                        "Clean up the work area, review output with client, and request task completion confirmation",
                        "Ask for extra unagreed cash",
                        "Keep client tools"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Cleaning work site and client walk-through earns top reviews."
                )
            )
        }
    }

    private fun initialWorkers(): List<GigWorker> {
        return listOf(
            GigWorker(
                id = "w1",
                name = "Fredrick Anziko",
                email = "anzikofred@gmail.com",
                phone = "+256 772 102 938",
                location = "Kampala Central, Nakawa",
                category = "Electrical",
                skills = listOf("Industrial Wiring", "Solar Inverter Setup", "Breaker Panel Repair", "CCTV Installation"),
                equipments = listOf("Fluke Multimeter", "Insulated Toolkit", "Ladder", "Wire Crimpers"),
                experienceYears = 6,
                rating = 4.9,
                reviewCount = 38,
                completedTasksCount = 42,
                verificationStatus = VerificationStatus.VETTED_APPROVED,
                aiVettingSummary = "Vetted & Verified: Passed Kivulu Electrical Competence AI Interview with 95% score. Verified Google Calendar availability.",
                hourlyRate = "UGX 45,000 / hr",
                bio = "Certified electrical technician specializing in home and commercial power setups, emergency repairs, and solar backup systems."
            ),
            GigWorker(
                id = "w2",
                name = "Beatrice Akello",
                email = "beatrice.a@kivulu.org",
                phone = "+256 701 445 921",
                location = "Wandegeya / Makerere",
                category = "IT & Tech",
                skills = listOf("Network Router Config", "PC Hardware Repair", "Smart Home IoT Setup", "Wi-Fi Mesh Installation"),
                equipments = listOf("Cat6 Crimping Kit", "LAN Cable Tester", "Solder Station", "Toolkit"),
                experienceYears = 4,
                rating = 4.8,
                reviewCount = 29,
                completedTasksCount = 31,
                verificationStatus = VerificationStatus.VETTED_APPROVED,
                aiVettingSummary = "Vetted & Verified: Highly proficient in networking, troubleshooting, and smart office devices.",
                hourlyRate = "UGX 50,000 / hr",
                bio = "IT Infrastructure & Network Technician. I quickly diagnose hardware faults, set up fast office Wi-Fi, and configure security cameras."
            ),
            GigWorker(
                id = "w3",
                name = "Moses Mugisha",
                email = "moses.m@kivulu.org",
                phone = "+256 788 331 002",
                location = "Entebbe / Katabi",
                category = "Plumbing",
                skills = listOf("High-Pressure Leak Repair", "Water Tank Installation", "Drain Unclogging", "Water Heater Fittings"),
                equipments = listOf("Pipe Wrench Set", "Drain Snake Cable", "Thread Sealant", "Pressure Gauge"),
                experienceYears = 7,
                rating = 4.95,
                reviewCount = 54,
                completedTasksCount = 60,
                verificationStatus = VerificationStatus.VETTED_APPROVED,
                aiVettingSummary = "Vetted & Verified: Master plumber with exceptional reliability score and zero reported leak returns.",
                hourlyRate = "UGX 40,000 / hr",
                bio = "Master plumber with 7 years on-site experience. Specialized in rapid emergency leak stoppage and modern bathroom fittings."
            ),
            GigWorker(
                id = "w4",
                name = "Sarah Namukasa",
                email = "sarah.n@kivulu.org",
                phone = "+256 752 900 114",
                location = "Bugolobi / Ntinda",
                category = "Cleaning",
                skills = listOf("Deep House Cleaning", "Post-Construction Clean-up", "Carpet Steam Wash", "Office Sanitization"),
                equipments = listOf("Industrial Vacuum Cleaner", "Steam Sanitizer", "Eco-Friendly Solvents"),
                experienceYears = 3,
                rating = 4.85,
                reviewCount = 22,
                completedTasksCount = 25,
                verificationStatus = VerificationStatus.VETTED_APPROVED,
                aiVettingSummary = "Vetted & Verified: Top-rated detail-oriented hygiene and sanitization professional.",
                hourlyRate = "UGX 35,000 / hr",
                bio = "Professional deep cleaning technician. I ensure pristine, sparkling spaces for homes, moving transitions, and corporate offices."
            ),
            GigWorker(
                id = "w5",
                name = "Joseph Kintu",
                email = "joseph.k@kivulu.org",
                phone = "+256 774 219 883",
                location = "Kisasi / Kyebando",
                category = "Carpentry",
                skills = listOf("Custom Cabinetry", "Door Lock & Frame Repair", "Furniture Assembly", "Roof Truss Repair"),
                equipments = listOf("Circular Saw", "Cordless Drill Set", "Wood Router", "Clamps"),
                experienceYears = 5,
                rating = 4.75,
                reviewCount = 18,
                completedTasksCount = 20,
                verificationStatus = VerificationStatus.VETTED_APPROVED,
                aiVettingSummary = "Vetted & Verified: Passed timber work precision assessment and custom furniture fitting.",
                hourlyRate = "UGX 42,000 / hr",
                bio = "Skilled woodworker and carpenter. Passionate about durable custom furniture, precise door alignment, and wood repairs."
            )
        )
    }

    private fun initialTasks(): List<ClientTask> {
        return listOf(
            ClientTask(
                id = "t101",
                clientName = "Dr. David Kato",
                clientEmail = "david.kato@healthcare.ug",
                clientPhone = "+256 701 992 334",
                title = "Emergency Circuit Breaker Tripping Repair",
                description = "Our clinic's main electrical sub-panel trips whenever the air conditioner and medical sterilizer run simultaneously. Need a vetted electrician to inspect load distribution and replace faulty breaker.",
                category = "Electrical",
                location = "Nakawa Medical Center, Kampala",
                deadline = "Urgent (Today)",
                budget = "UGX 120,000",
                status = TaskStatus.OPEN,
                postedAt = "2 hours ago"
            ),
            ClientTask(
                id = "t102",
                clientName = "Grace Tumusiime",
                clientEmail = "grace.t@designstudio.co.ug",
                clientPhone = "+256 782 554 110",
                title = "Office High-Speed Wi-Fi Router & Mesh Setup",
                description = "Configuring 3 mesh access points in our new 2-floor design studio for seamless roaming and secure guest Wi-Fi network isolation.",
                category = "IT & Tech",
                location = "Bugolobi Expressway, Kampala",
                deadline = "Tomorrow 10:00 AM",
                budget = "UGX 150,000",
                status = TaskStatus.MATCHED,
                assignedWorkerId = "w2",
                assignedWorkerName = "Beatrice Akello",
                scheduledTime = "2025-11-05T10:00:00",
                calendarEventLink = "https://calendar.google.com/calendar/r",
                postedAt = "Yesterday"
            ),
            ClientTask(
                id = "t103",
                clientName = "Samuel Ochieng",
                clientEmail = "sochieng@gmail.com",
                clientPhone = "+256 773 881 229",
                title = "Overhead Water Tank Float Valve Replacement",
                description = "The 2,000-liter overhead PVC water tank is overflowing automatically. Need a plumber to install a heavy-duty copper float valve and inspect supply line joints.",
                category = "Plumbing",
                location = "Kyanja Ring Road, Kampala",
                deadline = "In 2 days",
                budget = "UGX 90,000",
                status = TaskStatus.OPEN,
                postedAt = "3 hours ago"
            )
        )
    }
}
