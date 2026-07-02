package com.frag2win.pocketmind.domain.docs

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateChatPdf(content: String, fileName: String? = null): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val actualFileName = fileName ?: "PocketMind_Export_$timestamp.pdf"
            
            // For now, save to internal files directory for easy access
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), actualFileName)
            
            val writer = PdfWriter(FileOutputStream(file))
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
            // TODO: Better Markdown parsing if needed, for now just raw text
            document.add(Paragraph(content))

            // Footer
            val footer = Paragraph("\n\n---\nCreated by PocketMind - Fully On-Device AI")
                .setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(footer)

            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
