package com.serotonin.common.tourneys

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlin.concurrent.thread

object CachedFormatSuggestions {
    var suggestions: List<String> = emptyList()

    fun refresh() {
        thread(name = "refresh-format-suggestions") {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("http://localhost:3000/formats").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@thread
                val jsonArray = JSONArray(body)

                val allIds = mutableSetOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val id = jsonArray.getJSONObject(i).getString("id")
                    allIds.add(id.lowercase())
                }

                suggestions = allIds.toList().sorted()
                println("Loaded ${suggestions.size} format suggestions: ${suggestions.take(5)}...")
            } catch (e: Exception) {
                println("Failed to load format suggestions: ${e.message}")
            }
        }
    }
}
