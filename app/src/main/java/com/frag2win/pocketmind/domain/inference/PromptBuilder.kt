package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.repository.SearchResult

object PromptBuilder {

    fun buildRAGPrompt(query: String, searchResults: List<SearchResult>): String {
        val contextBuilder = StringBuilder()
        
        searchResults.forEachIndexed { index, result ->
            contextBuilder.append("Source ${index + 1}: ${result.snippet} (URL: ${result.url})\n")
        }

        val systemInstruction = """
            You are PocketMind, a helpful and precise AI assistant. 
            Today's date is ${java.time.LocalDate.now()}.
            Below is the [WEB CONTEXT] containing REAL-TIME information from the internet. 
            Use this context to answer the user's request accurately.
            
            IMPORTANT:
            1. DO NOT say you don't have access to real-time data. You HAVE access via the context below.
            2. If the context contains news or stock prices, report them as current.
            3. Synthesize the facts from all sources into a cohesive answer.
            4. If a specific source is mentioned, you can refer to it.
        """.trimIndent()

        return """
            $systemInstruction
            
            [START OF WEB CONTEXT]
            ${contextBuilder.toString()}
            [END OF WEB CONTEXT]
            
            User Prompt: $query
        """.trimIndent()
    }
}
