package com.example.model

enum class TaskCategory(val label: String) {
    RUTINA("Rutina"),
    COMPRAS("Compras"),
    HOGAR("Hogar"),
    TRABAJO("Trabajo")
}

data class DailyTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val isCompleted: Boolean = false,
    val timeTag: String
)
