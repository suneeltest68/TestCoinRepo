package com.example.backtest.strategy

import com.example.backtest.model.*
import java.time.LocalDate
import java.time.LocalTime

class OrbStrategy(val durationMinutes: Int) : Strategy {
    override val name: String = "ORB $durationMinutes-Min"

    private val rangeStartTime = LocalTime.of(9, 15)
    private val rangeEndTime = rangeStartTime.plusMinutes(durationMinutes.toLong())

    private var lastProcessedDate: LocalDate? = null
    private var rangeHigh = 0.0
    private var rangeLow = Double.MAX_VALUE
    private var longTakenToday = false
    private var shortTakenToday = false

    override fun generateSignal(index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal? {
        val candle = candles[index]
        val currentDate = candle.timestamp.toLocalDate()
        val currentTime = candle.timestamp.toLocalTime()

        if (currentDate != lastProcessedDate) {
            lastProcessedDate = currentDate
            rangeHigh = 0.0
            rangeLow = Double.MAX_VALUE
            longTakenToday = false
            shortTakenToday = false
        }

        if (currentTime.isAfter(rangeStartTime.minusMinutes(1)) && currentTime.isBefore(rangeEndTime.plusMinutes(1))) {
            if (candle.high > rangeHigh) rangeHigh = candle.high
            if (candle.low < rangeLow) rangeLow = candle.low
        }

        if (currentTime.isAfter(rangeEndTime)) {
            if (!longTakenToday && candle.close > rangeHigh) {
                longTakenToday = true
                return createSignal(SignalType.LONG, index, candles, indicators)
            }
            if (!shortTakenToday && candle.close < rangeLow) {
                shortTakenToday = true
                return createSignal(SignalType.SHORT, index, candles, indicators)
            }
        }
        return null
    }

    private fun createSignal(type: SignalType, index: Int, candles: List<Candle>, indicators: IndicatorResults): Signal {
        val candle = candles[index]
        val atr = indicators.get("ATR_14", index)
        val slPrice = if (type == SignalType.LONG) candle.close - atr else candle.close + atr
        val risk = Math.abs(candle.close - slPrice)
        val targetPrice = if (type == SignalType.LONG) candle.close + (risk * 2.0) else candle.close - (risk * 2.0)

        return Signal(type, candle.timestamp, candle.close, mapOf("stopLoss" to slPrice, "target" to targetPrice))
    }
}
