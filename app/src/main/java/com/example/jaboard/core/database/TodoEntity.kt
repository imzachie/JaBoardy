package com.example.jaboard.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TodoPriority(val label: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High"),
}

@Entity(tableName = "todos")
data class TodoEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val notes: String = "",
  val isCompleted: Boolean = false,
  val priority: String = TodoPriority.MEDIUM.name,
  val category: String = "General",
  val createdAt: Long = System.currentTimeMillis(),
)
