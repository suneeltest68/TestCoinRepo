package com.example.backtest.core

import com.example.backtest.model.*
import com.example.backtest.strategy.Strategy
import java.time.LocalTime
import java.util.*

/**
 * Proper Backtest Engine with Next-Candle Entry and Gap Protection.
 */
class BacktestEngine(
    private val candles: List<Candle>,
    private val strategy: Strategy,
    private val config: BacktestConfig = BacktestConfig(),
    private val precalculatedIndicators: IndicatorResults? = null
) {
    private val trades = mutableListOf<Trade>()
    private var activeTrade: Trade? = null
    private var currentStopLoss: Double = 0.0
    private var targetPrice: Double? = null
    private var currentCapital: Double = 0.0
    
    // Delayed entry state
    private var pendingSignal: Signal? = null

    fun run(initialCapital: Double = 100000.0): List<Trade> {
        if (candles.isEmpty()) return emptyList()
        currentCapital = initialCapital

        val indicators = precalculatedIndicators ?: IndicatorEngine().calculate(candles)

        for (i in 0 until candles.size) {
            val candle = candles[i]
            val currentTime = candle.timestamp.toLocalTime()

            // 1. Process Pending Entry (Execution on Next Candle Open)
            if (pendingSignal != null && activeTrade == null) {
                executePendingEntry(candle)
            }

            if (activeTrade == null) {
                // 2. Generate Signals (at end of current candle)
                if (currentTime.isBefore(config.intradaySquareOffTime) && currentTime.isAfter(config.startTradingTime)) {
                    val signal = strategy.generateSignal(i, candles, indicators)
                    if (signal != null && signal.type != SignalType.EXIT) {
                        pendingSignal = signal
                    }
                }
            } else {
                // 3. Process Active Trade
                val trade = activeTrade!!
                
                // Update stats and check Trailing SL
                updateTradeStats(candle, trade)

                // Check for Exit Conditions (SL, Target, Time)
                val exitReason = checkExitConditions(candle, trade, currentTime)
                
                if (exitReason != null) {
                    closeTrade(candle, trade, exitReason)
                } else {
                    // Check for Strategy Exit (Crossover back)
                    val signal = strategy.generateSignal(i, candles, indicators)
                    if (signal != null && isOppositeSignal(trade.side, signal.type)) {
                        closeTrade(candle, trade, ExitReason.STRATEGY)
                    }
                }
            }
        }
        return trades
    }

    private fun executePendingEntry(candle: Candle) {
        val signal = pendingSignal!!
        val side = if (signal.type == SignalType.LONG) TradeSide.LONG else TradeSide.SHORT
        
        // Execute at OPEN of the candle following the signal
        val rawPrice = candle.open 
        val entryPrice = if (side == TradeSide.LONG) rawPrice * (1 + config.slippagePercentage / 100.0) else rawPrice * (1 - config.slippagePercentage / 100.0)

        // Quantity Calculation (Lot based)
        val totalUnitsPossible = (currentCapital * config.leverage) / entryPrice
        val quantity = (totalUnitsPossible / config.lotSize).toInt() * config.lotSize

        if (quantity > 0) {
            val trade = Trade(UUID.randomUUID().toString(), candle.timestamp, null, entryPrice, null, side, quantity = quantity, capitalBefore = currentCapital)
            
            // Set initial SL
            val metaSl = signal.metadata["stopLoss"] as? Double
            currentStopLoss = metaSl ?: if (side == TradeSide.LONG) entryPrice * (1 - config.stopLossPercentage / 100.0) else entryPrice * (1 + config.stopLossPercentage / 100.0)
            
            // Set initial Target
            val metaTarget = signal.metadata["target"] as? Double
            targetPrice = metaTarget ?: if (side == TradeSide.LONG) entryPrice * (1 + config.targetPercentage / 100.0) else entryPrice * (1 - config.targetPercentage / 100.0)

            activeTrade = trade
        }
        pendingSignal = null
    }

    private fun checkExitConditions(candle: Candle, trade: Trade, currentTime: LocalTime): ExitReason? {
        // A. Time Exit
        if (currentTime.isAfter(config.intradaySquareOffTime) || currentTime == config.intradaySquareOffTime) return ExitReason.TIME

        // B. Stop Loss (with Gap Protection)
        if (trade.side == TradeSide.LONG) {
            if (candle.open <= currentStopLoss) return ExitReason.SL // Gap Down below SL
            if (candle.low <= currentStopLoss) return ExitReason.SL
        } else {
            if (candle.open >= currentStopLoss) return ExitReason.SL // Gap Up above SL
            if (candle.high >= currentStopLoss) return ExitReason.SL
        }

        // C. Target (with Gap Benefit)
        val tp = targetPrice ?: 0.0
        if (tp > 0) {
            if (trade.side == TradeSide.LONG) {
                if (candle.open >= tp || candle.high >= tp) return ExitReason.TARGET
            } else {
                if (candle.open <= tp || candle.low <= tp) return ExitReason.TARGET
            }
        }

        return null
    }

    private fun closeTrade(candle: Candle, trade: Trade, reason: ExitReason) {
        val rawExitPrice = when (reason) {
            ExitReason.SL -> {
                // If we opened below SL, we get the open price (realistic gap handling)
                if (trade.side == TradeSide.LONG && candle.open < currentStopLoss) candle.open
                else if (trade.side == TradeSide.SHORT && candle.open > currentStopLoss) candle.open
                else currentStopLoss
            }
            ExitReason.TARGET -> {
                // If we opened above target, we get the open price (positive gap)
                val tp = targetPrice ?: candle.close
                if (trade.side == TradeSide.LONG && candle.open > tp) candle.open
                else if (trade.side == TradeSide.SHORT && candle.open < tp) candle.open
                else tp
            }
            else -> candle.close
        }

        val exitPrice = if (trade.side == TradeSide.LONG) rawExitPrice * (1 - config.slippagePercentage / 100.0) else rawExitPrice * (1 + config.slippagePercentage / 100.0)

        trade.exitTime = candle.timestamp
        trade.exitPrice = exitPrice
        trade.exitReason = reason
        trade.status = "CLOSED"
        
        val points = if (trade.side == TradeSide.LONG) exitPrice - trade.entryPrice else trade.entryPrice - exitPrice
        val grossPnl = points * trade.quantity
        val turnover = (trade.entryPrice + exitPrice) * trade.quantity
        val comms = (2 * config.brokerageFlat) + (turnover * config.brokeragePercentage / 100.0)
        
        trade.pnl = grossPnl - comms
        trade.pnlPercentage = (trade.pnl / (trade.entryPrice * trade.quantity)) * 100.0
        
        currentCapital += trade.pnl
        trade.capitalAfter = currentCapital
        
        trades.add(trade)
        activeTrade = null
        targetPrice = null
    }

    private fun updateTradeStats(candle: Candle, trade: Trade) {
        if (candle.high > trade.maxPriceDuringTrade) trade.maxPriceDuringTrade = candle.high
        if (candle.low < trade.minPriceDuringTrade) trade.minPriceDuringTrade = candle.low

        if (config.trailingStopLossPercentage > 0.0) {
            if (trade.side == TradeSide.LONG) {
                val newSl = candle.close * (1 - config.trailingStopLossPercentage / 100.0)
                if (newSl > currentStopLoss) currentStopLoss = newSl
            } else {
                val newSl = candle.close * (1 + config.trailingStopLossPercentage / 100.0)
                if (newSl < currentStopLoss) currentStopLoss = newSl
            }
        }
    }

    private fun isOppositeSignal(side: TradeSide, type: SignalType): Boolean {
        return (side == TradeSide.LONG && type == SignalType.SHORT) || (side == TradeSide.SHORT && type == SignalType.LONG) || (type == SignalType.EXIT)
    }
}
