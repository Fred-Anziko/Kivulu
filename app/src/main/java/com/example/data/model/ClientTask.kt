package com.example.data.model

enum class TaskStatus {
    OPEN,
    MATCHED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class ClientTask(
    val id: String,
    val clientName: String,
    val clientEmail: String,
    val clientPhone: String = "+256700123456",
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val deadline: String,
    val budget: String,
    val status: TaskStatus = TaskStatus.OPEN,
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val scheduledTime: String? = null,
    val calendarEventLink: String? = null,
    val postedAt: String = "Today",
    val rating: Int? = null,
    val review: String? = null
)
