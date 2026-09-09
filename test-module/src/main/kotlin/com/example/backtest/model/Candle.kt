package com.example.backtest.model

import java.time.LocalDateTime

/**
 * Represent a single OHLCV data point.
 * Using a data class for immutability and clarity.
 */
data class Candle(
    val timestamp: LocalDateTime,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
