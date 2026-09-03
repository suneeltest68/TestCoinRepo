package com.example.testmodule
import kotlin.math.abs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId



enum class Action { HOLD, ENTER_LONG, ENTER_SHORT, EXIT }

data class EMATrendConfig(
    val emaFastPeriod: Int = 4,
    val emaMidPeriod: Int = 11,
    val emaSlowPeriod: Int = 18,
    val atrPeriod: Int = 14,
    val adxPeriod: Int = 14,
    val slopeLookback: Int = 3,
    val adxThreshold: Double = 20.0,
    val distanceAtrMultiplier: Double = 0.5,
    val ema11SlopeAtrMultiplier: Double = 0.3,
    val ema18SlopeAtrMultiplier: Double = 0.2,
    val fullBodyMinRatio: Double = 0.5
) {
    init {
        require(emaFastPeriod > 0 && emaMidPeriod > 0 && emaSlowPeriod > 0 && atrPeriod > 0 && adxPeriod > 0 && slopeLookback > 0) {
            "All period values must be positive."
        }
        require(emaFastPeriod < emaMidPeriod && emaMidPeriod < emaSlowPeriod) {
            "Require emaFastPeriod < emaMidPeriod < emaSlowPeriod."
        }
        require(adxThreshold >= 0.0 && distanceAtrMultiplier >= 0.0 && ema11SlopeAtrMultiplier >= 0.0 && ema18SlopeAtrMultiplier >= 0.0) {
            "EMA thresholds and multipliers must be non-negative."
        }
        require(fullBodyMinRatio in 0.0..1.0) {
            "`fullBodyMinRatio` must be between 0.0 and 1.0."
        }
    }
}

data class EMATrendPositionContext(
    val direction: String,
    val entryUnderlying: Double,
    val stopUnderlying: Double = 0.0
)

data class EMATrendDecision(
    val action: Action = Action.HOLD,
    val entryUnderlying: Double = 0.0,
    val stopUnderlying: Double = 0.0,
    val exitReason: String = "",
    val signalTriggered: Boolean = false
)

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    var ema4: Double = 0.0,
    var ema11: Double = 0.0,
    var ema18: Double = 0.0,
    var atr: Double = 0.0,
    var adx: Double = 0.0,
    var candleBody: Double = 0.0,
    var candleRange: Double = 0.0,
    var candleBodyRatio: Double = 0.0,
    var fullBodyCandle: Boolean = false,
    var emaDistance: Double = 0.0,
    var ema11Slope: Double = 0.0,
    var ema18Slope: Double = 0.0,
    var ema11SlopeStrength: Double = 0.0,
    var ema18SlopeStrength: Double = 0.0,
    var ema11DeltaCurrent: Double = 0.0,
    var ema11DeltaPrevious: Double = 0.0,
    var longSetup: Boolean = false,
    var shortSetup: Boolean = false,
    var longExit: Boolean = false,
    var shortExit: Boolean = false
)

object IndicatorCalculator {
    fun buildEmaTrendWithIndicators(
        ohlc: List<Candle>,
        config: EMATrendConfig = EMATrendConfig()
    ): List<Candle> {
        if (ohlc.isEmpty()) return emptyList()

        val candles = ohlc.sortedBy { it.timestamp }.distinctBy { it.timestamp }
        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }

        val ema4Values = calculateEMA(closes, config.emaFastPeriod)
        val ema11Values = calculateEMA(closes, config.emaMidPeriod)
        val ema18Values = calculateEMA(closes, config.emaSlowPeriod)
        val atrValues = calculateATR(highs, lows, closes, config.atrPeriod)
        val adxValues = calculateADX(highs, lows, closes, config.adxPeriod)

