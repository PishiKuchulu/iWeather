package com.pishi.iweather.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-app updater, modelled on Anvex's release chain:
 *  - source of truth is the repo's GitHub "latest release";
 *  - the .sha256 sidecar asset is MANDATORY — no sidecar, no update;
 *  - the downloaded APK's SHA-256 must match the sidecar;
 *  - the APK must be signed with the SAME key as the installed app (checked before the
 *    installer is invoked, so a key change fails with a clear message, not a system error).
 */
object UpdateManager {

    data class UpdateInfo(val version: String, val apkUrl: String, val sha256Url: String)

    class SignerMismatchException : Exception("update APK signer differs from the installed app")

    private const val LATEST_URL = "https://api.github.com/repos/PishiKuchulu/iWeather/releases/latest"
    private const val USER_AGENT = "iWeather-Updater"

    /** The newer release than [currentVersion], or null when up to date. Throws on I/O failure. */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val root = JSONObject(getText(LATEST_URL))
        val version = root.optString("tag_name").removePrefix("v").trim()
        if (version.isBlank() || !isNewer(version, currentVersion)) return@withContext null
        var apkUrl: String? = null
        var shaUrl: String? = null
        val assets = root.optJSONArray("assets") ?: return@withContext null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            when (a.optString("name")) {
                "iWeather-$version.apk" -> apkUrl = a.optString("browser_download_url")
                "iWeather-$version.apk.sha256" -> shaUrl = a.optString("browser_download_url")
            }
        }
        // Both assets must exist; a release without its checksum sidecar is not offered.
        val apk = apkUrl ?: return@withContext null
        val sha = shaUrl ?: return@withContext null
        UpdateInfo(version, apk, sha)
    }

    /** Numeric dotted-version comparison ("1.10.0" > "1.9.0"); non-numeric parts count as 0. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * Downloads the APK into the updater cache folder and verifies it against the .sha256
     * sidecar. A mismatch deletes the file and throws — an unverified APK is never kept.
     */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(dir, "iWeather-${info.version}.apk")
            downloadTo(info.apkUrl, apkFile, onProgress)
            val expected = getText(info.sha256Url).trim()
                .split(Regex("\\s+")).firstOrNull()?.lowercase()
                ?: throw IOException("empty sha256 sidecar")
            if (sha256Hex(apkFile) != expected) {
                apkFile.delete()
                throw IOException("sha256 mismatch for iWeather-${info.version}.apk")
            }
            apkFile
        }

    /** True when [apkFile]'s signing certificate equals the installed app's. */
    @Suppress("DEPRECATION")
    fun verifySigner(context: Context, apkFile: File): Boolean {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                val newInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                    ?: return false
                val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val a = newInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return false
                val b = installed.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return false
                a.contentEquals(b)
            } else {
                val newInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                    ?: return false
                val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                val a = newInfo.signatures?.firstOrNull()?.toByteArray() ?: return false
                val b = installed.signatures?.firstOrNull()?.toByteArray() ?: return false
                a.contentEquals(b)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun installIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
        }

    private fun getText(url: String): String {
        val conn = open(url)
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadTo(url: String, dest: File, onProgress: (Int) -> Unit) {
        val conn = open(url)
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                }
            }
            onProgress(100)
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
