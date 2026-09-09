package com.example.backtest.core

import com.example.backtest.model.Trade
import org.knowm.xchart.*
import org.knowm.xchart.style.Styler
import java.awt.Color
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*

class ChartEngine {

    fun generateCharts(trades: List<Trade>, outputDir: String = "backtest_results") {
        if (trades.isEmpty()) return

        val dir = File(outputDir)
        if (!dir.exists()) dir.mkdirs()

        plotEquityCurve(trades, "$outputDir/equity_curve.png")
        plotDrawdownCurve(trades, "$outputDir/drawdown_curve.png")
        plotMonthlyProfit(trades, "$outputDir/monthly_profit.png")
        
        println("Charts generated in: ${dir.absolutePath}")
    }

    private fun plotEquityCurve(trades: List<Trade>, path: String) {
        val xData = mutableListOf<Int>()
        val yData = mutableListOf<Double>()
        
        var currentEquity = 0.0
        xData.add(0)
        yData.add(0.0)

        trades.forEachIndexed { index, trade ->
            currentEquity += trade.pnl
            xData.add(index + 1)
            yData.add(currentEquity)
        }

        val chart = XYChartBuilder()
            .width(1200).height(600)
            .title("Equity Curve")
            .xAxisTitle("Trade #")
            .yAxisTitle("Points")
            .build()

        chart.styler.legendPosition = Styler.LegendPosition.InsideNW
        chart.styler.setMarkerSize(0)
        chart.addSeries("Equity", xData.toDoubleArray(), yData.toDoubleArray())

        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG)
    }

    private fun plotDrawdownCurve(trades: List<Trade>, path: String) {
        val xData = mutableListOf<Int>()
        val yData = mutableListOf<Double>()
        
        var peak = 0.0
        var currentEquity = 0.0
        
        xData.add(0)
        yData.add(0.0)

        trades.forEachIndexed { index, trade ->
            currentEquity += trade.pnl
            if (currentEquity > peak) peak = currentEquity
            
            val dd = currentEquity - peak
            xData.add(index + 1)
            yData.add(dd)
        }

        val chart = XYChartBuilder()
            .width(1200).height(600)
            .title("Drawdown Curve")
            .xAxisTitle("Trade #")
            .yAxisTitle("Points")
            .build()

        chart.styler.seriesColors = arrayOf(Color.RED)
        chart.styler.setMarkerSize(0)
        chart.addSeries("Drawdown", xData.toDoubleArray(), yData.toDoubleArray())

        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG)
    }

    private fun plotMonthlyProfit(trades: List<Trade>, path: String) {
        val monthlyMap = TreeMap<String, Double>()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

        trades.forEach { trade ->
            val monthKey = trade.entryTime.format(formatter)
            monthlyMap[monthKey] = (monthlyMap[monthKey] ?: 0.0) + trade.pnl
        }

        val xData = monthlyMap.keys.toList()
        val yData = monthlyMap.values.toList()

        val chart = CategoryChartBuilder()
            .width(1200).height(600)
            .title("Monthly Profit/Loss")
            .xAxisTitle("Month")
            .yAxisTitle("Points")
            .build()

        chart.styler.legendPosition = Styler.LegendPosition.InsideNW
        chart.styler.xAxisLabelRotation = 45
        
        chart.addSeries("PnL", xData, yData)

        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG)
    }

    private fun List<Int>.toDoubleArray() = this.map { it.toDouble() }.toDoubleArray()
}
