package com.example.jaboard.core.database

import kotlinx.coroutines.flow.Flow

class DashboardRepository(private val dashboardDao: DashboardDao) {
  val cards: Flow<List<DashboardCardEntity>> = dashboardDao.observeCards()

  suspend fun ensureSeeded() {
    if (dashboardDao.countCards() == 0) {
      dashboardDao.upsertCards(DashboardSeeds.cardsForPreset(DashboardPreset.EXPRESSIVE_BENTO))
    }
  }

  suspend fun saveOrder(orderedCards: List<DashboardCardEntity>) {
    val reindexed = orderedCards.mapIndexed { index, card -> card.copy(sortOrder = index) }
    dashboardDao.upsertCards(reindexed)
  }

  suspend fun setVisibility(cardId: String, visible: Boolean, current: List<DashboardCardEntity>) {
    val updated =
      current.map { if (it.cardId == cardId) it.copy(isVisible = visible) else it }
    dashboardDao.upsertCards(updated)
  }

  suspend fun setCardSize(cardId: String, size: CardSize, current: List<DashboardCardEntity>) {
    val updated =
      current.map { if (it.cardId == cardId) it.copy(cardSize = size.name) else it }
    dashboardDao.upsertCards(updated)
  }

  suspend fun applyPreset(preset: DashboardPreset) {
    dashboardDao.deleteAll()
    dashboardDao.upsertCards(DashboardSeeds.cardsForPreset(preset))
  }
}

class NotesRepository(private val noteDao: NoteDao) {
  val allNotes: Flow<List<NoteEntity>> = noteDao.observeAllNotes()

  suspend fun getById(id: Long): NoteEntity? = noteDao.getNoteById(id)

  suspend fun upsert(note: NoteEntity): Long = noteDao.upsertNote(note)

  suspend fun togglePin(note: NoteEntity) {
    noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
  }

  suspend fun deleteById(id: Long) = noteDao.deleteNoteById(id)

  suspend fun clearAll() = noteDao.deleteAllNotes()
}

class TodoRepository(private val todoDao: TodoDao) {
  val allTodos: Flow<List<TodoEntity>> = todoDao.observeAllTodos()

  suspend fun addTodo(title: String, notes: String, priority: TodoPriority, category: String) {
    todoDao.upsertTodo(
      TodoEntity(
        title = title.trim(),
        notes = notes.trim(),
        priority = priority.name,
        category = category.ifBlank { "General" },
      )
    )
  }

  suspend fun setCompleted(id: Long, completed: Boolean) = todoDao.setCompleted(id, completed)

  suspend fun deleteById(id: Long) = todoDao.deleteTodoById(id)

  suspend fun clearCompleted() = todoDao.deleteCompletedTodos()

  suspend fun clearAll() = todoDao.deleteAllTodos()
}
