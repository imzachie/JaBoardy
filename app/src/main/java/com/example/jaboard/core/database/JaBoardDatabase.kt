package com.example.jaboard.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities =
    [
      DashboardCardEntity::class,
      NoteEntity::class,
      TodoEntity::class,
      WeatherCacheEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
abstract class JaBoardDatabase : RoomDatabase() {
  abstract fun dashboardDao(): DashboardDao

  abstract fun noteDao(): NoteDao

  abstract fun todoDao(): TodoDao

  abstract fun weatherCacheDao(): WeatherCacheDao

  companion object {
    @Volatile private var INSTANCE: JaBoardDatabase? = null

    fun getInstance(context: Context): JaBoardDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                JaBoardDatabase::class.java,
                "jaboard.db",
              )
              .build()
              .also { INSTANCE = it }
        }
  }
}
