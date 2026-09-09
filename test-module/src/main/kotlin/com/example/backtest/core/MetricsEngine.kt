package com.example.backtest.core

import com.example.backtest.model.BacktestMetrics
import com.example.backtest.model.Trade
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class MetricsEngine {

    fun calculate(trades: List<Trade>): BacktestMetrics {
        if (trades.isEmpty()) {
            return BacktestMetrics(0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0.0, 0, 0)
        }

        val totalTrades = trades.size
        val winningTrades = trades.filter { it.pnl > 0 }
        val losingTrades = trades.filter { it.pnl <= 0 }
        
        val netProfit = trades.sumOf { it.pnl }
        val winPercentage = (winningTrades.size.toDouble() / totalTrades) * 100.0
        
        val totalGain = winningTrades.sumOf { it.pnl }
        val totalLoss = abs(losingTrades.sumOf { it.pnl })
        
        val avgWinner = if (winningTrades.isNotEmpty()) totalGain / winningTrades.size else 0.0
        val avgLoser = if (losingTrades.isNotEmpty()) totalLoss / losingTrades.size else 0.0
        
        val largestWinner = winningTrades.maxOfOrNull { it.pnl } ?: 0.0
        val largestLoser = losingTrades.minOfOrNull { it.pnl } ?: 0.0
        
        // Holding Time
        val totalHoldingMinutes = trades.sumOf { t ->
            if (t.exitTime != null) ChronoUnit.MINUTES.between(t.entryTime, t.exitTime) else 0L
        }
        val avgHoldingTime = totalHoldingMinutes / totalTrades

        // Trades per Day
        val uniqueDays = trades.map { it.entryTime.toLocalDate() }.distinct().size
        val tradesPerDay = if (uniqueDays > 0) totalTrades.toDouble() / uniqueDays else 0.0

        val profitFactor = if (totalLoss != 0.0) totalGain / totalLoss else totalGain
        
        val winRate = winningTrades.size.toDouble() / totalTrades
        val lossRate = 1.0 - winRate
        val expectancy = (winRate * avgWinner) - (lossRate * avgLoser)

        // Drawdown & Recovery Tracking
        var peak = 0.0
        var currentCumulativePnl = 0.0
        var maxDd = 0.0
        var longestDdDuration = 0
        var currentDdDuration = 0
        
        // Consecutive tracking
        var maxConsecWins = 0
        var maxConsecLosses = 0
        var currentConsecWins = 0
        var currentConsecLosses = 0

        val returns = mutableListOf<Double>()
        val suspiciousTrades = mutableListOf<String>()

        trades.forEach { trade ->
            currentCumulativePnl += trade.pnl
            returns.add(trade.pnlPercentage)

            // Sanity Check: Trade exceeds 10% of capital
            if (trade.capitalBefore > 0) {
                val pnlImpact = abs(trade.pnl) / trade.capitalBefore
                if (pnlImpact > 0.10) {
                    suspiciousTrades.add("Trade ${trade.id} on ${trade.entryTime}: PnL impact ${String.format(Locale.US, "%.2f%%", pnlImpact * 100)} exceeds 10% limit.")
                }
            }

            // Drawdown calculation
            if (currentCumulativePnl > peak) {
                peak = currentCumulativePnl
                currentDdDuration = 0
            } else {
                val currentDd = peak - currentCumulativePnl
                if (currentDd > maxDd) maxDd = currentDd
                currentDdDuration++
                if (currentDdDuration > longestDdDuration) longestDdDuration = currentDdDuration
            }

            // Consecutive Wins/Losses
            if (trade.pnl > 0) {
                currentConsecWins++
                currentConsecLosses = 0
                if (currentConsecWins > maxConsecWins) maxConsecWins = currentConsecWins
            } else {
                currentConsecLosses++
                currentConsecWins = 0
                if (currentConsecLosses > maxConsecLosses) maxConsecLosses = currentConsecLosses
            }
        }

        val sharpe = calculateSharpe(returns)
        val mar = if (maxDd != 0.0) netProfit / maxDd else 0.0

        return BacktestMetrics(
            netProfit = netProfit,
            totalTrades = totalTrades,
            winPercentage = winPercentage,
            avgWinner = avgWinner,
            avgLoser = avgLoser,
            largestWinner = largestWinner,
            largestLoser = largestLoser,
            avgHoldingTimeMinutes = avgHoldingTime,
            tradesPerDay = tradesPerDay,
            profitFactor = profitFactor,
            expectancy = expectancy,
            maxDrawdown = maxDd,
            longestDrawdownDuration = longestDdDuration,
            recoveryTime = longestDdDuration, 
            sharpeRatio = sharpe,
            marRatio = mar,
            maxConsecutiveWins = maxConsecWins,
            maxConsecutiveLosses = maxConsecLosses,
            suspiciousTrades = suspiciousTrades
        )
    }

    private fun calculateSharpe(returns: List<Double>): Double {
        if (returns.size < 2) return 0.0
        val avgReturn = returns.average()
        val stdDev = sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())
        return if (stdDev != 0.0) (avgReturn / stdDev) * sqrt(252.0) else 0.0
    }

    fun printValidationReport(metrics: BacktestMetrics) {
        println("\n" + "=".repeat(60))
        println("VALIDATION & SANITY REPORT")
        println("-".repeat(60))
        println(String.format(Locale.US, "%-25s : %.2f", "Avg Profit/Trade", metrics.avgWinner))
        println(String.format(Locale.US, "%-25s : %.2f", "Avg Loss/Trade", metrics.avgLoser))
        println(String.format(Locale.US, "%-25s : %.2f", "Largest Winner", metrics.largestWinner))
        println(String.format(Locale.US, "%-25s : %.2f", "Largest Loser", metrics.largestLoser))
        println(String.format(Locale.US, "%-25s : %d min", "Avg Holding Time", metrics.avgHoldingTimeMinutes))
        println(String.format(Locale.US, "%-25s : %.2f", "Trades per Day", metrics.tradesPerDay))
        
        if (metrics.suspiciousTrades.isNotEmpty()) {
            println("\n[WARNING] SUSPICIOUS TRADES DETECTED (> 10% Capital Impact):")
            metrics.suspiciousTrades.forEach { println(" - $it") }
        } else {
            println("\n[SUCCESS] No single trade exceeded 10% capital impact.")
        }
        println("=".repeat(60) + "\n")
    }
}
