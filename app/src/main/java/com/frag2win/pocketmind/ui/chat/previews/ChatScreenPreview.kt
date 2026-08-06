package com.frag2win.pocketmind.ui.chat.previews

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.frag2win.pocketmind.ui.chat.ChatScreen
import com.frag2win.pocketmind.ui.chat.MessageBubble
import com.frag2win.pocketmind.data.local.ChatMessage

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            messages = listOf(
                ChatMessage(id = 1, role = "user", content = "Hello!"),
                ChatMessage(id = 2, role = "assistant", content = "Hi there! How can I help you today?")
            ),
            isGenerating = false,
            isModelLoading = false,
            streamingMessage = null,
            attachedFileName = null,
            onSendMessage = {},
            onAttachFile = {},
            onDetachFile = {},
            onOpenFile = {},
            onExportPdf = {},
            onClearChat = {},
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MessageBubble(
                message = ChatMessage(role = "user", content = "User message bubble"),
                onExportPdf = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            MessageBubble(
                message = ChatMessage(role = "assistant", content = "Assistant message bubble"),
                onExportPdf = {}
            )
        }
    }
}
