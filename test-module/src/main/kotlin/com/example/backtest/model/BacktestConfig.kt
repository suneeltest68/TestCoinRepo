package com.example.backtest.model

import java.time.LocalTime

/**
 * Configuration for the backtesting engine.
 */
data class BacktestConfig(
    val stopLossPercentage: Double = 0.0,
    val targetPercentage: Double = 0.0,
    val trailingStopLossPercentage: Double = 0.0,
    val riskRewardRatio: Double = 0.0,
    
    val leverage: Double = 1.0,
    val lotSize: Int = 1,                     // e.g., 65 for Nifty
    val slippagePercentage: Double = 0.0,
    val brokerageFlat: Double = 20.0,
    val brokeragePercentage: Double = 0.0,
    
    val intradaySquareOffTime: LocalTime = LocalTime.of(15, 20),
    val startTradingTime: LocalTime = LocalTime.of(9, 15)
)
