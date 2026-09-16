package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.repository.SearchResult
import java.time.LocalDate

object PromptBuilder {

    fun buildRAGPrompt(query: String, searchResults: List<SearchResult>, displayName: String = "User"): String {
        val contextBuilder = StringBuilder()
        
        searchResults.forEachIndexed { index, result ->
            val cleanSnippet = result.snippet.replace("[Note: Full page scrape failed]", "").trim()
            contextBuilder.append("Source ${index + 1} [${result.title}]: $cleanSnippet (URL: ${result.url})\n\n")
        }

        val effectiveName = if (displayName.isBlank()) "User" else displayName
        val systemInstruction = """
            You are PocketMind, a knowledgeable AI assistant. You are talking to $effectiveName.
            Today's date is ${LocalDate.now()}.
            Below is the [WEB CONTEXT] containing REAL-TIME evidence retrieved from current web search results.
            
            STRICT INSTRUCTIONS FOR RESPONSE GENERATION:
            1. Answer $effectiveName's request directly using the facts, news headlines, and sources provided below.
            2. If the user asked for a specific number of items (e.g. 'top 5'), provide up to that number using ONLY the verified retrieved results. Do NOT fabricate missing items.
            3. Include the source publication name or URL for each item when available. Do NOT invent source URLs or claim sources not listed below.
            4. Do NOT repeat or echo source titles, search questions, or web metadata as your final answer.
            5. Do NOT discuss search engines, technical errors, or page scraping mechanics.
            6. Synthesize facts from all sources into a clear, cohesive, multi-sentence or bulleted summary.
        """.trimIndent()

        return """
            $systemInstruction
            
            [START OF WEB CONTEXT]
            ${contextBuilder.toString().trim()}
            [END OF WEB CONTEXT]
            
            User Prompt: $query
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
