package com.frag2win.pocketmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.frag2win.pocketmind.data.repository.SearchResult

enum class ArtifactType { PPTX, MARKDOWN, DOCX }
enum class ArtifactStatus { GENERATING, READY, ERROR }

data class ArtifactPayload(
    val type: ArtifactType,
    val title: String,
    val data: String,
    val status: ArtifactStatus = ArtifactStatus.READY
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int = 0, // Links to ChatSession
    val role: String, // "user" or "assistant"
    val content: String,
    val displayContent: String? = null, // Content to show in UI (optional)
    val fileUri: String? = null, // URI of the attached file
    val artifactType: String? = null,
    val artifactTitle: String? = null,
    val artifactData: String? = null,
    val artifactStatus: String? = null,
    val searchResults: List<SearchResult>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

fun ChatMessage.getArtifactPayload(): ArtifactPayload? {
    if (artifactData.isNullOrBlank()) return null
    val type = try {
        ArtifactType.valueOf(artifactType ?: "PPTX")
    } catch (_: Exception) {
        ArtifactType.PPTX
    }
    val status = try {
        ArtifactStatus.valueOf(artifactStatus ?: "READY")
    } catch (_: Exception) {
        ArtifactStatus.READY
    }
    return ArtifactPayload(
        type = type,
        title = artifactTitle ?: "AI Canvas Artifact",
        data = artifactData,
        status = status
    )
}
