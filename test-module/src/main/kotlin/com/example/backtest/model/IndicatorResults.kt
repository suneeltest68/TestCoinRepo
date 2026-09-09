package com.example.backtest.model

/**
 * Caches precalculated indicators.
 * Optimized for dynamic lookup during strategy optimization.
 */
class IndicatorResults(val size: Int) {
    private val storage = mutableMapOf<String, DoubleArray>()

    fun add(key: String, values: DoubleArray) {
        storage[key] = values
    }

    fun get(key: String): DoubleArray? = storage[key]

    fun get(key: String, index: Int): Double = storage[key]?.get(index) ?: 0.0

    // Convenience helpers
    fun getEma(period: Int, index: Int) = get("EMA_$period", index)
    fun getRsi(period: Int, index: Int) = get("RSI_$period", index)
    fun getAtr(period: Int, index: Int) = get("ATR_$period", index)
    fun getVwap(index: Int) = get("VWAP", index)
    fun getSuperTrendDir(period: Int, mult: Double, index: Int) = get("ST_DIR_${period}_$mult", index)
    fun getSuperTrendVal(period: Int, mult: Double, index: Int) = get("ST_VAL_${period}_$mult", index)
    fun getBbUpper(period: Int, dev: Double, index: Int) = get("BB_UPPER_${period}_$dev", index)
    fun getBbLower(period: Int, dev: Double, index: Int) = get("BB_LOWER_${period}_$dev", index)
    fun getBbMiddle(period: Int, dev: Double, index: Int) = get("BB_MIDDLE_${period}_$dev", index)
}
