package com.example.backtest.core

import com.example.backtest.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.sqrt

class PortfolioBacktester(
    private val candles: List<Candle>,
    val initialCapital: Double,
    private val engineConfig: BacktestConfig = BacktestConfig()
) {
    private val indicatorEngine = IndicatorEngine()
    private val metricsEngine = MetricsEngine()

    fun run(strategyConfigs: List<PortfolioStrategyConfig>): PortfolioResult {
        if (candles.isEmpty()) throw IllegalArgumentException("Candles list is empty")

        println("Pre-calculating indicators for Portfolio...")
        val indicators = indicatorEngine.calculate(candles)

        val allTrades = mutableListOf<Trade>()
        val strategyDailyReturns = mutableMapOf<String, Map<LocalDate, Double>>()
        val strategyContributions = mutableMapOf<String, Double>()

        strategyConfigs.forEach { config ->
            val allocatedCapital = initialCapital * (config.weightPercentage / 100.0)
            
            println("Running strategy: ${config.strategy.name} (Allocated: ${String.format(Locale.US, "%.2f", allocatedCapital)})")
            
            val engine = BacktestEngine(candles, config.strategy, engineConfig, indicators)
            val trades = engine.run(initialCapital = allocatedCapital)
            
            allTrades.addAll(trades)
            strategyDailyReturns[config.strategy.name] = calculateDailyReturns(trades)
            strategyContributions[config.strategy.name] = trades.sumOf { it.pnl }
            
            val strategyMetrics = metricsEngine.calculate(trades)
            metricsEngine.printValidationReport(strategyMetrics)
        }

        val sortedTrades = allTrades.sortedBy { it.entryTime }
        val winningTrades = sortedTrades.count { it.pnl > 0 }
        val totalNetProfit = sortedTrades.sumOf { it.pnl }
        val winPercentage = if (sortedTrades.isNotEmpty()) (winningTrades.toDouble() / sortedTrades.size) * 100.0 else 0.0

        var currentEquity = 0.0
        var peak = 0.0
        var maxDd = 0.0
        var currentDdDuration = 0
        var maxDdDuration = 0

        sortedTrades.forEach { trade ->
            currentEquity += trade.pnl
            if (currentEquity > peak) {
                peak = currentEquity
                currentDdDuration = 0
            } else {
                val dd = peak - currentEquity
                if (dd > maxDd) maxDd = dd
                currentDdDuration++
                if (currentDdDuration > maxDdDuration) maxDdDuration = currentDdDuration
            }
        }

        val correlations = calculateCorrelations(strategyDailyReturns)

        val monthlyReturns = TreeMap<String, Double>()
        sortedTrades.forEach { trade ->
            val monthKey = trade.entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            monthlyReturns[monthKey] = (monthlyReturns[monthKey] ?: 0.0) + trade.pnl
        }

        val returns = sortedTrades.map { it.pnlPercentage }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdDev = if (returns.size > 1) sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average()) else 0.0
        val sharpe = if (stdDev != 0.0) (avgReturn / stdDev) * sqrt(252.0) else 0.0

        return PortfolioResult(
            totalNetProfit = totalNetProfit,
            totalTrades = sortedTrades.size,
            winPercentage = winPercentage,
            maxDrawdown = maxDd,
            sharpeRatio = sharpe,
            recoveryTimeTrades = maxDdDuration,
            strategyCorrelations = correlations,
            monthlyReturns = monthlyReturns,
            strategyContributions = strategyContributions
        )
    }

    private fun calculateDailyReturns(trades: List<Trade>): Map<LocalDate, Double> {
        return trades.groupBy { it.entryTime.toLocalDate() }
            .mapValues { it.value.sumOf { t -> t.pnl } }
    }

    private fun calculateCorrelations(dailyReturns: Map<String, Map<LocalDate, Double>>): Map<String, Map<String, Double>> {
        val names = dailyReturns.keys.toList()
        val allDates = dailyReturns.values.flatMap { it.keys }.distinct().sorted()
        val result = mutableMapOf<String, Map<String, Double>>()
        for (i in names.indices) {
            val innerMap = mutableMapOf<String, Double>()
            for (j in names.indices) {
                if (i == j) {
                    innerMap[names[j]] = 1.0
                } else {
                    val r1 = allDates.map { dailyReturns[names[i]]?.get(it) ?: 0.0 }
                    val r2 = allDates.map { dailyReturns[names[j]]?.get(it) ?: 0.0 }
                    innerMap[names[j]] = pearsonCorrelation(r1, r2)
                }
            }
            result[names[i]] = innerMap
        }
        return result
    }

    private fun pearsonCorrelation(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size != ys.size || xs.isEmpty()) return 0.0
        val avgX = xs.average(); val avgY = ys.average()
        var num = 0.0; var denX = 0.0; var denY = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - avgX; val dy = ys[i] - avgY
            num += dx * dy; denX += dx * dx; denY += dy * dy
        }
        val den = sqrt(denX * denY)
        return if (den != 0.0) num / den else 0.0
    }

    fun printSummary(result: PortfolioResult) {
        println("\n" + "=".repeat(100))
        println("PORTFOLIO BACKTEST SUMMARY")
        println("-".repeat(100))
        println(String.format(Locale.US, "%-30s : %.2f", "Total Net Profit", result.totalNetProfit))
        println(String.format(Locale.US, "%-30s : %d", "Total Trades", result.totalTrades))
        println(String.format(Locale.US, "%-30s : %.2f%%", "Combined Win %", result.winPercentage))
        println(String.format(Locale.US, "%-30s : %.2f", "Max Drawdown", result.maxDrawdown))
        println(String.format(Locale.US, "%-30s : %.2f", "Sharpe Ratio", result.sharpeRatio))
        println(String.format(Locale.US, "%-30s : %d trades", "Recovery Time (Max DD Duration)", result.recoveryTimeTrades))
        println("\nSTRATEGY CONTRIBUTIONS")
        result.strategyContributions.forEach { (name, profit) -> println(String.format(Locale.US, "%-30s : %.2f", name, profit)) }
        println("=".repeat(100))
    }
}
