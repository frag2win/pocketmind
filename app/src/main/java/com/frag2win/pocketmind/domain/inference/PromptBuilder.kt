package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.repository.SearchResult
import java.net.URI
import java.time.LocalDate

object PromptBuilder {

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            var host = uri.host ?: url
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            host
        } catch (_: Exception) {
            url
        }
    }

    fun buildRAGPrompt(query: String, searchResults: List<SearchResult>, displayName: String = "User"): String {
        val contextBuilder = StringBuilder()
        
        searchResults.forEachIndexed { index, result ->
            val cleanSnippet = result.snippet.replace("[Note: Full page scrape failed]", "").trim()
            val domain = extractDomain(result.url)
            contextBuilder.append("""
                SOURCE ${index + 1}
                Title: ${result.title}
                Source Domain: $domain
                URL: ${result.url}
                Content: $cleanSnippet
            """.trimIndent()).append("\n\n")
        }

        val effectiveName = if (displayName.isBlank()) "User" else displayName
        val systemInstruction = """
            You are PocketMind, a knowledgeable AI assistant. You are talking to $effectiveName.
            Today's date is ${LocalDate.now()}.
            Below is the [RETRIEVED WEB EVIDENCE] containing REAL-TIME facts retrieved from current web search results.
            
            STRICT INSTRUCTIONS FOR RESPONSE SYNTHESIS:
            1. Answer $effectiveName's request directly using ONLY the factual evidence provided in [RETRIEVED WEB EVIDENCE].
            2. If the user asked for a specific number of items (e.g. 'top 5'), list up to that number of DISTINCT retrieved stories. If fewer distinct reliable stories are available in the context (e.g. 3 available), list ONLY the available distinct stories — NEVER fabricate additional stories or repeat the same story to meet a requested count.
            3. For each story, format cleanly:
               1. **Headline Title** — Source Domain (URL)
                  Concise, factual description of the event.
            4. Do NOT use meta-phrases like "Based on the provided text", "The provided text mentions", "In summary", or "According to the sources".
            5. Do NOT discuss search engines, technical errors, or page scraping mechanics.
            6. Never fabricate headlines, facts, or source URLs not present in the retrieved evidence.
        """.trimIndent()

        return """
            $systemInstruction
            
            [START OF RETRIEVED WEB EVIDENCE]
            ${contextBuilder.toString().trim()}
            [END OF RETRIEVED WEB EVIDENCE]
            
            USER REQUEST: $query
        """.trimIndent()
    }

    fun buildPdfRAGPrompt(documentName: String, extractedPdfText: String, userPrompt: String): String {
        return """
            System: You are analyzing the attached document '$documentName'. 
            Answer the user's prompt strictly based on the text inside [DOCUMENT START] and [DOCUMENT END]. 
            Ignore unrelated prior conversation context.

            [DOCUMENT START]
            $extractedPdfText
            [DOCUMENT END]

            User Prompt: $userPrompt
        """.trimIndent()
    }

    fun buildCodeAnalysisPrompt(path: String, code: String): String {
        return """
            You are a Senior Software Engineer. Analyze the following source code from the file: $path.
            Provide a clear explanation of its purpose, logic, and suggest any potential bugs or optimizations.
            
            [SOURCE CODE START]
            $code
            [SOURCE CODE END]
            
            Analysis:
        """.trimIndent()
    }

    fun buildPRSummaryPrompt(owner: String, repo: String, pullNumber: Int, diff: String): String {
        return """
            You are a Senior Developer reviewing a Pull Request (#$pullNumber) for $owner/$repo.
            Summarize the key changes, identified risks, and provide a high-level review of the implementation quality based on the diff below.
            
            [DIFF START]
            $diff
            [DIFF END]
            
            PR Summary:
        """.trimIndent()
    }

    /**
     * System prompt instructing Gemma to output strict JSON schema for the AI Canvas bridge.
     */
    val CANVAS_SYSTEM_PROMPT = """
        System: You are an AI Presentation and Document Architect.
        Your sole task is to generate structured canvas data in valid JSON format.
        
        CRITICAL OUTPUT INSTRUCTIONS:
        1. Output ONLY a valid JSON object matching the requested schema.
        2. Do NOT output markdown code fences (like ```json or ```), preamble, or extra explanations.
        3. Ensure all quotes and special characters within strings are correctly escaped.
        
        REQUIRED JSON SCHEMA:
        {
          "title": "Document Title",
          "type": "presentation",
          "slides": [
            {
              "title": "Slide Title",
              "bullets": [
                "Key takeaway 1",
                "Key takeaway 2"
              ]
            }
          ]
        }
    """.trimIndent()

    /**
     * Builds a canvas prompt incorporating the strict JSON output schema requirement.
     */
    fun buildCanvasPrompt(userPrompt: String, canvasType: String = "presentation"): String {
        return """
            $CANVAS_SYSTEM_PROMPT
            
            [CANVAS TYPE]: $canvasType
            [USER REQUEST]: $userPrompt
            
            [JSON OUTPUT]:
        """.trimIndent()
    }
}
