package com.example.data.model

enum class VerificationStatus {
    UNVERIFIED,
    INTERVIEW_PENDING,
    VETTED_APPROVED
}

data class GigWorker(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val location: String,
    val category: String,
    val skills: List<String>,
    val equipments: List<String>,
    val experienceYears: Int,
    val rating: Double,
    val reviewCount: Int,
    val completedTasksCount: Int,
    val verificationStatus: VerificationStatus,
    val aiVettingSummary: String,
    val hourlyRate: String,
    val bio: String,
    val avatarUrl: String = "",
    val googleCalendarConnected: Boolean = true
)
