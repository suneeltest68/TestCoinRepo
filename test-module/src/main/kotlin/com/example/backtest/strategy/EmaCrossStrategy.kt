package com.example.backtest.strategy

import com.example.backtest.model.Candle
import com.example.backtest.model.IndicatorResults
import com.example.backtest.model.Signal
import com.example.backtest.model.SignalType

/**
 * Simple EMA Cross Strategy.
 * Entry Long: EMA20 > EMA50
 * Entry Short: EMA20 < EMA50
 */
class EmaCrossStrategy : Strategy {
    override val name: String = "EMA Cross 20/50"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        if (index < 1) return null
        
        val currEma20 = indicators.get("EMA_20", index)
        val currEma50 = indicators.get("EMA_50", index)
        val prevEma20 = indicators.get("EMA_20", index - 1)
        val prevEma50 = indicators.get("EMA_50", index - 1)
        
        // Long Cross
        if (prevEma20 <= prevEma50 && currEma20 > currEma50) {
            return Signal(SignalType.LONG, candles[index].timestamp, candles[index].close)
        }
        
        // Short Cross
        if (prevEma20 >= prevEma50 && currEma20 < currEma50) {
            return Signal(SignalType.SHORT, candles[index].timestamp, candles[index].close)
        }
        
        return null
    }
}
