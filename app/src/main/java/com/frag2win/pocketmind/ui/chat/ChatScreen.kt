package com.frag2win.pocketmind.ui.chat

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.ui.draw.blur
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
    val isModelLoading by viewModel.isModelLoading.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val attachedFileName by viewModel.attachedFileName.collectAsState()
    val pdfFile by viewModel.pdfExportStatus.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            var name = "Document.pdf"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            viewModel.attachFile(it, name)
        }
    }

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
        isModelLoading = isModelLoading,
        streamingMessage = streamingMessage,
        attachedFileName = attachedFileName,
        onSendMessage = { viewModel.sendMessage(it, context) },
        onAttachFile = { filePicker.launch("application/pdf") },
        onDetachFile = { viewModel.detachFile() },
        onOpenFile = { uriString ->
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = Uri.parse(uriString)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback for some PDF viewers or if no viewer is found
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(uriString), "application/pdf")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        },
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
    isModelLoading: Boolean,
    streamingMessage: String?,
    attachedFileName: String?,
    onSendMessage: (String) -> Unit,
    onAttachFile: () -> Unit,
    onDetachFile: () -> Unit,
    onOpenFile: (String) -> Unit,
    onExportPdf: (ChatMessage) -> Unit,
    onClearChat: () -> Unit,
    onNewChat: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 1. & 2. Performance Fix: Use reverseLayout to anchor growth to the bottom.
    // By using reverseLayout = true, new tokens (at index 0) grow upwards from the bottom.
    // This removes the need for LaunchedEffect-driven scrolling during token emission,
    // which was the primary cause of the violent vertical jitter and vibration.
    
    val isAtBottom by remember {
        derivedStateOf {
            // In reverseLayout, index 0 is the bottom-most item.
            listState.firstVisibleItemIndex == 0
        }
    }

    // Only auto-scroll to snap to bottom when a new full message arrives,
    // and ONLY if the user was already at the bottom (Respects User Gestures).
    LaunchedEffect(messages.size) {
        if (isAtBottom && messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
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
                    "PocketMind",
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && streamingMessage == null) {
                EmptyChatState()
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true, // Key: Anchors items to the bottom
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.Top // Stack items from the bottom up
                ) {
                    // In reverseLayout, index 0 is the bottom of the screen
                    if (streamingMessage != null) {
                        item(key = "streaming_key") {
                            MessageBubble(
                                message = ChatMessage(role = "assistant", content = streamingMessage),
                                isStreaming = true
                            )
                        }
                    }
                    items(messages.asReversed(), key = { it.id }) { message ->
                        MessageBubble(
                            message = message, 
                            onOpenFile = onOpenFile,
                            onExportPdf = { onExportPdf(message) }
                        )
                    }
                }
            }
        }

        // Bottom Input - Gemini Style
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.Transparent
        ) {
            Column {
                if (attachedFileName != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(attachedFileName, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(18.dp).clickable { onDetachFile() }
                            )
                        },
                        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2A2B2D),
                            labelColor = Color.White,
                            leadingIconContentColor = Color.Red
                        ),
                        border = null
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1E1F20)) // Gemini dark input background
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onAttachFile) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Attach PDF",
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
    
    if (isModelLoading) {
        ModelLoadingOverlay()
    }
}

@Composable
fun ModelLoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .blur(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PocketMindIcon(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Loading PocketMind...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Initializing AI Model in RAM",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun PocketMindIcon(modifier: Modifier = Modifier) {
    val pmPath = "M30,35 L55,35 C62,35 62,45 55,45 L30,45 L30,75 M45,45 L45,75 M45,45 L58,58 L71,45 L71,75"
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString(pmPath).toPath()
        
        // Scale path to fit canvas
        val scaleX = size.width / 108f
        val scaleY = size.height / 108f
        val matrix = androidx.compose.ui.graphics.Matrix()
        matrix.scale(scaleX, scaleY)
        path.transform(matrix)
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color.LightGray)
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * scaleX)
        )
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PocketMind Monogram Icon
        PocketMindIcon(modifier = Modifier.size(100.dp))
        
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
    onOpenFile: (String) -> Unit = {},
    onExportPdf: () -> Unit = {},
    isStreaming: Boolean = false
) {
    val isUser = message.role == "user"
    
    // Key Optimization: Only recompose the content part when streaming
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            PocketMindIcon(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                val pdfName = remember(message.displayContent, message.content) {
                    val raw = message.displayContent ?: message.content
                    if (raw.startsWith("__PDF_ATTACHED_FILE__:", ignoreCase = true)) {
                        raw.substringAfter("__PDF_ATTACHED_FILE__:").substringBefore(" ").trim()
                    } else null
                }

                val userText = remember(message.displayContent, message.content) {
                    val raw = message.displayContent ?: message.content
                    if (raw.startsWith("__PDF_ATTACHED_FILE__:", ignoreCase = true)) {
                        raw.substringAfter("__PDF_ATTACHED_FILE__:").substringAfter(" ").trim()
                    } else raw
                }

                if (pdfName != null) {
                    PdfAttachmentCard(
                        fileName = pdfName,
                        onClick = {
                            message.fileUri?.let { onOpenFile(it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Surface(
                    color = Color(0xFF2A2B2D), // Dark grey for user bubble
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(start = 48.dp)
                ) {
                    Text(
                        text = userText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                if (isStreaming || message.content == "Thinking...") {
                    // PERFORMANCE & UI FIX: Use Stream-Safe Markdown parsing.
                    // This prevents raw delimiters (**, ##) from flashing during generation.
                    StreamingMarkdownText(
                        content = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Use Full Markdown library only for completed messages
                    SelectionContainer {
                        Markdown(
                            content = message.content,
                            colors = pocketMindMarkdownColors(),
                            typography = pocketMindMarkdownTypography(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (message.content.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
        }
    }
}

@Composable
fun PdfAttachmentCard(fileName: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1E1F20),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3C4043)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
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
