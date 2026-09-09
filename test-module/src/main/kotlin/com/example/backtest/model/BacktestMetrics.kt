package com.example.backtest.model

data class BacktestMetrics(
    val netProfit: Double,
    val totalTrades: Int,
    val winPercentage: Double,
    val avgWinner: Double,
    val avgLoser: Double,
    val largestWinner: Double,
    val largestLoser: Double,
    val avgHoldingTimeMinutes: Long,
    val tradesPerDay: Double,
    val profitFactor: Double,
    val expectancy: Double,
    val maxDrawdown: Double,
    val longestDrawdownDuration: Int,
    val recoveryTime: Int,
    val sharpeRatio: Double,
    val marRatio: Double,
    val maxConsecutiveWins: Int,
    val maxConsecutiveLosses: Int,
    val suspiciousTrades: List<String> = emptyList()
)
