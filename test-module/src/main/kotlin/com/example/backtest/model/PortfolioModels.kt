package com.example.backtest.model

import com.example.backtest.strategy.Strategy
import java.util.TreeMap

data class PortfolioStrategyConfig(
    val strategy: Strategy,
    val weightPercentage: Double // e.g., 40.0 for 40%
)

data class PortfolioResult(
    val totalNetProfit: Double,
    val totalTrades: Int,
    val winPercentage: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    val recoveryTimeTrades: Int,
    val strategyCorrelations: Map<String, Map<String, Double>>,
    val monthlyReturns: TreeMap<String, Double>,
    val strategyContributions: Map<String, Double>
)
