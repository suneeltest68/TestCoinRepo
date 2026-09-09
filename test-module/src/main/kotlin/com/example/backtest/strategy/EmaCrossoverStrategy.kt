package com.example.backtest.strategy

import com.example.backtest.model.*

/**
 * Fixed EMA Crossover Strategy (Event-based)
 */
class EmaCrossoverStrategy(val fast: Int, val slow: Int) : Strategy {
    override val name: String = "EMA Crossover $fast/$slow"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        if (index < 1) return null
        
        val fCurr = indicators.get("EMA_$fast", index)
        val sCurr = indicators.get("EMA_$slow", index)
        val fPrev = indicators.get("EMA_$fast", index - 1)
        val sPrev = indicators.get("EMA_$slow", index - 1)
        
        val candle = candles[index]

        // Only signal on the EXACT CROSS
        if (fPrev <= sPrev && fCurr > sCurr) {
            return Signal(SignalType.LONG, candle.timestamp, candle.close)
        }
        
        if (fPrev >= sPrev && fCurr < sCurr) {
            return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        }
        
        return null
    }
}
