package com.example.testmodule

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.time.LocalDateTime
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.*
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun main() = runBlocking {
    // 1. Start a dummy Web Server to keep Render happy
    startDummyWebServer()

    // 2. Silence ALL internal logs
    System.setProperty("webdriver.chrome.silentOutput", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
    val rootLogger = Logger.getLogger("")
    rootLogger.handlers.forEach { it.level = Level.OFF }
    rootLogger.level = Level.OFF

    // 2. Load Secrets
    val telegramToken = System.getenv("TELEGRAM_BOT_TOKEN") ?: ""
    val telegramChatId = System.getenv("TELEGRAM_CHAT_ID") ?: ""
    val bot = TelegramBot(telegramToken, telegramChatId)

    val styleId = "23250698"
    val url = "https://www.myntra.com/gold-coin/bhima/bhima-floral-24k-999-purity-gold-bar-10-gram/$styleId/buy"

    println("🚀 Production Monitoring Bot (Coroutines) Initialized")
    bot.sendMessage("🤖 Bot Online: Monitoring Myntra Style $styleId every 60s.")

    // 3. Setup Driver Manager once
    try {
        WebDriverManager.chromedriver().setup()
    } catch (e: Exception) {
        println("Critical: Failed to setup WebDriver: ${e.message}")
        return@runBlocking
    }

    val options = ChromeOptions().apply {
        addArguments("--headless") 
        addArguments("--disable-gpu")
        addArguments("--no-sandbox")
        addArguments("--disable-dev-shm-usage")
        addArguments("--window-size=1920,1080")
        
        // --- Stealth / Anti-Detection ---
        addArguments("--disable-blink-features=AutomationControlled")
        setExperimentalOption("excludeSwitches", listOf("enable-automation"))
        setExperimentalOption("useAutomationExtension", false)
        addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        addArguments("--blink-settings=imagesEnabled=false")
    }

    // Monitoring loop using Coroutines
    while (isActive) {
        var driver: WebDriver? = null
        try {
            driver = ChromeDriver(options)
            println("[${LocalDateTime.now()}] Checking Myntra...")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(15))
            wait.until { d -> 
                d.findElements(By.className("pdp-offers-container")).isNotEmpty() || 
                d.findElements(By.className("pdp-title")).isNotEmpty()
            }

            val pageSource = driver.pageSource
            val targetCoupons = listOf("WELCOMEBACK", "MYNTRA300")
            
            var found = false
            for (code in targetCoupons) {
                if (pageSource?.contains(code, ignoreCase = true) == true) {
                    println(" ALERT: Found '$code")
                    bot.sendMessage("COUPON ALERT: Found '$code' on Myntra!\nURL: $url")
                    found = true
                }
            }
            
            if (!found) {
                println("Check complete: No target coupons found.")
            }

        } catch (e: Exception) {
            println("❌ Monitoring Error: ${e.message}")
        } finally {
            try {
                driver?.quit()
            } catch (e: Exception) { /* Ignore */ }
        }

        println("Waiting for 1 minute...")
        delay(60000) 
    }
}

/**
 * Tiny web server to prevent Render from sleeping
 */
fun startDummyWebServer() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/") { exchange ->
        val response = "Bot is running!"
        exchange.sendResponseHeaders(200, response.length.toLong())
        val os = exchange.responseBody
        os.write(response.toByteArray())
        os.close()
    }
    server.executor = null
    server.start()
    println("Dummy Web Server started on port $port")
}
