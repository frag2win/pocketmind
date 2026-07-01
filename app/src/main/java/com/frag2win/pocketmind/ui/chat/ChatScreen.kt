package com.frag2win.pocketmind.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frag2win.pocketmind.data.local.ChatMessage

@Composable
fun ChatScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()

    ChatScreen(
        messages = messages,
        isGenerating = isGenerating,
        streamingMessage = streamingMessage,
        onSendMessage = { viewModel.sendMessage(it) },
        modifier = modifier
    )
}

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingMessage: String?,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (streamingMessage != null) {
                item {
                    MessageBubble(message = ChatMessage(role = "assistant", content = streamingMessage))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                enabled = !isGenerating,
                placeholder = { Text("Type a message...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onSendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isGenerating
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            messages = listOf(
                ChatMessage(role = "user", content = "Hello!"),
                ChatMessage(role = "assistant", content = "Hi there! How can I help you today?")
            ),
            isGenerating = false,
            streamingMessage = null,
            onSendMessage = {}
        )
    }
}
