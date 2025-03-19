package dev.samadali.zen

data class Task(
    val name: String,
    val description: String,
    var isCompleted: Boolean = false
) 