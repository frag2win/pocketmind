package com.frag2win.pocketmind.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frag2win.pocketmind.data.local.ChatMessage
import androidx.compose.foundation.text.selection.SelectionContainer
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ChatScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val pdfFile by viewModel.pdfExportStatus.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (pdfFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPdfStatus() },
            title = { Text("PDF Generated") },
            text = { Text("PDF saved successfully to your Documents folder.") },
            dismissButton = {
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, pdfFile)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
                    viewModel.clearPdfStatus()
                }) {
                    Text("Share")
                }
            },
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
        onClearChat = { viewModel.clearChat() },
        onNewChat = { viewModel.startNewChat() },
        onMenuClick = onMenuClick,
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
    onClearChat: () -> Unit,
    onNewChat: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingMessage) {
        val totalItems = messages.size + if (streamingMessage != null) 1 else 0
        if (totalItems > 0) {
            val isAtBottom = !listState.canScrollForward
            val userIsScrolling = listState.isScrollInProgress

            if (streamingMessage != null) {
                // If we are at the bottom and the user isn't actively fighting the scroll, keep it pinned
                if (isAtBottom && !userIsScrolling) {
                    listState.scrollToItem(totalItems - 1)
                }
            } else {
                // Smooth scroll for new complete messages (only if we are already near the bottom)
                if (isAtBottom) {
                    listState.animateScrollToItem(totalItems - 1)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF000B18) // Dark deep blue gradient at bottom
                    )
                )
            )
            .imePadding()
    ) {
        // Top Bar - Gemini Style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Text(
                    "PocketMind Pro",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && streamingMessage == null) {
                EmptyChatState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message, onExportPdf = { onExportPdf(message) })
                    }
                    if (streamingMessage != null) {
                        item(key = "streaming_key") {
                            MessageBubble(
                                message = ChatMessage(role = "assistant", content = streamingMessage),
                                onExportPdf = {}
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input - Gemini Style
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1E1F20)) // Gemini dark input background
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = Color.White
                    )
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating,
                    placeholder = { 
                        Text(
                            "Ask PocketMind", 
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    maxLines = 4
                )
                
                if (inputText.isBlank()) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onSendMessage(inputText)
                            inputText = ""
                        },
                        enabled = !isGenerating
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2B2D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Gemini Live",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // More accurate Gemini-style four-pointed star
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val width = size.width
                    val height = size.height
                    moveTo(width / 2, 0f)
                    quadraticBezierTo(width / 2, height / 2, width, height / 2)
                    quadraticBezierTo(width / 2, height / 2, width / 2, height)
                    quadraticBezierTo(width / 2, height / 2, 0f, height / 2)
                    quadraticBezierTo(width / 2, height / 2, width / 2, 0f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4285F4), // Blue
                            Color(0xFF9B72CB), // Purple
                            Color(0xFFD96570), // Red/Pink
                            Color(0xFFF4AF5F)  // Orange/Yellow
                        )
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "What's the vibe, shubham?",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onExportPdf: () -> Unit
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Small Gemini Star for Assistant
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        val width = size.width
                        val height = size.height
                        moveTo(width / 2, 0f)
                        quadraticBezierTo(width / 2, height / 2, width, height / 2)
                        quadraticBezierTo(width / 2, height / 2, width / 2, height)
                        quadraticBezierTo(width / 2, height / 2, 0f, height / 2)
                        quadraticBezierTo(width / 2, height / 2, width / 2, 0f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4285F4),
                                Color(0xFF9B72CB),
                                Color(0xFFD96570),
                                Color(0xFFF4AF5F)
                            )
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            } else {
                SelectionContainer {
                    Markdown(
                        content = message.content,
                        colors = pocketMindMarkdownColors(),
                        typography = pocketMindMarkdownTypography(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (message.content.isNotEmpty() && message.content != "Thinking...") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        IconButton(
                            onClick = { 
                                clipboardManager.setText(AnnotatedString(message.content))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Message",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onExportPdf,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        
        if (isUser) {
            // No icon for user in the new style, just text aligned right
        }
    }
}

@Composable
fun AIProfileIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun UserProfileIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            messages = listOf(
                ChatMessage(id = 1, role = "user", content = "Hello!"),
                ChatMessage(id = 2, role = "assistant", content = "Hi there! How can I help you today?")
            ),
            isGenerating = false,
            streamingMessage = null,
            onSendMessage = {},
            onExportPdf = {},
            onClearChat = {},
            onMenuClick = {}
        )
    }
}
