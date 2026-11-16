package com.example.jaboard.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities =
    [
      DashboardCardEntity::class,
      NoteEntity::class,
      TodoEntity::class,
      WeatherCacheEntity::class,
      WeatherAiSummaryEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
abstract class JaBoardDatabase : RoomDatabase() {
  abstract fun dashboardDao(): DashboardDao

  abstract fun noteDao(): NoteDao

  abstract fun todoDao(): TodoDao

  abstract fun weatherCacheDao(): WeatherCacheDao

  abstract fun weatherAiSummaryDao(): WeatherAiSummaryDao

  companion object {
    @Volatile private var INSTANCE: JaBoardDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
          CREATE TABLE IF NOT EXISTS weather_ai_summary (
            locationKey TEXT NOT NULL PRIMARY KEY,
            summaryText TEXT NOT NULL,
            generatedAt INTEGER NOT NULL
          )
        """.trimIndent())
      }
    }

    fun getInstance(context: Context): JaBoardDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                JaBoardDatabase::class.java,
                "jaboard.db",
              )
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { INSTANCE = it }
        }
  }
}
