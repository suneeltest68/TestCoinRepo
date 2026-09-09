package com.example.backtest.model

import java.time.LocalDate

data class DailyLevels(
    val date: LocalDate,
    val pivot: Double,
    val tc: Double,
    val bc: Double,
    val r1: Double,
    val s1: Double
)