        for (i in candles.indices) {
            val c = candles[i]
            c.ema4 = ema4Values[i]
            c.ema11 = ema11Values[i]
            c.ema18 = ema18Values[i]
            c.atr = atrValues[i]
            c.adx = adxValues[i]

            c.candleBody = abs(c.close - c.open)
            c.candleRange = abs(c.high - c.low)
            c.candleBodyRatio = if (c.candleRange > 0) c.candleBody / c.candleRange else 0.0
            c.fullBodyCandle = c.candleRange > 0.0 && c.candleBody >= config.fullBodyMinRatio * c.candleRange

            c.emaDistance = c.ema4 - c.ema18

            val lookback = config.slopeLookback
            c.ema11Slope = if (i >= lookback) c.ema11 - candles[i - lookback].ema11 else 0.0
            c.ema18Slope = if (i >= lookback) c.ema18 - candles[i - lookback].ema18 else 0.0

            c.ema11SlopeStrength = if (c.atr > 0) c.ema11Slope / c.atr else 0.0
            c.ema18SlopeStrength = if (c.atr > 0) c.ema18Slope / c.atr else 0.0

            c.ema11DeltaCurrent = if (i >= 1) c.ema11 - candles[i - 1].ema11 else 0.0
            c.ema11DeltaPrevious = if (i >= 2) candles[i - 1].ema11 - candles[i - 2].ema11 else 0.0

            c.longSetup = isLongSetup(c, config)
            c.shortSetup = isShortSetup(c, config)
            c.longExit = c.low < c.ema11
            c.shortExit = c.high > c.ema11
        }
        return candles
    }

    private fun isLongSetup(c: Candle, config: EMATrendConfig): Boolean {
        return c.close > c.ema4 &&
                c.close > c.ema11 &&
                c.close > c.ema18 &&
                c.ema4 > c.ema11 &&
                c.ema11 > c.ema18 &&
                c.emaDistance > config.distanceAtrMultiplier * c.atr &&
                c.ema18Slope > 0.0 &&
                c.ema11Slope > 0.0 &&
                c.ema18Slope >= config.ema18SlopeAtrMultiplier * c.atr &&
                c.ema11Slope >= config.ema11SlopeAtrMultiplier * c.atr &&
                c.ema11SlopeStrength > c.ema18SlopeStrength &&
                c.ema11DeltaCurrent > c.ema11DeltaPrevious &&
                c.adx > config.adxThreshold &&
                c.fullBodyCandle
    }

    private fun isShortSetup(c: Candle, config: EMATrendConfig): Boolean {
        return c.close < c.ema4 &&
                c.close < c.ema11 &&
                c.close < c.ema18 &&
                c.ema4 < c.ema11 &&
                c.ema11 < c.ema18 &&
                c.emaDistance < -config.distanceAtrMultiplier * c.atr &&
                c.ema18Slope < 0.0 &&
                c.ema11Slope < 0.0 &&
                c.ema18Slope <= -config.ema18SlopeAtrMultiplier * c.atr &&
                c.ema11Slope <= -config.ema11SlopeAtrMultiplier * c.atr &&
                c.ema11SlopeStrength < c.ema18SlopeStrength &&
                c.ema11DeltaCurrent < c.ema11DeltaPrevious &&
                c.adx > config.adxThreshold &&
                c.fullBodyCandle
    }

    private fun calculateEMA(values: List<Double>, period: Int): DoubleArray {
        val result = DoubleArray(values.size)
        if (values.size < period) return result
        val multiplier = 2.0 / (period + 1)
        var sum = 0.0
        for (i in 0 until period) sum += values[i]
        result[period - 1] = sum / period
        for (i in period until values.size) {
            result[i] = (values[i] - result[i - 1]) * multiplier + result[i - 1]
        }
        return result
    }

    private fun calculateATR(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): DoubleArray {
        val result = DoubleArray(highs.size)
        if (highs.size < period + 1) return result
        val tr = DoubleArray(highs.size)
        tr[0] = highs[0] - lows[0]
        for (i in 1 until highs.size) {
            val hl = highs[i] - lows[i]
            val hpc = abs(highs[i] - closes[i - 1])
            val lpc = abs(lows[i] - closes[i - 1])
            tr[i] = maxOf(hl, hpc, lpc)
        }
        var sum = 0.0
        for (i in 0 until period) sum += tr[i]
        result[period - 1] = sum / period
        for (i in period until highs.size) {
            result[i] = (result[i - 1] * (period - 1) + tr[i]) / period
        }
        return result
    }

    private fun calculateADX(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): DoubleArray {
        val result = DoubleArray(highs.size)
        if (highs.size < period * 2) return result
        val tr = DoubleArray(highs.size)
        val plusDM = DoubleArray(highs.size)
        val minusDM = DoubleArray(highs.size)

        for (i in 1 until highs.size) {
            val upMove = highs[i] - highs[i - 1]
            val downMove = lows[i - 1] - lows[i]
            plusDM[i] = if (upMove > downMove && upMove > 0) upMove else 0.0
            minusDM[i] = if (downMove > upMove && downMove > 0) downMove else 0.0

            val hl = highs[i] - lows[i]
            val hpc = abs(highs[i] - closes[i - 1])
            val lpc = abs(lows[i] - closes[i - 1])
            tr[i] = maxOf(hl, hpc, lpc)
        }

        var smoothedTR = tr.asSequence().take(period + 1).sum()
        var smoothedPlusDM = plusDM.asSequence().take(period + 1).sum()
        var smoothedMinusDM = minusDM.asSequence().take(period + 1).sum()

        val dx = DoubleArray(highs.size)

        for (i in period until highs.size) {
            if (i > period) {
                smoothedTR = smoothedTR - (smoothedTR / period) + tr[i]
                smoothedPlusDM = smoothedPlusDM - (smoothedPlusDM / period) + plusDM[i]
                smoothedMinusDM = smoothedMinusDM - (smoothedMinusDM / period) + minusDM[i]
            }
            val plusDI = 100 * (smoothedPlusDM / smoothedTR)
            val minusDI = 100 * (smoothedMinusDM / smoothedTR)
            val diSum = plusDI + minusDI
            dx[i] = if (diSum != 0.0) 100 * abs(plusDI - minusDI) / diSum else 0.0
        }

        var adxSum = 0.0
        for (i in period until period * 2) adxSum += dx[i]
        result[period * 2 - 1] = adxSum / period
        for (i in period * 2 until highs.size) {
            result[i] = (result[i - 1] * (period - 1) + dx[i]) / period
        }
        return result
    }
}

