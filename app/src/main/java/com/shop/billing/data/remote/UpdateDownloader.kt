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
        private const val URL_FILE_NAME = "download.url"
    }

    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state

    private var cancelled = false

    private fun getDir(): File = File(context.filesDir, DIR_NAME)
    private fun getFile(): File = File(getDir(), FILE_NAME)
    private fun getUrlFile(): File = File(getDir(), URL_FILE_NAME)

    suspend fun download(url: String): Result<Uri> = withContext(Dispatchers.IO) {
        cancelled = false
        try {
            val dir = getDir()
            dir.mkdirs()
            val apkFile = getFile()
            val urlFile = getUrlFile()

            // Reuse cached APK only if the download URL matches
            if (apkFile.exists() && urlFile.exists() && urlFile.readText().trim() == url) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                _state.value = DownloadState(isComplete = true, uri = uri, progress = 1f)
                return@withContext Result.success(uri)
            }

            // Stale or missing cache — clean and re-download
            apkFile.delete()
            urlFile.delete()
            _state.value = DownloadState(isDownloading = true)

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
            val output = apkFile.outputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgress = 0f

            while (input.read(buffer).also { bytesRead = it } != -1) {
                if (cancelled) {
                    input.close()
                    output.close()
                    apkFile.delete()
                    urlFile.delete()
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

            // Save download URL for cache validation
            urlFile.writeText(url)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
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
        val apkFile = getFile()
        if (apkFile.exists()) {
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        }
        return null
    }
}