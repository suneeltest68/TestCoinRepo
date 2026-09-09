package com.example.testmodule

import com.example.backtest.core.*
import com.example.backtest.model.*
import com.example.backtest.strategy.*
import java.util.Locale

fun main() {
    val loader = CsvCandleLoader()
    val fileName = "nifty_1min_5years.csv"
    val inputStream = object {}.javaClass.getResourceAsStream("/$fileName")
    
    if (inputStream == null) {
        println("Data file not found.")
        return
    }

    println("Loading historical data...")
    val allCandles = loader.load(inputStream)
    if (allCandles.isEmpty()) return

    val indicatorEngine = IndicatorEngine()
    println("Calculating Indicators for all strategies...")
    val indicators = indicatorEngine.calculate(
        allCandles,
        emaPeriods = listOf(20, 200),
        rsiPeriods = listOf(14),
        atrPeriods = listOf(7, 14),
        superTrendParams = listOf(7 to 3.0),
        bbParams = listOf(20 to 2.0)
    )
    val cprMap = indicatorEngine.calculateDailyCpr(allCandles)

    val strategies = listOf(
        OrbVwapStrategy(),
        SupertrendEmaStrategy(),
        BbRsiScalpStrategy(),
        CprStrategy(cprMap),
        DualTfStrategy()
    )

    val config = BacktestConfig(
        leverage = 25.0,
        lotSize = 65,
        slippagePercentage = 0.02,
        brokerageFlat = 20.0
    )

    val comparisonEngine = StrategyComparisonEngine(allCandles, config, indicators)
    println("Starting multi-strategy tournament backtest...")
    
    comparisonEngine.compare(strategies)
}
