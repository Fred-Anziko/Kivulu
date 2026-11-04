package com.example.data.model

enum class InterviewStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED_PASSED,
    NEEDS_REVIEW
}

data class InterviewQuestion(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class VettingInterview(
    val id: String,
    val workerId: String,
    val workerName: String,
    val category: String,
    val questions: List<InterviewQuestion>,
    val currentQuestionIndex: Int = 0,
    val userAnswers: Map<Int, Int> = emptyMap(),
    val score: Int = 0,
    val status: InterviewStatus = InterviewStatus.NOT_STARTED,
    val aiAssessmentSummary: String = ""
)
