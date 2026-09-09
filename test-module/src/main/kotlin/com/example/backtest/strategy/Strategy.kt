package com.example.backtest.strategy

import com.example.backtest.model.Candle
import com.example.backtest.model.IndicatorResults
import com.example.backtest.model.Signal

/**
 * Core interface for all trading strategies.
 * Implementing classes can define logic for EMA Cross, ORB, SuperTrend, etc.
 */
interface Strategy {
    /**
     * Unique name of the strategy for reporting and tracking.
     */
    val name: String

    /**
     * Analyzes market data at a specific index and generates a trading signal.
     *
     * @param index The current bar index being processed.
     * @param candles The historical list of candles for reference.
     * @param indicators The cached indicator results (EMA, RSI, SuperTrend, etc.).
     * @return A [Signal] if entry or exit conditions are met, otherwise null.
     */
    fun generateSignal(
        index: Int,
        candles: List<Candle>,
        indicators: IndicatorResults
    ): Signal?
}
