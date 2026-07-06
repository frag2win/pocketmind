package com.frag2win.pocketmind.data.repository

import com.frag2win.pocketmind.data.local.ModelPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

@Singleton
class SearchRepository @Inject constructor(
    private val modelPreferences: ModelPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun performWebSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val apiKey = modelPreferences.getTavilyApiKey()
        
        if (apiKey.isNullOrBlank()) {
            return@withContext performFreeSearch(query)
        }
        
        val jsonPayload = JSONObject().apply {
            put("api_key", apiKey)
            put("query", query)
            put("search_depth", "basic")
            put("max_results", 3)
        }

        val mediaType = "application/json".toMediaTypeOrNull()
        val body = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.tavily.com/search")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext performFreeSearch(query)
                
                val responseBody = response.body?.string() ?: return@withContext performFreeSearch(query)
                val jsonResponse = JSONObject(responseBody)
                val resultsArray = jsonResponse.getJSONArray("results")
                
                val searchResults = mutableListOf<SearchResult>()
                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    searchResults.add(
                        SearchResult(
                            title = obj.optString("title", ""),
                            snippet = obj.optString("content", ""),
                            url = obj.optString("url", "")
                        )
                    )
                }
                searchResults
            }
        } catch (e: Exception) {
            e.printStackTrace()
            performFreeSearch(query)
        }
    }

    private fun performFreeSearch(query: String): List<SearchResult> {
        return try {
            val url = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get()

            val results = doc.select(".result")
            val searchResults = mutableListOf<SearchResult>()
            
            // Limit to top 2 results for deep scraping to avoid long wait times
            val topResults = results.take(2)
            
            for (element in topResults) {
                val title = element.select(".result__title").text()
                var link = element.select(".result__a").attr("href")
                
                // Clean DuckDuckGo's proxy links if necessary
                if (link.startsWith("//duckduckgo.com/l/?kh=-1&uddg=")) {
                    link = java.net.URLDecoder.decode(link.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                } else if (link.startsWith("/l/?kh=-1&uddg=")) {
                    link = java.net.URLDecoder.decode(link.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                }

                // Deep Scrape: Fetch the actual page content
                val deepSnippet = try {
                    val pageDoc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(8000)
                        .followRedirects(true)
                        .get()
                    
                    // Remove noise
                    pageDoc.select("script, style, nav, footer, header, noscript").remove()

                    // Try to find the main article content specifically
                    val mainContent = pageDoc.select("article, .article, .post, .content, #main, main")
                    
                    val text = if (mainContent.isNotEmpty()) {
                        mainContent.select("p, div.article-body, div.story-details").joinToString("\n") { it.text() }
                    } else {
                        pageDoc.body().select("p, div.article-body, div.story-details").joinToString("\n") { it.text() }
                    }
                    
                    val cleanedText = text.replace(Regex("\\s+"), " ").trim()
                    
                    if (cleanedText.length < 200) {
                        // Fallback to all text if no paragraphs found
                        pageDoc.text().take(1500)
                    } else {
                        cleanedText.take(1500)
                    }
                } catch (e: Exception) {
                    element.select(".result__snippet").text() + " [Note: Full page scrape failed]"
                }
                
                if (title.isNotBlank() && deepSnippet.isNotBlank()) {
                    searchResults.add(SearchResult(title, deepSnippet, link))
                }
            }
            searchResults
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
