package com.example.jaboard.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class LrclibResponse(
  val id: Long = 0L,
  @Json(name = "name") val name: String? = null,
  @Json(name = "trackName") val trackName: String? = null,
  @Json(name = "artistName") val artistName: String? = null,
  @Json(name = "albumName") val albumName: String? = null,
  @Json(name = "duration") val duration: Double? = null,
  @Json(name = "instrumental") val instrumental: Boolean = false,
  @Json(name = "plainLyrics") val plainLyrics: String? = null,
  @Json(name = "syncedLyrics") val syncedLyrics: String? = null,
)

interface LrclibService {
  @GET("api/get")
  suspend fun getLyrics(
    @Query("track_name") trackName: String,
    @Query("artist_name") artistName: String,
    @Query("album_name") albumName: String? = null,
    @Query("duration") durationSeconds: Int? = null,
  ): LrclibResponse

  @GET("api/search")
  suspend fun searchLyrics(
    @Query("q") query: String,
  ): List<LrclibResponse>
}

data class ParsedLyricLine(
  val timestampMs: Long,
  val text: String,
)

data class ParsedLyrics(
  val trackName: String,
  val artistName: String,
  val isInstrumental: Boolean,
  val plainLyrics: String?,
  val syncedLines: List<ParsedLyricLine>,
)

class LrclibRepository {

  private val moshi = Moshi.Builder().build()

  private val okHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()

  private val service =
    Retrofit.Builder()
      .baseUrl("https://lrclib.net/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(LrclibService::class.java)

  suspend fun fetchLyrics(
    trackName: String,
    artistName: String,
    durationMs: Long? = null,
  ): Result<ParsedLyrics> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanTrack = trackName.trim().replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()
      val cleanArtist = artistName.trim()
      val durationSec = durationMs?.let { (it / 1000).toInt() }?.takeIf { it > 0 }

      val direct = runCatching {
        service.getLyrics(
          trackName = cleanTrack,
          artistName = cleanArtist,
          durationSeconds = durationSec,
        )
      }.getOrNull()

      val response = direct ?: runCatching {
        val query = "$cleanArtist $cleanTrack".trim()
        service.searchLyrics(query).firstOrNull()
      }.getOrNull()

      if (response == null) {
        throw NoSuchElementException("No lyrics found on LRCLIB for '$cleanTrack'")
      }

      parseResponse(response, cleanTrack, cleanArtist)
    }
  }

  private fun parseResponse(
    response: LrclibResponse,
    defaultTrack: String,
    defaultArtist: String,
  ): ParsedLyrics {
    val synced = response.syncedLyrics
    val parsedLines = mutableListOf<ParsedLyricLine>()

    if (!synced.isNullOrBlank()) {
      val timeRegex = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})\](.*)""")
      synced.lineSequence().forEach { line ->
        val match = timeRegex.matchEntire(line.trim())
        if (match != null) {
          val (minStr, secStr, fracStr, text) = match.destructured
          val minutes = minStr.toLongOrNull() ?: 0L
          val seconds = secStr.toLongOrNull() ?: 0L
          val fracMs = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else (fracStr.toLongOrNull() ?: 0L)
          val totalMs = (minutes * 60_000L) + (seconds * 1000L) + fracMs
          parsedLines.add(ParsedLyricLine(totalMs, text.trim()))
        }
      }
    }

    return ParsedLyrics(
      trackName = response.trackName ?: defaultTrack,
      artistName = response.artistName ?: defaultArtist,
      isInstrumental = response.instrumental,
      plainLyrics = response.plainLyrics,
      syncedLines = parsedLines,
    )
  }
}
