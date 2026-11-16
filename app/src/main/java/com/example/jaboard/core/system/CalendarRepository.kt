package com.example.jaboard.core.system

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarEventItem(
  val id: Long,
  val title: String,
  val startMillis: Long,
  val endMillis: Long,
  val allDay: Boolean,
  val location: String?,
  val calendarColor: Int,
)

class CalendarRepository(private val context: Context) {

  fun hasCalendarPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED

  suspend fun loadUpcomingEvents(daysAhead: Int = 14): List<CalendarEventItem> =
    withContext(Dispatchers.IO) {
      if (!hasCalendarPermission()) return@withContext emptyList()

      val startMillis = System.currentTimeMillis()
      val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysAhead) }
      val endMillis = endCal.timeInMillis

      val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
      ContentUris.appendId(builder, startMillis)
      ContentUris.appendId(builder, endMillis)

      val projection =
        arrayOf(
          CalendarContract.Instances.EVENT_ID,
          CalendarContract.Instances.TITLE,
          CalendarContract.Instances.BEGIN,
          CalendarContract.Instances.END,
          CalendarContract.Instances.ALL_DAY,
          CalendarContract.Instances.EVENT_LOCATION,
          CalendarContract.Instances.DISPLAY_COLOR,
        )

      val events = mutableListOf<CalendarEventItem>()
      runCatching {
        context.contentResolver
          .query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
          )
          ?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)

            while (cursor.moveToNext() && events.size < 40) {
              val title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() } ?: "(Untitled event)"
              events.add(
                CalendarEventItem(
                  id = cursor.getLong(idIdx),
                  title = title,
                  startMillis = cursor.getLong(beginIdx),
                  endMillis = cursor.getLong(endIdx),
                  allDay = cursor.getInt(allDayIdx) == 1,
                  location = cursor.getString(locIdx)?.takeIf { it.isNotBlank() },
                  calendarColor = cursor.getInt(colorIdx),
                )
              )
            }
          }
      }
      events
    }
}
