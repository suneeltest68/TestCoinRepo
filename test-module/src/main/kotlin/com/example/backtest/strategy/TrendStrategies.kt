package com.example.backtest.strategy

import com.example.backtest.model.*
import java.time.LocalTime

/**
 * EMA Trend Strategy: Fast EMA > Slow EMA
 */
class EmaTrendStrategy(val fast: Int, val slow: Int) : Strategy {
    override val name: String = "EMA Trend $fast/$slow"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val f = indicators.get("EMA_$fast", index)
        val s = indicators.get("EMA_$slow", index)
        val candle = candles[index]
        
        if (f > s) return Signal(SignalType.LONG, candle.timestamp, candle.close)
        if (f < s) return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        return null
    }
}

/**
 * SuperTrend Strategy: Direction based on indicator
 */
class SuperTrendStrategy(val period: Int, val multiplier: Double) : Strategy {
    override val name: String = "SuperTrend $period/$multiplier"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val dir = indicators.get("ST_DIR_${period}_$multiplier", index)
        val candle = candles[index]
        
        if (dir > 0) return Signal(SignalType.LONG, candle.timestamp, candle.close)
        if (dir < 0) return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        return null
    }
}

/**
 * VWAP Strategy: Price vs VWAP
 */
class VwapStrategy(val period: Int) : Strategy {
    override val name: String = "VWAP $period"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val vwap = indicators.get("VWAP_$period", index)
        val candle = candles[index]
        
        if (candle.close > vwap) return Signal(SignalType.LONG, candle.timestamp, candle.close)
        if (candle.close < vwap) return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        return null
    }
}
