package com.example.jaboard.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val content: String,
  val isPinned: Boolean = false,
  val colorTag: String = "DEFAULT",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
