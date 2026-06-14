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
            val currentVersionCode = getCurrentVersionCode()
            val latestRelease = fetchLatestRelease() ?: return@withContext null
            
            val latestVersionCode = latestRelease.optString("tag_name", "v0").removePrefix("v").toLongOrNull() ?: 0L
            val latestVersionName = latestRelease.optString("tag_name", "Unknown")
            val changelog = latestRelease.optString("body", "")
            
            // Get APK download URL from assets
            val assets = latestRelease.optJSONArray("assets") ?: return@withContext null
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
            
            if (downloadUrl.isBlank()) {
                Log.w(TAG, "No APK found in latest release")
                return@withContext null
            }
            
            if (latestVersionCode > currentVersionCode) {
                AppVersion(
                    versionCode = latestVersionCode,
                    versionName = latestVersionName,
                    downloadUrl = downloadUrl,
                    changelog = changelog
                )
            } else {
                null // No update available
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            null
        }
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }
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
