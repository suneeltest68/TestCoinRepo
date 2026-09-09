package com.example.backtest.core

import com.example.backtest.model.Candle
import com.google.gson.stream.JsonReader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

/**
 * High-performance Candle loader using GSON Streaming (JsonReader).
 * Designed to handle 5+ years of 1-minute data without loading the entire string into memory.
 */
class CandleLoader {

    // Default Nifty format, e.g., "2023-01-01 09:15:00"
    // Adjust pattern if your JSON uses a different format
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Loads candles from a large JSON file.
     * @param file The JSON file containing an array of candle objects.
     * @return List of parsed Candle objects.
     */
    fun load(file: File): List<Candle> {
        val startTime = System.currentTimeMillis()
        
        // Pre-size ArrayList if possible, 2.5M is roughly 5 years of 1-min data
        val candles = ArrayList<Candle>(2_500_000)

        if (!file.exists()) {
            throw NoSuchFileException(file, reason = "JSON data file not found")
        }

        // Use BufferedInputStream for faster disk I/O
        JsonReader(InputStreamReader(BufferedInputStream(FileInputStream(file)), "UTF-8")).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                candles.add(readCandle(reader))
            }
            reader.endArray()
        }

        val endTime = System.currentTimeMillis()
        println("Successfully loaded ${candles.size} candles.")
        println("Execution Time: ${endTime - startTime} ms")
        
        return candles
    }

    private fun readCandle(reader: JsonReader): Candle {
        var timestamp: LocalDateTime? = null
        var open = 0.0
        var high = 0.0
        var low = 0.0
        var close = 0.0
        var volume = 0L

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "timestamp" -> {
                    val dateStr = reader.nextString()
                    timestamp = try {
                        LocalDateTime.parse(dateStr, dateTimeFormatter)
                    } catch (e: Exception) {
                        // Fallback to ISO format if custom pattern fails
                        LocalDateTime.parse(dateStr)
                    }
                }
                "open" -> open = reader.nextDouble()
                "high" -> high = reader.nextDouble()
                "low" -> low = reader.nextDouble()
                "close" -> close = reader.nextDouble()
                "volume" -> volume = reader.nextLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Candle(
            timestamp ?: throw IllegalArgumentException("Missing timestamp in JSON object"),
            open, high, low, close, volume
        )
    }
}
