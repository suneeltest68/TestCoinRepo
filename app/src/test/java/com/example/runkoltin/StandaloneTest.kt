package com.example.runkoltin

import org.junit.Test
import org.junit.Assert.*

/**
 * You can run this directly in Android Studio by clicking the green arrow
 * in the gutter next to the class or method name.
 */
class StandaloneTest {

    @Test
    fun runMyScript() {
        println("--- Starting Standalone Script ---")
        
        val numbers = listOf(1, 2, 3, 4, 5)
        val sum = numbers.sum()
        
        println("Numbers: $numbers")
        println("Sum: $sum")
        
        assertEquals(15, sum)
        
        println("--- Script Finished Successfully ---")
    }
}