class EMATrendSignalEngine(val config: EMATrendConfig = EMATrendConfig()) {

    private fun minimumHistoryBars(): Int {
        return maxOf(
            config.emaFastPeriod,
            config.emaMidPeriod,
            config.emaSlowPeriod,
            config.atrPeriod,
            config.adxPeriod
        ) + config.slopeLookback + 2
    }

    private fun evaluateExit(position: EMATrendPositionContext, currentCandle: Candle): EMATrendDecision {
        val direction = position.direction.trim().uppercase()
        if (direction == "LONG" && currentCandle.low < currentCandle.ema11) {
            return EMATrendDecision(action = Action.EXIT, exitReason = "EMA11_EXIT")
        }
        if (direction == "SHORT" && currentCandle.high > currentCandle.ema11) {
            return EMATrendDecision(action = Action.EXIT, exitReason = "EMA11_EXIT")
        }
        return EMATrendDecision(action = Action.HOLD)
    }

    fun evaluateCandle(
        candlesWithIndicators: List<Candle>,
        position: EMATrendPositionContext? = null
    ): EMATrendDecision {
        if (candlesWithIndicators.size < minimumHistoryBars()) {
            return EMATrendDecision(action = Action.HOLD)
        }

        val current = candlesWithIndicators.last()

        if (position != null) {
            return evaluateExit(position, current)
        }

        if (current.longSetup) {
            return EMATrendDecision(
                action = Action.ENTER_LONG,
                entryUnderlying = current.close,
                stopUnderlying = current.ema11,
                signalTriggered = true
            )
        }

        if (current.shortSetup) {
            return EMATrendDecision(
                action = Action.ENTER_SHORT,
                entryUnderlying = current.close,
                stopUnderlying = current.ema11,
                signalTriggered = true
            )
        }

        return EMATrendDecision(action = Action.HOLD)
    }
}




data class Position(
    var direction: String,
    var entryPrice: Double,
    var size: Int
)

