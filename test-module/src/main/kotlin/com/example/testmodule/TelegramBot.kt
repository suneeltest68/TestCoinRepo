package com.example.testmodule

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class TelegramBot(private val token: String, private val chatId: String) {
    private val client = createUnsafeOkHttpClient()

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun sendMessage(text: String) {
        if (token.isBlank() || chatId.isBlank()) {
            println("[Telegram] Skipping notification: TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID is not set.")
            return
        }

        val url = "https://api.telegram.org/bot$token/sendMessage"
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("chat_id", chatId)
            ?.addQueryParameter("text", text)
            ?.build()

        if (url == null) {
            println("[Telegram] CRITICAL: Failed to build API URL. Check your token format.")
            return
        }

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("[Telegram] ERROR: Network failure when sending notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "No error body"
                        println("[Telegram] FAILED: API returned code ${response.code}. Details: $errorBody")
                        if (response.code == 401 || response.code == 404) {
                            println("[Telegram] HINT: Your Bot Token might be invalid.")
                        } else if (response.code == 400) {
                            println("[Telegram] HINT: Your Chat ID might be invalid.")
                        }
                    } else {
                        println("[Telegram] Message sent successfully.")
                    }
                }
            }
        })
    }
}
