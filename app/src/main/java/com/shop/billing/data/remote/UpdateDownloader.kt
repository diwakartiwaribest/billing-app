package com.shop.billing.data.remote

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val isComplete: Boolean = false,
    val error: String? = null,
    val uri: Uri? = null
)

class UpdateDownloader(private val context: Context) {
    companion object {
        private const val DIR_NAME = "updates"
        private const val FILE_NAME = "app-release.apk"
    }

    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state

    private var cancelled = false

    suspend fun download(url: String): Result<Uri> = withContext(Dispatchers.IO) {
        cancelled = false
        try {
            _state.value = DownloadState(isDownloading = true)

            val dir = File(context.cacheDir, DIR_NAME)
            dir.mkdirs()
            val file = File(dir, FILE_NAME)
            if (file.exists()) file.delete()

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            conn.instanceFollowRedirects = true
            conn.connect()

            if (conn.responseCode !in 200..399) {
                val error = "Download failed (HTTP ${conn.responseCode})"
                _state.value = DownloadState(error = error)
                return@withContext Result.failure(IOException(error))
            }

            val totalBytes = conn.contentLengthLong
            _state.value = _state.value.copy(totalBytes = totalBytes)

            val input = conn.inputStream
            val output = file.outputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgress = 0f

            while (input.read(buffer).also { bytesRead = it } != -1) {
                if (cancelled) {
                    input.close()
                    output.close()
                    file.delete()
                    _state.value = DownloadState()
                    return@withContext Result.failure(IOException("Download cancelled"))
                }
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytes > 0) {
                    val p = (totalBytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    if (p - lastProgress >= 0.01f || p >= 1f) {
                        _state.value = _state.value.copy(
                            bytesDownloaded = totalBytesRead,
                            progress = p
                        )
                        lastProgress = p
                    }
                }
            }

            input.close()
            output.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            _state.value = DownloadState(isComplete = true, uri = uri, progress = 1f)
            Result.success(uri)
        } catch (e: Exception) {
            if (e is IOException && e.message?.contains("canceled", true) == true) {
                _state.value = DownloadState()
            } else {
                _state.value = DownloadState(error = e.message ?: "Download failed")
            }
            Result.failure(e)
        }
    }

    fun cancel() {
        cancelled = true
        _state.value = DownloadState()
    }

    fun reset() {
        cancelled = true
        _state.value = DownloadState()
    }

    fun getDownloadedApkUri(): Uri? {
        val file = File(File(context.cacheDir, DIR_NAME), FILE_NAME)
        if (file.exists()) {
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        return null
    }
}