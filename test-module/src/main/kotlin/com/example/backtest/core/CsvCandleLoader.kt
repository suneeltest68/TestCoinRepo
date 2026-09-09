package com.example.backtest.core

import com.example.backtest.model.Candle
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

/**
 * High-performance CSV Candle loader.
 * Designed to handle large Nifty CSV files efficiently.
 */
class CsvCandleLoader {

    // Format: 2026-06-01 09:15:00+05:30
    // Using OffsetDateTime to handle the +05:30 timezone offset
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")

    /**
     * Loads candles from a CSV input stream.
     * Expected format: Close,High,Low,Open,timestamp,Volume
     * @param inputStream The stream of the CSV file.
     * @return List of parsed Candle objects.
     */
    fun load(inputStream: InputStream): List<Candle> {
        val startTime = System.currentTimeMillis()
        val candles = ArrayList<Candle>(2_500_000)

        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readLine() // Skip header
            
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        try {
                            val close = parts[0].toDouble()
                            val high = parts[1].toDouble()
                            val low = parts[2].toDouble()
                            val open = parts[3].toDouble()
                            val timestampStr = parts[4]
                            val volume = parts[5].toDouble().toLong()

                            // Convert to LocalDateTime (discarding offset for engine simplicity)
                            val ldt = OffsetDateTime.parse(timestampStr, dateTimeFormatter).toLocalDateTime()

                            candles.add(Candle(ldt, open, high, low, close, volume))
                        } catch (e: Exception) {
                            // Log or skip malformed lines
                            println(e.cause?.message)
                        }
                    }
                }
                line = reader.readLine()
            }
        }

        val endTime = System.currentTimeMillis()
        println("Successfully loaded ${candles.size} candles from CSV.")
        println("Execution Time: ${endTime - startTime} ms")
        
        return candles
    }
}
