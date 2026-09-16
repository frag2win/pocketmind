package com.frag2win.pocketmind.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ChatDao
import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.data.inference.implementations.LiteRTInferenceEngine
import com.frag2win.pocketmind.data.repository.SearchResult
import com.frag2win.pocketmind.data.repository.SearchRepository
import com.frag2win.pocketmind.domain.docs.PdfGenerator
import com.frag2win.pocketmind.domain.inference.GemmaPromptFormatter
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.frag2win.pocketmind.domain.inference.PromptBuilder
import com.frag2win.pocketmind.data.local.ChatSession
import com.frag2win.pocketmind.util.DocumentParser
import com.frag2win.pocketmind.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val inferenceEngine: PocketMindInference,
    private val chatDao: ChatDao,
    private val pdfGenerator: PdfGenerator,
    private val modelPreferences: ModelPreferences,
    private val searchRepository: SearchRepository,
    private val documentParser: DocumentParser
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _userName = MutableStateFlow(modelPreferences.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun refreshUserName() {
        _userName.value = modelPreferences.getUserName()
    }

    val sessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else chatDao.getMessagesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    private val _pdfExportStatus = MutableStateFlow<Uri?>(null)
    val pdfExportStatus = _pdfExportStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _attachedFileUri = MutableStateFlow<Uri?>(null)
    val attachedFileUri = _attachedFileUri.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName = _attachedFileName.asStateFlow()

    private val _isCanvasMode = MutableStateFlow(false)
    val isCanvasMode: StateFlow<Boolean> = _isCanvasMode.asStateFlow()

    private var generationJob: Job? = null

    private suspend fun cancelInFlightGeneration() {
        val job = generationJob
        if (job != null && job.isActive) {
            job.cancelAndJoin()
            if (generationJob == job) {
                generationJob = null
                _isGenerating.value = false
                _streamingMessage.value = null
            }
        }
    }

    fun toggleCanvasMode(enabled: Boolean = !_isCanvasMode.value) {
        _isCanvasMode.value = enabled
    }

    fun setCanvasMode(enabled: Boolean) {
        _isCanvasMode.value = enabled
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val searchResults: StateFlow<List<ChatMessage>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else chatDao.searchMessages("*$query*")
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Automatically start a session if none exists
        viewModelScope.launch {
            chatDao.getAllSessions().collect { sessionList ->
                if (sessionList.isEmpty()) {
                    startNewChat()
                } else if (_currentSessionId.value == null) {
                    _currentSessionId.value = sessionList.first().id
                }
            }
        }
    }

    fun exportToPdf(message: ChatMessage) {
        viewModelScope.launch {
            val uri = pdfGenerator.generateChatPdf(message.content)
            _pdfExportStatus.value = uri
        }
    }

    fun clearPdfStatus() {
        _pdfExportStatus.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            cancelInFlightGeneration()
            _currentSessionId.value?.let { 
                chatDao.deleteMessagesForSession(it)
                inferenceEngine.resetSession()
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            cancelInFlightGeneration()
            // Check if current session is empty
            val currentMessages = messages.value
            if (currentMessages.isEmpty() && _currentSessionId.value != null) {
                // Already in an empty session, just stay here
                return@launch
            }

            inferenceEngine.resetSession()
            val newSessionId = chatDao.createSession(ChatSession(title = "New Chat"))
            _currentSessionId.value = newSessionId.toInt()
        }
    }

    fun selectSession(sessionId: Int) {
        if (_currentSessionId.value != sessionId) {
            viewModelScope.launch {
                cancelInFlightGeneration()
                inferenceEngine.resetSession()
                _currentSessionId.value = sessionId
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun attachFile(uri: Uri, name: String) {
        _attachedFileUri.value = uri
        _attachedFileName.value = name
    }

    fun detachFile() {
        _attachedFileUri.value = null
        _attachedFileName.value = null
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            cancelInFlightGeneration()
            inferenceEngine.resetSession()
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            
            // If we deleted the current session, switch to another one or create new
            if (_currentSessionId.value == sessionId) {
                val remainingSessions = sessions.value
                if (remainingSessions.isNotEmpty()) {
                    _currentSessionId.value = remainingSessions.first().id
                } else {
                    startNewChat()
                }
            }
        }
    }

    private suspend fun awaitMessageInFlow(insertedId: Long) {
        try {
            withTimeout(2000) {
                messages.first { list -> list.any { it.id.toLong() == insertedId } }
            }
        } catch (_: Exception) {
            // Fallback timeout protection in case Flow emission took > 2s
        }
    }

    private fun isSuspiciouslyShortResponse(userQuery: String, response: String): Boolean {
        val cleanResp = response.trim()
        val cleanQuery = userQuery.trim()

        if (cleanResp.isBlank() || cleanResp.startsWith("Error:") || cleanResp.contains("{\n") || cleanResp.contains("\"slides\"")) {
            return false
        }

        // Do NOT trigger on short conversational turns ending with terminal punctuation (. ! ?)
        val hasTerminalPunctuation = cleanResp.endsWith(".") || cleanResp.endsWith("!") || cleanResp.endsWith("?")
        if (hasTerminalPunctuation) {
            return false
        }

        // Trigger ONLY when response is extremely short (< 30 chars, no linebreaks, no terminal punctuation) on queries > 8 chars
        return cleanQuery.length > 8 && cleanResp.length in 5..30 && !cleanResp.contains("\n")
    }

    private fun stripScaffolding(text: String): String {
        var clean = text
        if (clean.startsWith("__PDF_ATTACHED_FILE__:")) {
            clean = clean.substringAfter(":").substringAfter(" ")
        }
        return clean
            .replace("SYSTEM: You are a document analysis assistant. Use the text below to answer.", "")
            .replace("EXTRACTED TEXT:", "")
            .replace("USER QUESTION:", "")
            .replace("🪄", "")
            .trim()
    }

    private suspend fun isSearchRequired(query: String): Boolean {
        val clean = stripScaffolding(query)
        if (clean.isBlank()) return false

        // 1. Fast-path keyword check
        val fastKeywords = listOf(
            "today", "latest", "current", "news", "weather",
            "2024", "2025", "2026", "now", "recent", "price", "stock",
            "market", "time", "date"
        )
        if (fastKeywords.any { clean.contains(it, ignoreCase = true) }) {
            return true
        }

        // 2. Single-token YES/NO model classifier prompt
        return try {
            val classifierPrompt = """
                Does the following user query require real-time web search or up-to-date information not available offline?
                Answer ONLY with YES or NO.

                Query: "$clean"
                Answer:
            """.trimIndent()

            val response = inferenceEngine.generate(classifierPrompt).trim().uppercase()
            response.startsWith("YES")
        } catch (_: Exception) {
            false
        }
    }

    fun sendMessage(content: String, context: android.content.Context? = null) {
        val isCanvas = _isCanvasMode.value
        if (isCanvas) {
            _isCanvasMode.value = false
            val canvasPrompt = PromptBuilder.buildCanvasPrompt(content)
            val uiDisplay = "🪄 $content"
            processMessage(uiDisplay, canvasPrompt, null, null, isCanvas = true)
            return
        }

        if (_attachedFileUri.value != null && context != null) {
            viewModelScope.launch {
                val fileUri = _attachedFileUri.value!!
                val fileName = _attachedFileName.value ?: "Document"
                _isGenerating.value = true
                _streamingMessage.value = "Extracting document text..."
                
                try {
                    // Strictly await extraction on IO thread
                    val extractedText = withContext(Dispatchers.IO) {
                        documentParser.extractTextFromPdf(context, fileUri)
                    }
                    
                    if (extractedText.isBlank()) {
                        _streamingMessage.value = "Failed to read PDF content."
                        _isGenerating.value = false
                        return@launch
                    }

                    val processedPrompt = PromptBuilder.buildPdfRAGPrompt(fileName, extractedText, content)
                    val internalMarker = "__PDF_ATTACHED_FILE__:$fileName"
                    val uiDisplay = "$internalMarker $content"
                    
                    detachFile() // Clear attachment after successful processing
                    _isGenerating.value = false // Reset before entering processMessage
                    processMessage(uiDisplay, processedPrompt, null, fileUri.toString())
                } catch (e: Exception) {
                    _streamingMessage.value = "Error parsing PDF: ${e.localizedMessage}"
                    _isGenerating.value = false
                }
            }
        } else {
            processMessage(content, content)
        }
    }

    fun sendGitHubMessage(uiDisplay: String, actualPrompt: String) {
        processMessage(uiDisplay, actualPrompt)
    }

    private fun processMessage(
        uiDisplay: String, 
        actualPrompt: String, 
        pdfContext: String? = null, 
        fileUri: String? = null,
        isCanvas: Boolean = false
    ) {
        if (uiDisplay.isBlank() || _isGenerating.value) return

        // Set generating flag immediately to close race condition window before launch
        _isGenerating.value = true
        val previousJob = generationJob

        generationJob = viewModelScope.launch {
            try {
                previousJob?.cancelAndJoin()

                val isFirstTurn = messages.value.isEmpty()
                val sessionId = _currentSessionId.value ?: run {
                    val id = chatDao.createSession(ChatSession(title = "New Chat"))
                    _currentSessionId.value = id.toInt()
                    id.toInt()
                }

                // 1. Fetch prior history BEFORE inserting current user turn into Room DB
                val priorHistory = chatDao.getMessagesForSessionDirect(sessionId)

                var attachedSearchResults: List<SearchResult>? = null
                var processedContent = actualPrompt
                
                // Handle PDF Context injection
                if (pdfContext != null) {
                    processedContent = """
                        SYSTEM: You are a document analysis assistant. Use the text below to answer.
                        
                        EXTRACTED TEXT:
                        $pdfContext
                        
                        USER QUESTION:
                        $actualPrompt
                    """.trimIndent()
                } else {
                    val isOnline = NetworkUtils.isOnline(appContext)
                    if (!isOnline) {
                        _streamingMessage.value = "Offline — using local knowledge"
                    } else {
                        val requiresSearch = isSearchRequired(actualPrompt)
                        if (requiresSearch) {
                            _streamingMessage.value = "Searching the web..."
                            try {
                                var searchQuery = stripScaffolding(actualPrompt)
                                if (priorHistory.isNotEmpty()) {
                                    val formattedTurns = priorHistory.takeLast(3).joinToString("\n") { msg ->
                                        val roleName = if (msg.role == "user") "User" else "Assistant"
                                        "$roleName: ${stripScaffolding(msg.content)}"
                                    }
                                    val rewritePrompt = """
                                        Based on the conversation history below and the user's latest message, condense and rewrite the user's request into a single standalone search query for Google.
                                        Output ONLY the single-line search query string with no explanation, formatting, or quotes.

                                        History:
                                        $formattedTurns

                                        Latest User Message: ${stripScaffolding(actualPrompt)}

                                        Standalone Search Query:
                                    """.trimIndent()
                                    try {
                                        val rewritten = inferenceEngine.generate(rewritePrompt).trim().replace("\"", "").replace("\n", " ")
                                        if (rewritten.isNotBlank()) {
                                            searchQuery = rewritten
                                        }
                                    } catch (_: Exception) {
                                        // Fallback to un-rewritten query
                                    }
                                }

                                val searchResults = searchRepository.performWebSearch(searchQuery)
                                if (searchResults.isNotEmpty()) {
                                    attachedSearchResults = searchResults
                                    processedContent = PromptBuilder.buildRAGPrompt(
                                        query = actualPrompt,
                                        searchResults = searchResults,
                                        displayName = modelPreferences.getDisplayName()
                                    )
                                    _streamingMessage.value = "Analyzing web results..."
                                } else {
                                    _streamingMessage.value = "No relevant web results found. Falling back to local knowledge..."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                _streamingMessage.value = "Web search failed. Falling back to local knowledge..."
                            }
                        }
                    }
                }

                // 2. Add user message to DB with the FULL processed content for the model
                // and the UI display string for the user.
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = sessionId, 
                        role = "user", 
                        content = processedContent,
                        displayContent = uiDisplay,
                        fileUri = fileUri,
                        searchResults = attachedSearchResults
                    )
                )
                
                // Update session title with temporary snippet if it was the first message
                if (isFirstTurn) {
                    val displayTitle = if (uiDisplay.startsWith("__PDF_ATTACHED_FILE__:")) {
                        uiDisplay.substringAfter(":").substringAfter(" ").take(30)
                    } else {
                        uiDisplay.take(30)
                    }
                    chatDao.updateSessionTitle(sessionId, "$displayTitle...")
                }
                
                try {
                    // Architectural Patch: Ensure model is initialized before first inference
                    if (!inferenceEngine.isReady()) {
                        _isModelLoading.value = true
                        try {
                            val selectedVariantName = modelPreferences.getSelectedVariant()
                            val variant = if (selectedVariantName != null) {
                                GemmaVariant.valueOf(selectedVariantName)
                            } else {
                                GemmaVariant.E2B // Default
                            }
                            
                            if (inferenceEngine is LiteRTInferenceEngine) {
                                inferenceEngine.initializeSafe(variant)
                            }
                        } finally {
                            _isModelLoading.value = false
                        }
                    }

                    // Inject temporary "Thinking..." state
                    _streamingMessage.value = "Thinking..."

                    var fullResponse = ""
                    var isFirstToken = true
                    val responseFlow = inferenceEngine.generateStream(
                        userMessage = processedContent,
                        history = priorHistory,
                        displayName = modelPreferences.getDisplayName()
                    )
                    responseFlow.collect { token ->
                        if (isFirstToken) {
                            _streamingMessage.value = "" // Clear "Thinking..."
                            isFirstToken = false
                        }
                        fullResponse += token
                        _streamingMessage.value = GemmaPromptFormatter.sanitizeOutput(fullResponse)
                    }
                    
                    var finalCleanResponse = GemmaPromptFormatter.sanitizeOutput(fullResponse)
                    
                    // Option 2 Deterministic Guardrail: If output is a truncated single-line title on an open-ended question, auto-continue once for full detail.
                    // Note: Uses priorHistory to pass the isNotEmpty gate and reuse the active C++ Conversation KV-cache.
                    if (!isCanvas && isSuspiciouslyShortResponse(processedContent, finalCleanResponse)) {
                        _streamingMessage.value = "$finalCleanResponse\n\nExpanding explanation..."
                        val continuationPrompt = "Provide a complete and detailed explanation with key points for: '$actualPrompt'."
                        var expandedText = "$finalCleanResponse\n\n"
                        val continuationFlow = inferenceEngine.generateStream(
                            userMessage = continuationPrompt,
                            history = priorHistory,
                            displayName = modelPreferences.getDisplayName()
                        )
                        continuationFlow.collect { token ->
                            expandedText += token
                            _streamingMessage.value = GemmaPromptFormatter.sanitizeOutput(expandedText)
                        }
                        finalCleanResponse = GemmaPromptFormatter.sanitizeOutput(expandedText)
                    }
                    
                    val insertedId = if (isCanvas || (finalCleanResponse.contains("\"slides\"") && finalCleanResponse.contains("\"bullets\""))) {
                        var jsonStr = finalCleanResponse.trim()
                        if (jsonStr.startsWith("```json")) jsonStr = jsonStr.substringAfter("```json")
                        else if (jsonStr.startsWith("```")) jsonStr = jsonStr.substringAfter("```")
                        if (jsonStr.endsWith("```")) jsonStr = jsonStr.substringBeforeLast("```")
                        jsonStr = jsonStr.trim()

                        val title = try {
                            JSONObject(jsonStr).optString("title", "AI Canvas Presentation")
                        } catch (_: Exception) {
                            "AI Canvas Presentation"
                        }

                        chatDao.insertMessage(
                            ChatMessage(
                                sessionId = sessionId,
                                role = "assistant",
                                content = "Here is your generated AI Canvas presentation:",
                                artifactType = "PPTX",
                                artifactTitle = title,
                                artifactData = jsonStr,
                                artifactStatus = "READY"
                            )
                        )
                    } else {
                        chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "assistant", content = finalCleanResponse))
                    }
                    
                    // Seamless atomic swap: Await until messages Flow contains insertedId BEFORE clearing streaming placeholder
                    awaitMessageInFlow(insertedId)
                    _streamingMessage.value = null

                    if (isFirstTurn) {
                        generateSessionTitle(sessionId, uiDisplay, finalCleanResponse)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _isModelLoading.value = false
                    val errId = chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "assistant", content = "Error: ${e.message}"))
                    awaitMessageInFlow(errId)
                    _streamingMessage.value = null
                } finally {
                    if (generationJob == coroutineContext[Job]) {
                        _isGenerating.value = false
                        _streamingMessage.value = null
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    private fun generateSessionTitle(sessionId: Int, userMsg: String, assistantMsg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanUserMsg = if (userMsg.startsWith("__PDF_ATTACHED_FILE__:")) {
                    userMsg.substringAfter(":").substringAfter(" ")
                } else userMsg
                val prompt = GemmaPromptFormatter.formatSummaryPrompt(cleanUserMsg, assistantMsg)
                val rawTitle = inferenceEngine.generate(prompt)
                val cleanTitle = rawTitle
                    .replace("\"", "")
                    .replace("'", "")
                    .replace("*", "")
                    .trim()
                    .take(50)
                
                if (cleanTitle.isNotBlank()) {
                    chatDao.updateSessionTitle(sessionId, cleanTitle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
