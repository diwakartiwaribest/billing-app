package com.shop.billing.data.remote

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppVersion(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String
)

class UpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/diwakartiwaribest/billing-app/releases/latest"
    }

    suspend fun checkForUpdate(): AppVersion? = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentVersionName = getCurrentVersionName()
            Log.d(TAG, "Current version name: $currentVersionName")

            val latestRelease = fetchLatestRelease() ?: return@withContext null

            val tagName = latestRelease.optString("tag_name", "Unknown")
            val latestVersionName = tagName.removePrefix("v")
            val body = latestRelease.optString("body", "")

            // Get APK download URL from assets
            val assets = latestRelease.optJSONArray("assets") ?: return@withContext null
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    Log.d(TAG, "Found APK: $assetName")
                    break
                }
            }

            if (downloadUrl.isBlank()) {
                Log.w(TAG, "No APK found in latest release")
                return@withContext null
            }

            val hasUpdate = compareVersions(latestVersionName, currentVersionName) > 0
            Log.d(TAG, "Comparing: latest ($latestVersionName) > current ($currentVersionName) = $hasUpdate")

            if (hasUpdate) {
                Log.d(TAG, "Update available: $latestVersionName")
                AppVersion(
                    versionCode = 0L,
                    versionName = latestVersionName,
                    downloadUrl = downloadUrl,
                    changelog = body
                )
            } else {
                Log.d(TAG, "No update available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            null
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    private fun fetchLatestRelease(): JSONObject? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response)
            } else {
                Log.w(TAG, "GitHub API returned ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch latest release", e)
            null
        }
    }
}
