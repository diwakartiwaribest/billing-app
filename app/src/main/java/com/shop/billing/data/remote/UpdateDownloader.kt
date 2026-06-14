package com.shop.billing.data.remote

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

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

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state

    private var currentCall: okhttp3.Call? = null

    suspend fun download(url: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            _state.value = DownloadState(isDownloading = true)

            val dir = File(context.cacheDir, DIR_NAME)
            dir.mkdirs()
            val file = File(dir, FILE_NAME)
            if (file.exists()) file.delete()

            val request = Request.Builder().url(url).build()
            currentCall = client.newCall(request)
            val response = currentCall!!.execute()

            if (!response.isSuccessful) {
                val error = "Download failed (HTTP ${response.code})"
                _state.value = DownloadState(error = error)
                return@withContext Result.failure(IOException(error))
            }

            val body = response.body ?: run {
                _state.value = DownloadState(error = "Download failed: empty response")
                return@withContext Result.failure(IOException("Empty response body"))
            }

            val totalBytes = body.contentLength()
            _state.value = _state.value.copy(totalBytes = totalBytes)

            val sink = file.sink().buffer()
            val source = body.source()
            val buffer = okio.Buffer()
            var bytesRead: Long
            var totalBytesRead = 0L
            var lastProgress = 0f

            while (source.read(buffer, 8192).also { bytesRead = it } != -1L) {
                sink.write(buffer, bytesRead)
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

            sink.close()
            source.close()

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
        } finally {
            currentCall = null
        }
    }

    fun cancel() {
        currentCall?.cancel()
        currentCall = null
        _state.value = DownloadState()
    }

    fun reset() {
        currentCall?.cancel()
        currentCall = null
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
