package com.frag2win.pocketmind.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val pdfFile by viewModel.pdfExportStatus.collectAsState()

    if (pdfFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPdfStatus() },
            title = { Text("PDF Generated") },
            text = { Text("PDF saved successfully to your Documents folder.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPdfStatus() }) {
                    Text("OK")
                }
            }
        )
    }

    ChatScreen(
        messages = messages,
        isGenerating = isGenerating,
        streamingMessage = streamingMessage,
        onSendMessage = { viewModel.sendMessage(it) },
        onExportPdf = { viewModel.exportToPdf(it) },
        modifier = modifier
    )
}

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingMessage: String?,
    onSendMessage: (String) -> Unit,
    onExportPdf: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingMessage) {
        val totalItems = messages.size + if (streamingMessage != null) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message, onExportPdf = { onExportPdf(message) })
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (streamingMessage != null) {
                item {
                    MessageBubble(
                        message = ChatMessage(role = "assistant", content = streamingMessage),
                        onExportPdf = {}
                    )
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
fun MessageBubble(
    message: ChatMessage,
    onExportPdf: () -> Unit
) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.content)
                if (!isUser && message.content.isNotEmpty()) {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.size(24.dp).align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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
            onSendMessage = {},
            onExportPdf = {}
        )
    }
}
