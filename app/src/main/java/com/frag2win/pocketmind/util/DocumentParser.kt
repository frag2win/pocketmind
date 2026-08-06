package com.frag2win.pocketmind.util

import android.content.Context
import android.net.Uri
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentParser @Inject constructor() {

    companion object {
        private const val MAX_CHARS = 25000
        private const val TRUNCATION_MESSAGE = "\n\n[Text truncated due to context limits]"
    }

    suspend fun extractTextFromPdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // Request persistent read permissions
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, 
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Might fail if the URI wasn't launched with FLAG_GRANT_PERSISTABLE_URI_PERMISSION, 
            // but we try anyway for long-term access.
            e.printStackTrace()
        }

        val stringBuilder = StringBuilder()
        var inputStream: java.io.InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val reader = PdfReader(inputStream)
            val n = reader.numberOfPages
            
            for (i in 1..n) {
                val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                stringBuilder.append(pageText).append("\n")
                
                if (stringBuilder.length > MAX_CHARS) {
                    return@withContext stringBuilder.substring(0, MAX_CHARS) + TRUNCATION_MESSAGE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Propagate error to ViewModel
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val result = stringBuilder.toString().trim()
        if (result.length > MAX_CHARS) {
            result.substring(0, MAX_CHARS) + TRUNCATION_MESSAGE
        } else {
            result
        }
    }
}
