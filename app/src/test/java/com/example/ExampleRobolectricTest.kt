package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.jaboard.core.database.DashboardPreset
import com.example.jaboard.core.database.DashboardRepository
import com.example.jaboard.core.database.JaBoardDatabase
import com.example.jaboard.core.database.NoteEntity
import com.example.jaboard.core.database.NotesRepository
import com.example.jaboard.core.database.TodoPriority
import com.example.jaboard.core.database.TodoRepository
import com.example.jaboard.core.database.WeatherCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: JaBoardDatabase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, JaBoardDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches JaBoard`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("JaBoard", appName)
  }

  @Test
  fun `dashboard seeds 8 initial cards and switches presets`() = runTest {
    val repo = DashboardRepository(db.dashboardDao())
    repo.ensureSeeded()
    val seeded = repo.cards.first()
    assertEquals(8, seeded.size)
    assertEquals(8, seeded.count { it.isVisible })

    repo.applyPreset(DashboardPreset.MINIMALIST)
    val minimalist = repo.cards.first()
    assertEquals(3, minimalist.count { it.isVisible })
  }

  @Test
  fun `notes repository persists, pins, and deletes notes`() = runTest {
    val repo = NotesRepository(db.noteDao())
    val id1 = repo.upsert(NoteEntity(title = "First", content = "Alpha"))
    val id2 = repo.upsert(NoteEntity(title = "Second", content = "Beta"))

    val saved1 = repo.getById(id1)
    assertNotNull(saved1)
    repo.togglePin(saved1!!)

    val all = repo.allNotes.first()
    assertEquals(2, all.size)
    assertEquals(id1, all.first().id)
    assertTrue(all.first().isPinned)

    repo.deleteById(id2)
    assertEquals(1, repo.allNotes.first().size)
  }

  @Test
  fun `todo repository adds, completes, and clears completed tasks`() = runTest {
    val repo = TodoRepository(db.todoDao())
    repo.addTodo("Buy coffee", "Dark roast", TodoPriority.HIGH, "Errands")
    repo.addTodo("Review PR", "", TodoPriority.MEDIUM, "Work")

    val list = repo.allTodos.first()
    assertEquals(2, list.size)

    val firstId = list.first().id
    repo.setCompleted(firstId, true)
    repo.clearCompleted()

    val remaining = repo.allTodos.first()
    assertEquals(1, remaining.size)
  }

  @Test
  fun `weather cache stores and retrieves offline forecast payload`() = runTest {
    val dao = db.weatherCacheDao()
    dao.upsertWeather(
      WeatherCacheEntity(
        locationKey = "3.14_101.69",
        cityName = "Kuala Lumpur",
        country = "Malaysia",
        latitude = 3.14,
        longitude = 101.69,
        payloadJson = """{"latitude":3.14,"longitude":101.69}""",
        fetchedAt = 123456L,
      )
    )
    val cached = dao.getWeather("3.14_101.69")
    assertNotNull(cached)
    assertEquals("Kuala Lumpur", cached?.cityName)
  }
}
