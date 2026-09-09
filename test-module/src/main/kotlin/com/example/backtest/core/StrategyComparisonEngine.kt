package com.example.backtest.core

import com.example.backtest.model.*
import com.example.backtest.strategy.Strategy
import java.util.*

class StrategyComparisonEngine(
    private val candles: List<Candle>,
    private val config: BacktestConfig = BacktestConfig(),
    private val precalculatedIndicators: IndicatorResults? = null
) {
    private val metricsEngine = MetricsEngine()

    data class ComparisonResult(
        val strategyName: String,
        val metrics: BacktestMetrics
    )

    fun compare(strategies: List<Strategy>): List<ComparisonResult> {
        if (candles.isEmpty()) return emptyList()

        println("Starting Strategy Comparison for ${strategies.size} strategies...")
        
        val results = strategies.map { strategy ->
            val engine = BacktestEngine(candles, strategy, config, precalculatedIndicators)
            val trades = engine.run()
            val metrics = metricsEngine.calculate(trades)
            ComparisonResult(strategy.name, metrics)
        }

        printRankingTable(results)
        
        return results
    }

    private fun printRankingTable(results: List<ComparisonResult>) {
        val sortedResults = results.sortedByDescending { it.metrics.netProfit }

        println("\n" + "=".repeat(120))
        println(
            String.format(
                Locale.US,
                "%-30s | %12s | %10s | %10s | %12s | %10s | %10s",
                "Strategy Name", "Net Profit", "Win %", "Prof Fact", "Max DD", "Sharpe", "Expectancy"
            )
        )
        println("-".repeat(120))

        sortedResults.forEach { res ->
            val m = res.metrics
            println(
                String.format(
                    Locale.US,
                    "%-30s | %12.2f | %9.2f%% | %10.2f | %12.2f | %10.2f | %10.2f",
                    res.strategyName, m.netProfit, m.winPercentage, m.profitFactor, m.maxDrawdown, m.sharpeRatio, m.expectancy
                )
            )
        }
        println("=".repeat(120) + "\n")
    }
}
