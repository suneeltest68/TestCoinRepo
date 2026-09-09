package com.example.backtest.model

data class OptimizationResult(
    val strategyName: String,
    val parameters: Map<String, Any>,
    val netProfit: Double,
    val totalTrades: Int,
    val winPercentage: Double,
    val profitFactor: Double,
    val expectancy: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    val marRatio: Double,
    val periodName: String // Training, Validation, Forward
)

data class ParameterSpace(
    val strategyName: String,
    val params: Map<String, List<Any>>
)
