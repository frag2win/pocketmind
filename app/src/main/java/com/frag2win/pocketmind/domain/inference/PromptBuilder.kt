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
}
