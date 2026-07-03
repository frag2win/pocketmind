package com.frag2win.pocketmind.data.repository

import android.content.Context
import android.util.Log
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.remote.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ModelDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("ModelDownloadClient") private val okHttpClient: OkHttpClient
) {
    /**
     * Downloads a model file and emits progress updates.
     */
    fun downloadModel(variant: GemmaVariant, url: String): Flow<DownloadState> = flow {
        Log.d("ModelDownload", "Starting download for ${variant.name} from $url")
        emit(DownloadState.Downloading(0))

        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, variant.name.lowercase() + ".litertlm")
        val tempFile = File(modelsDir, variant.name.lowercase() + ".tmp")

        try {
            val request = Request.Builder().url(url).build()
            Log.d("ModelDownload", "Executing request...")
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMsg = "Server error: ${response.code} ${response.message}"
                Log.e("ModelDownload", errorMsg)
                emit(DownloadState.Error(errorMsg))
                return@flow
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            Log.d("ModelDownload", "File size: $totalBytes bytes")
            
            if (totalBytes <= 0) {
                emit(DownloadState.Error("Unknown file size"))
                return@flow
            }

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var lastReportedProgress = -1
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read
                        val progress = ((bytesRead * 100) / totalBytes).toInt()
                        
                        if (progress != lastReportedProgress) {
                            Log.d("ModelDownload", "Progress: $progress%")
                            emit(DownloadState.Downloading(progress))
                            lastReportedProgress = progress
                        }
                    }
                }
            }

            Log.d("ModelDownload", "Download complete. Renaming file...")
            // Move temp file to target
            if (tempFile.renameTo(targetFile)) {
                emit(DownloadState.Completed)
            } else {
                emit(DownloadState.Error("Failed to save model file"))
            }

        } catch (e: Exception) {
            Log.e("ModelDownload", "Exception during download", e)
            emit(DownloadState.Error(e.localizedMessage ?: "Download failed"))
            if (tempFile.exists()) tempFile.delete()
        }
    }.flowOn(Dispatchers.IO)

    fun isModelDownloaded(variant: GemmaVariant): Boolean {
        val modelsDir = File(context.filesDir, "models")
        val file = File(modelsDir, variant.name.lowercase() + ".litertlm")
        return file.exists()
    }

    fun deleteModel(variant: GemmaVariant) {
        val modelsDir = File(context.filesDir, "models")
        val file = File(modelsDir, variant.name.lowercase() + ".litertlm")
        if (file.exists()) file.delete()
    }
}
