package com.example.backtest.strategy

import com.example.backtest.model.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * 1. 15-Minute ORB + VWAP
 */
class OrbVwapStrategy : Strategy {
    override val name: String = "ORB 15-Min + VWAP"
    private var lastDate: LocalDate? = null
    private var orbHigh = 0.0
    private var orbLow = Double.MAX_VALUE
    private var tradeTakenToday = false

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val candle = candles[index]
        val date = candle.timestamp.toLocalDate()
        val time = candle.timestamp.toLocalTime()

        if (date != lastDate) {
            lastDate = date; orbHigh = 0.0; orbLow = Double.MAX_VALUE; tradeTakenToday = false
        }

        // Mark ORB Range (9:15 to 9:30)
        if (time >= LocalTime.of(9, 15) && time <= LocalTime.of(9, 30)) {
            if (candle.high > orbHigh) orbHigh = candle.high
            if (candle.low < orbLow) orbLow = candle.low
        }

        // Entry window (9:31 to 11:30)
        if (!tradeTakenToday && time > LocalTime.of(9, 30) && time < LocalTime.of(11, 30)) {
            if (orbHigh > 0 && candle.close > orbHigh) {
                tradeTakenToday = true
                return Signal(SignalType.LONG, candle.timestamp, candle.close, mapOf("stopLoss" to candle.close - 50.0, "target" to candle.close + 100.0))
            }
            if (orbLow < Double.MAX_VALUE && candle.close < orbLow) {
                tradeTakenToday = true
                return Signal(SignalType.SHORT, candle.timestamp, candle.close, mapOf("stopLoss" to candle.close + 50.0, "target" to candle.close - 100.0))
            }
        }
        return null
    }
}

/**
 * 2. Supertrend (7, 3) + 200 EMA
 */
class SupertrendEmaStrategy : Strategy {
    override val name: String = "Supertrend (7,3) + 200 EMA"
    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        if (index < 1) return null
        val candle = candles[index]
        val stDir = indicators.get("ST_DIR_7_3.0", index)
        val prevStDir = indicators.get("ST_DIR_7_3.0", index - 1)
        val ema200 = indicators.get("EMA_200", index)

        if (candle.close > ema200 && prevStDir < 0 && stDir > 0) {
            return Signal(SignalType.LONG, candle.timestamp, candle.close)
        }
        if (candle.close < ema200 && prevStDir > 0 && stDir < 0) {
            return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        }
        return null
    }
}

/**
 * 3. Bollinger Band + RSI Scalp
 */
class BbRsiScalpStrategy : Strategy {
    override val name: String = "BB + RSI Scalp"
    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val candle = candles[index]
        val upper = indicators.get("BB_UPPER_20_2.0", index)
        val lower = indicators.get("BB_LOWER_20_2.0", index)
        val rsi = indicators.get("RSI_14", index)

        if (candle.low <= lower && rsi < 35) {
            return Signal(SignalType.LONG, candle.timestamp, candle.close, mapOf("stopLoss" to candle.close - 30.0, "target" to candle.close + 60.0))
        }
        if (candle.high >= upper && rsi > 65) {
            return Signal(SignalType.SHORT, candle.timestamp, candle.close, mapOf("stopLoss" to candle.close + 30.0, "target" to candle.close - 60.0))
        }
        return null
    }
}

/**
 * 4. CPR Retest & Fade
 */
class CprStrategy(val cprMap: Map<LocalDate, DailyLevels>) : Strategy {
    override val name: String = "CPR Retest & Fade"
    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val candle = candles[index]
        val levels = cprMap[candle.timestamp.toLocalDate()] ?: return null
        
        if (candle.low <= levels.tc && candle.close > levels.tc) {
            return Signal(SignalType.LONG, candle.timestamp, candle.close, mapOf("stopLoss" to levels.bc, "target" to levels.r1))
        }
        if (candle.high >= levels.bc && candle.close < levels.bc) {
            return Signal(SignalType.SHORT, candle.timestamp, candle.close, mapOf("stopLoss" to levels.tc, "target" to levels.s1))
        }
        return null
    }
}

/**
 * 5. Dual Timeframe MACD + EMA Pullback
 */
class DualTfStrategy : Strategy {
    override val name: String = "Dual TF MACD + EMA"
    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        if (index < 1) return null
        val macd = indicators.get("MACD_5M", index)
        val sig = indicators.get("MACD_SIGNAL_5M", index)
        val ema20 = indicators.get("EMA_20", index)
        val candle = candles[index]

        if (macd > sig && candle.low <= ema20 && candle.close > ema20) {
            return Signal(SignalType.LONG, candle.timestamp, candle.close)
        }
        if (macd < sig && candle.high >= ema20 && candle.close < ema20) {
            return Signal(SignalType.SHORT, candle.timestamp, candle.close)
        }
        return null
    }
}
