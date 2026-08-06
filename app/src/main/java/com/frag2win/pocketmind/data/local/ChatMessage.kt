package com.frag2win.pocketmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int = 0, // Links to ChatSession
    val role: String, // "user" or "assistant"
    val content: String,
    val displayContent: String? = null, // Content to show in UI (optional)
    val fileUri: String? = null, // URI of the attached file
    val timestamp: Long = System.currentTimeMillis()
)
