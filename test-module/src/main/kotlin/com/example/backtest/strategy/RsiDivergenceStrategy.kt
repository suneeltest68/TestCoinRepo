package com.example.backtest.strategy

import com.example.backtest.model.*

/**
 * Simple RSI Based Strategy (Oversold/Overbought)
 */
class RsiStrategy(val period: Int) : Strategy {
    override val name: String = "RSI $period"

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val rsi = indicators.get("RSI_$period", index)
        val candle = candles[index]

        if (rsi < 30) return Signal(SignalType.LONG, candle.timestamp, candle.close)
        if (rsi > 70) return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        return null
    }
}
