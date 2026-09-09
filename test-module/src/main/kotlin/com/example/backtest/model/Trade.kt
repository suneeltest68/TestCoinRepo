package com.example.backtest.model

import java.time.LocalDateTime

enum class TradeSide { LONG, SHORT }

enum class ExitReason { SL, TARGET, TRAILING_SL, TIME, STRATEGY }

/**
 * Represents a completed or open trade in the backtest.
 */
data class Trade(
    val id: String,
    val entryTime: LocalDateTime,
    var exitTime: LocalDateTime? = null,
    val entryPrice: Double,
    var exitPrice: Double? = null,
    val side: TradeSide,
    var quantity: Int = 0,
    var pnl: Double = 0.0,
    var pnlPercentage: Double = 0.0,
    var capitalBefore: Double = 0.0,
    var capitalAfter: Double = 0.0,
    var status: String = "OPEN",
    var exitReason: ExitReason? = null,
    var maxPriceDuringTrade: Double = 0.0,
    var minPriceDuringTrade: Double = Double.MAX_VALUE,
    var metadata: Map<String, Any> = emptyMap()
)
