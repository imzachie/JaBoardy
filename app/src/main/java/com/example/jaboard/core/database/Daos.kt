package com.example.jaboard.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
  @Query("SELECT * FROM dashboard_cards ORDER BY sortOrder ASC")
  fun observeCards(): Flow<List<DashboardCardEntity>>

  @Query("SELECT COUNT(*) FROM dashboard_cards")
  suspend fun countCards(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCards(cards: List<DashboardCardEntity>)

  @Query("DELETE FROM dashboard_cards")
  suspend fun deleteAll()
}

@Dao
interface NoteDao {
  @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
  fun observeAllNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  suspend fun getNoteById(id: Long): NoteEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNote(note: NoteEntity): Long

  @Update
  suspend fun updateNote(note: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNoteById(id: Long)

  @Query("DELETE FROM notes")
  suspend fun deleteAllNotes()
}

@Dao
interface TodoDao {
  @Query("SELECT * FROM todos ORDER BY isCompleted ASC, createdAt DESC")
  fun observeAllTodos(): Flow<List<TodoEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTodo(todo: TodoEntity): Long

  @Query("UPDATE todos SET isCompleted = :completed WHERE id = :id")
  suspend fun setCompleted(id: Long, completed: Boolean)

  @Query("DELETE FROM todos WHERE id = :id")
  suspend fun deleteTodoById(id: Long)

  @Query("DELETE FROM todos WHERE isCompleted = 1")
  suspend fun deleteCompletedTodos()

  @Query("DELETE FROM todos")
  suspend fun deleteAllTodos()
}

@Dao
interface WeatherCacheDao {
  @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey LIMIT 1")
  fun observeWeather(locationKey: String): Flow<WeatherCacheEntity?>

  @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey LIMIT 1")
  suspend fun getWeather(locationKey: String): WeatherCacheEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWeather(entity: WeatherCacheEntity)

  @Query("DELETE FROM weather_cache")
  suspend fun clearAll()
}
