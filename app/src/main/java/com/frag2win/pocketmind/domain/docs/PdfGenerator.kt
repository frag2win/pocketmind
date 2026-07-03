package com.frag2win.pocketmind.domain.docs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateChatPdf(content: String, fileName: String? = null): Uri? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val actualFileName = fileName ?: "PocketMind_Export_$timestamp.pdf"
            
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, actualFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/PocketMind")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collection, contentValues) ?: return@withContext null

            resolver.openOutputStream(uri)?.use { outputStream ->
                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                // Header
                val header = Paragraph("PocketMind AI Export")
                    .setBold()
                    .setFontSize(24f)
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(header)
                
                val dateSubHeader = Paragraph("Generated on: ${SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic()
                document.add(dateSubHeader)
                
                document.add(Paragraph("\n")) // Spacer

                // Content
                document.add(Paragraph(content))

                // Footer
                val footer = Paragraph("\n\n---\nCreated by PocketMind - Fully On-Device AI")
                    .setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(footer)

                document.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
