package com.frag2win.pocketmind.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

@Singleton
class SearchRepository @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private data class CacheEntry(
        val results: List<SearchResult>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = object : LinkedHashMap<String, CacheEntry>(20, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > 20
        }
    }

    @Synchronized
    private fun getFromCache(key: String): List<SearchResult>? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > 10 * 60 * 1000L) { // 10 min TTL
            cache.remove(key)
            return null
        }
        return entry.results
    }

    @Synchronized
    private fun putInCache(key: String, results: List<SearchResult>) {
        if (results.isNotEmpty()) {
            cache[key] = CacheEntry(results)
        }
    }

    suspend fun performWebSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val normalizedKey = query.trim().lowercase()
        getFromCache(normalizedKey)?.let { cachedResults ->
            return@withContext cachedResults
        }

        // 1. Try SearXNG public JSON API instances
        val searxngResults = performSearXNGSearch(query)
        if (searxngResults.isNotEmpty()) {
            putInCache(normalizedKey, searxngResults)
            return@withContext searxngResults
        }

        // 2. Fallback to parallelized DuckDuckGo deep-scrape
        val freeResults = performFreeSearch(query)
        if (freeResults.isNotEmpty()) {
            putInCache(normalizedKey, freeResults)
        }
        return@withContext freeResults
    }

    private fun isQualitySnippet(title: String, snippet: String): Boolean {
        val cleanSnippet = snippet.trim()
        val cleanTitle = title.trim()

        if (cleanSnippet.length < 25) return false

        val lowerSnippet = cleanSnippet.lowercase()
        val lowerTitle = cleanTitle.lowercase()

        // Reject navigation/landing-page labels or search engine metadata
        val metadataPhrases = listOf(
            "google news", "explore top news stories", "top stories", "overview",
            "search engine", "sign in", "all rights reserved", "cookie policy"
        )
        if (metadataPhrases.any { lowerSnippet == it || lowerSnippet == "$it." || lowerTitle == it }) return false

        // Reject question-only snippets that end in '?' and have no factual sentences
        if (cleanSnippet.endsWith("?") && cleanSnippet.length < 65 && !cleanSnippet.contains(".")) {
            return false
        }

        return true
    }

    private fun performSearXNGSearch(query: String): List<SearchResult> {
        val instances = listOf(
            "https://searx.be/search?q=%s&format=json",
            "https://search.indiebits.io/search?q=%s&format=json",
            "https://searx.priv.art/search?q=%s&format=json"
        )

        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query
        }

        for (instancePattern in instances) {
            val targetUrl = String.format(instancePattern, encodedQuery)
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyString = response.body?.string() ?: return@use
                    val jsonObj = JSONObject(bodyString)
                    val resultsArray = jsonObj.optJSONArray("results") ?: return@use

                    val results = mutableListOf<SearchResult>()
                    for (i in 0 until minOf(resultsArray.length(), 5)) {
                        val item = resultsArray.getJSONObject(i)
                        val title = item.optString("title", "")
                        val snippet = item.optString("content", item.optString("snippet", ""))
                        val url = item.optString("url", "")
                        if (title.isNotBlank() && isQualitySnippet(title, snippet)) {
                            results.add(SearchResult(title, snippet, url))
                            if (results.size >= 3) break
                        }
                    }
                    if (results.isNotEmpty()) {
                        return results
                    }
                }
            } catch (_: Exception) {
                // Fallthrough to next instance on network failure/timeout
            }
        }
        return emptyList()
    }

    private suspend fun performFreeSearch(query: String): List<SearchResult> = coroutineScope {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(8000)
                .get()

            val results = doc.select(".result")
            val topResults = results.take(3)

            val deferredResults = topResults.map { element ->
                async(Dispatchers.IO) {
                    val title = element.select(".result__title").text()
                    var link = element.select(".result__a").attr("href")

                    if (link.startsWith("//duckduckgo.com/l/?kh=-1&uddg=")) {
                        link = URLDecoder.decode(link.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                    } else if (link.startsWith("/l/?kh=-1&uddg=")) {
                        link = URLDecoder.decode(link.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                    }

                    val fallbackSnippet = element.select(".result__snippet").text().trim()

                    val deepSnippet = try {
                        val pageDoc = Jsoup.connect(link)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(8000)
                            .followRedirects(true)
                            .get()

                        val readability = Readability4J(link, pageDoc.outerHtml())
                        val article = readability.parse()
                        val parsedText = article.textContent ?: article.excerpt ?: ""
                        val cleanedText = parsedText.replace(Regex("\\s+"), " ").trim()

                        if (cleanedText.length < 150) {
                            if (isQualitySnippet(title, fallbackSnippet)) fallbackSnippet else pageDoc.text().take(1200)
                        } else {
                            cleanedText.take(1500)
                        }
                    } catch (e: Exception) {
                        fallbackSnippet
                    }

                    if (title.isNotBlank() && deepSnippet.isNotBlank() && isQualitySnippet(title, deepSnippet)) {
                        SearchResult(title, deepSnippet, link)
                    } else null
                }
            }

            deferredResults.awaitAll().filterNotNull()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
