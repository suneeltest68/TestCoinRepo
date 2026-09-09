package com.example.backtest.core

import com.example.backtest.model.Candle
import com.example.backtest.model.DailyLevels
import com.example.backtest.model.IndicatorResults
import org.ta4j.core.BarSeries
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.Indicator
import org.ta4j.core.indicators.*
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator
import org.ta4j.core.num.Num
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class IndicatorEngine {

    private val zoneId = ZoneId.of("Asia/Kolkata")

    fun calculate(
        candles: List<Candle>,
        emaPeriods: List<Int> = listOf(20, 50, 200),
        rsiPeriods: List<Int> = listOf(14),
        atrPeriods: List<Int> = listOf(14, 7),
        superTrendParams: List<Pair<Int, Double>> = listOf(7 to 3.0, 10 to 3.0),
        bbParams: List<Pair<Int, Double>> = listOf(20 to 2.0)
    ): IndicatorResults {
        val series = buildSeries(candles)
        val results = IndicatorResults(candles.size)
        val closePrice = ClosePriceIndicator(series)

        emaPeriods.forEach { p -> results.add("EMA_$p", seriesToDoubleArray(EMAIndicator(closePrice, p), candles.size)) }
        rsiPeriods.forEach { p -> results.add("RSI_$p", seriesToDoubleArray(RSIIndicator(closePrice, p), candles.size)) }
        atrPeriods.forEach { p -> results.add("ATR_$p", seriesToDoubleArray(ATRIndicator(series, p), candles.size)) }

        bbParams.forEach { (p, d) ->
            val middle = BollingerBandsMiddleIndicator(SMAIndicator(closePrice, p))
            val sd = StandardDeviationIndicator(closePrice, p)
            val upper = BollingerBandsUpperIndicator(middle, sd, series.function().apply(d))
            val lower = BollingerBandsLowerIndicator(middle, sd, series.function().apply(d))
            results.add("BB_MIDDLE_${p}_$d", seriesToDoubleArray(middle, candles.size))
            results.add("BB_UPPER_${p}_$d", seriesToDoubleArray(upper, candles.size))
            results.add("BB_LOWER_${p}_$d", seriesToDoubleArray(lower, candles.size))
        }

        superTrendParams.forEach { (p, m) ->
            val (stVal, stDir) = calculateSuperTrend(series, p, m)
            results.add("ST_VAL_${p}_$m", stVal)
            results.add("ST_DIR_${p}_$m", stDir)
        }

        results.add("VWAP", calculateIntradayVwap(candles))
        calculateMacd5m(candles, results)

        return results
    }

    fun calculateDailyCpr(candles: List<Candle>): Map<LocalDate, DailyLevels> {
        val dailyGroups = candles.groupBy { it.timestamp.toLocalDate() }.toSortedMap()
        val cprMap = mutableMapOf<LocalDate, DailyLevels>()
        
        var prevHigh = 0.0; var prevLow = 0.0; var prevClose = 0.0
        
        dailyGroups.forEach { (date, dayCandles) ->
            if (prevHigh > 0) {
                val pivot = (prevHigh + prevLow + prevClose) / 3.0
                val bc = (prevHigh + prevLow) / 2.0
                val tc = (pivot - bc) + pivot
                val r1 = (2 * pivot) - prevLow
                val s1 = (2 * pivot) - prevHigh
                cprMap[date] = DailyLevels(date, pivot, maxOf(tc, bc), minOf(tc, bc), r1, s1)
            }
            prevHigh = dayCandles.maxOf { it.high }
            prevLow = dayCandles.minOf { it.low }
            prevClose = dayCandles.last().close
        }
        return cprMap
    }

    private fun calculateIntradayVwap(candles: List<Candle>): DoubleArray {
        val vwapArray = DoubleArray(candles.size)
        var cumulativePv = 0.0; var cumulativeVol = 0.0
        var currentDate = candles.firstOrNull()?.timestamp?.toLocalDate()
        for (i in candles.indices) {
            val c = candles[i]
            if (c.timestamp.toLocalDate() != currentDate) {
                cumulativePv = 0.0; cumulativeVol = 0.0
                currentDate = c.timestamp.toLocalDate()
            }
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            cumulativePv += typicalPrice * c.volume; cumulativeVol += c.volume
            vwapArray[i] = if (cumulativeVol != 0.0) cumulativePv / cumulativeVol else c.close
        }
        return vwapArray
    }

    private fun calculateMacd5m(candles: List<Candle>, results: IndicatorResults) {
        val resampled = mutableListOf<Candle>(); var current5m: Candle? = null
        for (c in candles) {
            val startOf5m = c.timestamp.withMinute((c.timestamp.minute / 5) * 5).withSecond(0).withNano(0)
            if (current5m == null || current5m.timestamp != startOf5m) {
                if (current5m != null) resampled.add(current5m)
                current5m = Candle(startOf5m, c.open, c.high, c.low, c.close, c.volume)
            } else {
                current5m = current5m.copy(high = maxOf(current5m.high, c.high), low = minOf(current5m.low, c.low), close = c.close, volume = current5m.volume + c.volume)
            }
        }
        if (current5m != null) resampled.add(current5m)
        val series5m = buildSeries(resampled); val close5m = ClosePriceIndicator(series5m)
        val macd = MACDIndicator(close5m, 12, 26); val signal = EMAIndicator(macd, 9)
        val macdArr = DoubleArray(candles.size); val signalArr = DoubleArray(candles.size)
        var rIdx = 0
        for (i in candles.indices) {
            val c = candles[i]
            while (rIdx < resampled.size - 1 && resampled[rIdx + 1].timestamp.isBefore(c.timestamp.plusSeconds(1))) { rIdx++ }
            macdArr[i] = macd.getValue(rIdx).doubleValue(); signalArr[i] = signal.getValue(rIdx).doubleValue()
        }
        results.add("MACD_5M", macdArr); results.add("MACD_SIGNAL_5M", signalArr)
    }

    private fun buildSeries(candles: List<Candle>): BarSeries {
        val series = BaseBarSeriesBuilder().withName("S").build()
        for (c in candles) {
            val zdt = ZonedDateTime.of(c.timestamp, zoneId)
            val bar = BaseBar(Duration.ofMinutes(1), zdt, c.open, c.high, c.low, c.close, c.volume.toDouble(), c.close * c.volume, 0L, series.function())
            series.addBar(bar)
        }
        return series
    }

    private fun seriesToDoubleArray(indicator: Indicator<Num>, size: Int): DoubleArray {
        val array = DoubleArray(size)
        for (i in 0 until size) { array[i] = indicator.getValue(i).doubleValue() }
        return array
    }

    private fun calculateSuperTrend(series: BarSeries, period: Int, multiplier: Double): Pair<DoubleArray, DoubleArray> {
        val size = series.barCount; val stValue = DoubleArray(size); val stDir = DoubleArray(size)
        val atr = ATRIndicator(series, period); val finalUpperBand = DoubleArray(size); val finalLowerBand = DoubleArray(size)
        for (i in 0 until size) {
            val bar = series.getBar(i)
            val hl2 = (bar.highPrice.doubleValue() + bar.lowPrice.doubleValue()) / 2.0; val atrVal = atr.getValue(i).doubleValue()
            val basicUpper = hl2 + (multiplier * atrVal); val basicLower = hl2 - (multiplier * atrVal)
            if (i == 0) { finalUpperBand[i] = basicUpper; finalLowerBand[i] = basicLower; stDir[i] = 1.0; stValue[i] = basicLower } else {
                val prevClose = series.getBar(i - 1).closePrice.doubleValue()
                finalUpperBand[i] = if (basicUpper < finalUpperBand[i-1] || prevClose > finalUpperBand[i-1]) basicUpper else finalUpperBand[i-1]
                finalLowerBand[i] = if (basicLower > finalLowerBand[i-1] || prevClose < finalLowerBand[i-1]) basicLower else finalLowerBand[i-1]
                if (stDir[i-1] == 1.0) {
                    if (bar.closePrice.doubleValue() < finalLowerBand[i]) { stDir[i] = -1.0; stValue[i] = finalUpperBand[i] } else { stDir[i] = 1.0; stValue[i] = finalLowerBand[i] }
                } else {
                    if (bar.closePrice.doubleValue() > finalUpperBand[i]) { stDir[i] = 1.0; stValue[i] = finalLowerBand[i] } else { stDir[i] = -1.0; stValue[i] = finalUpperBand[i] }
                }
            }
        }
        return Pair(stValue, stDir)
    }
}
