package com.frag2win.pocketmind.ui.chat

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frag2win.pocketmind.data.local.ArtifactPayload
import com.frag2win.pocketmind.data.local.ArtifactType
import com.frag2win.pocketmind.ui.canvas.CanvasWebViewBridge
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactCard(
    artifact: ArtifactPayload,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPreviewModal by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Calculate item / slide count summary from JSON payload
    val slideCount = remember(artifact.data) {
        try {
            val json = JSONObject(artifact.data)
            if (json.has("slides")) {
                json.getJSONArray("slides").length()
            } else 1
        } catch (_: Exception) {
            1
        }
    }

    val cardBg = if (isDark) Color(0xFF1E1F20) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF2A2B2D) else Color(0xFFCBD5E1)
    val textColor = MaterialTheme.colorScheme.onBackground

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (artifact.type) {
                                ArtifactType.PPTX -> Icons.Default.Slideshow
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Surface(
                            color = Color(0xFF22D3EE).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when (artifact.type) {
                                    ArtifactType.PPTX -> "PRESENTATION"
                                    ArtifactType.MARKDOWN -> "MARKDOWN"
                                    ArtifactType.DOCX -> "DOCUMENT"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22D3EE),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = artifact.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Includes $slideCount slides generated by PocketMind AI Canvas.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action Button 1: Open / Preview
                Button(
                    onClick = { showPreviewModal = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22D3EE),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Preview",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.Bold)
                }

                // Action Button 2: Download / Export
                OutlinedButton(
                    onClick = { exportArtifactFile(context, artifact) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download")
                }
            }
        }
    }

    // Full-Screen Preview Bottom Sheet / Dialog Modal
    if (showPreviewModal) {
        ModalBottomSheet(
            onDismissRequest = { showPreviewModal = false },
            containerColor = Color(0xFF131314),
            modifier = Modifier.fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artifact.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showPreviewModal = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                CanvasWebViewBridge(
                    jsonPayload = artifact.data,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Downloads and exports the artifact file to the device Download/PocketMind folder.
 */
private fun exportArtifactFile(context: Context, artifact: ArtifactPayload) {
    try {
        val extension = when (artifact.type) {
            ArtifactType.PPTX -> "pptx"
            ArtifactType.MARKDOWN -> "md"
            ArtifactType.DOCX -> "docx"
        }
        val mimeType = when (artifact.type) {
            ArtifactType.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ArtifactType.MARKDOWN -> "text/markdown"
            ArtifactType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        }
        val cleanTitle = artifact.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "PocketMind_${cleanTitle}.${extension}"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PocketMind")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { os ->
                os.write(artifact.data.toByteArray())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Toast.makeText(context, "Exported to Download/PocketMind/$fileName", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to create export file URI", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
