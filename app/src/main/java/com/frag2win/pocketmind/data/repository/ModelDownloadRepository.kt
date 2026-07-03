package com.frag2win.pocketmind.data.repository

import android.content.Context
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
        emit(DownloadState.Downloading(0))

        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, variant.name.lowercase() + ".litertlm")
        val tempFile = File(modelsDir, variant.name.lowercase() + ".tmp")

        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Server error: ${response.code}"))
                return@flow
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            
            if (totalBytes <= 0) {
                emit(DownloadState.Error("Unknown file size"))
                return@flow
            }

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read
                        val progress = ((bytesRead * 100) / totalBytes).toInt()
                        emit(DownloadState.Downloading(progress))
                    }
                }
            }

            // Move temp file to target
            if (tempFile.renameTo(targetFile)) {
                emit(DownloadState.Completed)
            } else {
                emit(DownloadState.Error("Failed to save model file"))
            }

        } catch (e: Exception) {
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
