package com.example.backtest.core

import com.example.backtest.model.*
import com.example.backtest.strategy.Strategy
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.util.*

class OptimizationEngine(
    private val allCandles: List<Candle>,
    private val config: BacktestConfig = BacktestConfig()
) {
    private val metricsEngine = MetricsEngine()

    fun runOptimization(
        strategyFactories: List<(Map<String, Any>) -> Strategy>,
        paramSpaces: List<Map<String, List<Any>>>,
        periods: Map<String, ClosedRange<LocalDateTime>>
    ): List<OptimizationResult> = runBlocking {
        val allResults = mutableListOf<OptimizationResult>()

        periods.forEach { (periodName, range) ->
            println("\nProcessing Period: $periodName [$range]")
            val filteredCandles = allCandles.filter { it.timestamp in range }
            
            if (filteredCandles.isEmpty()) return@forEach

            val indicatorEngine = IndicatorEngine()
            val indicators = indicatorEngine.calculate(
                filteredCandles,
                emaPeriods = listOf(10, 20, 30, 50, 100, 200),
                rsiPeriods = listOf(14, 21),
                atrPeriods = listOf(10, 14),
                superTrendParams = listOf(10 to 2.0, 10 to 3.0, 10 to 4.0, 14 to 2.0, 14 to 3.0, 14 to 4.0)
            )

            val deferredResults = mutableListOf<Deferred<List<OptimizationResult>>>()

            strategyFactories.forEachIndexed { sIdx, factory ->
                val space = paramSpaces[sIdx]
                val combinations = generateCombinations(space)
                
                println("Strategy ${sIdx + 1}: Testing ${combinations.size} combinations...")

                val chunked = combinations.chunked(maxOf(1, combinations.size / Runtime.getRuntime().availableProcessors()))
                
                chunked.forEach { chunk ->
                    deferredResults.add(async(Dispatchers.Default) {
                        chunk.map { params ->
                            val strategy = factory(params)
                            val engine = BacktestEngine(filteredCandles, strategy, config, indicators)
                            val trades = engine.run()
                            val metrics = metricsEngine.calculate(trades)
                            
                            OptimizationResult(
                                strategyName = strategy.name,
                                parameters = params,
                                netProfit = metrics.netProfit,
                                totalTrades = metrics.totalTrades,
                                winPercentage = metrics.winPercentage,
                                profitFactor = metrics.profitFactor,
                                expectancy = metrics.expectancy,
                                maxDrawdown = metrics.maxDrawdown,
                                sharpeRatio = metrics.sharpeRatio,
                                marRatio = metrics.marRatio,
                                periodName = periodName
                            )
                        }
                    })
                }
            }

            allResults.addAll(deferredResults.awaitAll().flatten())
        }

        exportToCsv(allResults, "optimization_results.csv")
        printTop20(allResults)
        
        allResults
    }

    private fun generateCombinations(space: Map<String, List<Any>>): List<Map<String, Any>> {
        var results = listOf(emptyMap<String, Any>())
        space.forEach { (key, values) ->
            results = results.flatMap { map ->
                values.map { value -> map + (key to value) }
            }
        }
        return results
    }

    private fun exportToCsv(results: List<OptimizationResult>, fileName: String) {
        val file = File(fileName)
        file.printWriter().use { out ->
            out.println("Period,Strategy,Parameters,NetProfit,Trades,Win%,ProfitFactor,Expectancy,MaxDD,Sharpe,MAR")
            results.forEach { r ->
                val params = r.parameters.entries.joinToString(";") { "${it.key}=${it.value}" }
                out.println(String.format(Locale.US, "%s,%s,%s,%.2f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                    r.periodName, r.strategyName, params, r.netProfit, r.totalTrades,
                    r.winPercentage, r.profitFactor, r.expectancy, r.maxDrawdown, r.sharpeRatio, r.marRatio))
            }
        }
        println("Results saved to: ${file.absolutePath}")
    }

    private fun printTop20(results: List<OptimizationResult>) {
        val sorted = results.sortedWith(
            compareByDescending<OptimizationResult> { it.profitFactor }
                .thenBy { it.maxDrawdown }
                .thenByDescending { it.netProfit }
        ).take(20)

        println("\n" + "=".repeat(150))
        println("RANKING TABLE (Top 20 by PF -> MaxDD -> Net Profit)")
        println("-".repeat(150))
        println(String.format(Locale.US, "%-12s | %-25s | %-12s | %-8s | %-8s | %-10s | %-8s | %-6s", 
            "Period", "Strategy", "Net Profit", "PF", "Win %", "Max DD", "Sharpe", "Trades"))
        println("-".repeat(150))
        sorted.forEach { r ->
            println(String.format(Locale.US, "%-12s | %-25s | %12.2f | %8.2f | %7.2f%% | %10.2f | %8.2f | %6d",
                r.periodName, r.strategyName, r.netProfit, r.profitFactor, r.winPercentage, r.maxDrawdown, r.sharpeRatio, r.totalTrades))
        }
        println("=".repeat(150))
    }
}