class NiftyEmaTrendStrategyRunner(
    private val startingCapital: Double = 600000.0,
    private val lotSize: Int = 65,
    private val lots: Int = 3,
    private val marginRequirement: Double = 0.15,
    private val autoAdjustMargin: Boolean = true,
    private val minMarginFloor: Double = 0.02,
    private val minBars: Int = 120,
    private val entryStartTime: LocalTime = LocalTime.of(9, 25),
    private val squareOffTime: LocalTime = LocalTime.of(15, 15),
    private val dailyMaxLossPct: Double = 0.03,
    private val brokerage: Double = 80.0
) {
    private val positionSize = lotSize * lots
    private var equity = startingCapital
    private var position: Position? = null

    private var tradeDirection = ""
    private var entryUnderlying = 0.0
    private var stopUnderlying = 0.0

    private var signalCount = 0
    private var entrySubmitCount = 0
    private var exitCount = 0
    private var squareOffCount = 0
    private var dailyLossHaltCount = 0
    private var marginSkipCount = 0

    private var currentDay: LocalDate? = null
    private var dayStartEquity: Double? = null
    private val dayLossLimit = startingCapital * dailyMaxLossPct
    private var dayTradingBlocked = false
    private var squareOffRequestedDate: LocalDate? = null

    private val engine = EMATrendSignalEngine()

    fun runBacktest(candles: List<Candle>) {
        val processedCandles = mutableListOf<Candle>()

        for (i in candles.indices) {
            val candle = candles[i]
            val dateTime = Instant.ofEpochMilli(candle.timestamp).atZone(ZoneId.systemDefault())
            val barDate = dateTime.toLocalDate()
            val barTime = dateTime.toLocalTime()

            if (currentDay != barDate) {
                currentDay = barDate
                dayStartEquity = equity
                dayTradingBlocked = false
                squareOffRequestedDate = null
            }

            val startEquity = dayStartEquity ?: equity
            val dayLoss = maxOf(0.0, startEquity - equity)

            if (!dayTradingBlocked && dayLoss >= dayLossLimit) {
                dayTradingBlocked = true
                dailyLossHaltCount++
                println("Daily loss cap hit on $barDate at $barTime | Day Loss: $dayLoss")
            }

            if (dayTradingBlocked) {
                if (position != null) {
                    closePosition(candle.close)
                    exitCount++
                }
                continue
            }

            if (barTime >= squareOffTime) {
                if (position != null && squareOffRequestedDate != barDate) {
                    closePosition(candle.close)
                    exitCount++
                    squareOffCount++
                    squareOffRequestedDate = barDate
                }
                continue
            }

            if (barTime < entryStartTime) continue

            processedCandles.add(candle)
            if (processedCandles.size < minBars) continue

            val enrichedCandles = IndicatorCalculator.buildEmaTrendWithIndicators(processedCandles)
            val currentEnriched = enrichedCandles.last()

            val positionCtx = position?.let {
                EMATrendPositionContext(tradeDirection, entryUnderlying, stopUnderlying)
            }

            val decision = engine.evaluateCandle(enrichedCandles, positionCtx)

            if (position != null) {
                if (decision.action == Action.EXIT) {
                    closePosition(currentEnriched.close)
                    exitCount++
                }
                continue
            }

            if (barTime !in entryStartTime..squareOffTime) continue

            if (decision.signalTriggered) signalCount++

            if (decision.action == Action.ENTER_LONG || decision.action == Action.ENTER_SHORT) {
                val entryPrice = decision.entryUnderlying
                val (canEnter, _, _, _) = canAffordEntry(entryPrice)
                if (canEnter) {
                    tradeDirection = if (decision.action == Action.ENTER_LONG) "LONG" else "SHORT"
                    position = Position(tradeDirection, entryPrice, positionSize)
                    entrySubmitCount++
                    entryUnderlying = entryPrice
                    stopUnderlying = decision.stopUnderlying
                } else {
                    marginSkipCount++
                }
            }
        }

        printSummary()
    }

    private fun closePosition(exitPrice: Double) {
        val pos = position ?: return
        val pnl = if (pos.direction == "LONG") {
            (exitPrice - pos.entryPrice) * pos.size
        } else {
            (pos.entryPrice - exitPrice) * pos.size
        }
        equity += (pnl - brokerage)
        position = null
        tradeDirection = ""
        entryUnderlying = 0.0
        stopUnderlying = 0.0
    }

    private fun canAffordEntry(entryPrice: Double): Quad<Boolean, Double, Double, Double> {
        val notional = entryPrice * positionSize
        if (notional <= 0) return Quad(false, marginRequirement, 0.0, equity)

        val effectiveMargin = if (autoAdjustMargin) {
            val affordableMargin = (equity * 0.98) / notional
            minOf(marginRequirement, maxOf(minMarginFloor, affordableMargin))
        } else {
            marginRequirement
        }

        val requiredMargin = notional * effectiveMargin
        return Quad(requiredMargin <= equity, effectiveMargin, requiredMargin, equity)
    }

    private fun printSummary() {
        println("=== Backtest Completed ===")
        println("Final Equity: $equity")
        println("Signals: $signalCount | Entries: $entrySubmitCount | Exits: $exitCount")
        println("Square Offs: $squareOffCount | Daily Loss Halts: $dailyLossHaltCount | Margin Skips: $marginSkipCount")
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun loadCsv(fileName: String): List<Candle> {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        ?: return emptyList()

    return inputStream.bufferedReader().useLines { lines ->
        lines.drop(1).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 5) null
            else {
                Candle(
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    open = parts[1].toDoubleOrNull() ?: 0.0,
                    high = parts[2].toDoubleOrNull() ?: 0.0,
                    low = parts[3].toDoubleOrNull() ?: 0.0,
                    close = parts[4].toDoubleOrNull() ?: 0.0
                )
            }
        }.toList()
    }
}

fun main() {
    val candles = loadCsv("a.csv")
    val runner = NiftyEmaTrendStrategyRunner()
    runner.runBacktest(candles)
}