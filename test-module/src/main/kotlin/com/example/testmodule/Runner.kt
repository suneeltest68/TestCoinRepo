package com.example.testmodule

import com.example.backtest.core.CsvCandleLoader

fun main() {
    val loader = CsvCandleLoader()
    val fileName = "nifty_1min_5years.csv"
    
    // Load from resources
    val inputStream = object {}.javaClass.getResourceAsStream("/$fileName")
    
    if (inputStream != null) {
        println("Starting load from resources: $fileName...")
        val candles = loader.load(inputStream)
        
        if (candles.isNotEmpty()) {
            println("Total Candles: ${candles.size}")
            println("First Candle: ${candles.first()}")
            println("Last Candle: ${candles.last()}")
        }
    } else {
        println("Data file $fileName not found in resources.")
    }
}
