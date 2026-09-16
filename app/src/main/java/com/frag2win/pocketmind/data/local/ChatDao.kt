package com.frag2win.pocketmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.frag2win.pocketmind.data.repository.SearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET content = :content, searchResults = :searchResults WHERE id = :id")
    suspend fun updateMessageContentAndSearch(id: Int, content: String, searchResults: List<SearchResult>?)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionDirect(sessionId: Int): List<ChatMessage>

    @Insert
    suspend fun createSession(session: ChatSession): Long

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Int, title: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("""
        SELECT chat_messages.* FROM chat_messages
        JOIN chat_messages_fts ON chat_messages.content = chat_messages_fts.content
        WHERE chat_messages_fts MATCH :query
    """)
    fun searchMessages(query: String): Flow<List<ChatMessage>>
}
