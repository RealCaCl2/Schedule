package com.cacl2.schedule.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.cacl2.schedule.MainActivity
import com.cacl2.schedule.R
import com.cacl2.schedule.model.UpdateInfo
import com.cacl2.schedule.network.UpdateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult()
    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
    data object Checking : UpdateResult()
}

class UpdateManager(private val context: Context) {

    companion object {
        // Gitee 仓库 raw 地址：永远指向最新版 update.json
        private const val DEFAULT_UPDATE_URL =
            "https://gitee.com/cacl2lhg/lschedule/raw/master/update.json"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK = "last_check_timestamp"
        private const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val NOTIFICATION_CHANNEL_ID = "update_download"
        private const val NOTIFICATION_ID_DOWNLOAD = 1001
    }

    // ── Check ──────────────────────────────────────────────────

    suspend fun checkForUpdates(): UpdateResult {
        return fetchAndCompare(DEFAULT_UPDATE_URL)
    }

    suspend fun autoCheck(): UpdateResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val lastVersion = prefs.getInt("last_version_code", 0)
        val currentVersion = getCurrentVersionCode()
        val now = System.currentTimeMillis()

        // 首次安装时忽略冷却，立即检查一次
        val isFirstLaunch = lastVersion == 0

        if (!isFirstLaunch && now - lastCheck < AUTO_CHECK_INTERVAL_MS) {
            return UpdateResult.UpToDate // skip, checked recently
        }

        val result = fetchAndCompare(DEFAULT_UPDATE_URL)

        // 记录检查时间和当前版本号
        prefs.edit()
            .putLong(KEY_LAST_CHECK, now)
            .putInt("last_version_code", currentVersion)
            .apply()

        return result
    }

    // ── Download ───────────────────────────────────────────────

    suspend fun downloadApk(
        url: String,
        fileName: String,
        expectedSha256: String = "",
        totalSizeHint: Long = 0,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                createNotificationChannel()

                val targetFile = File(context.cacheDir, fileName)

                val result = UpdateApi.downloadFile(url, totalSizeHint) { downloaded, total ->
                    updateDownloadNotification(downloaded, total, fileName)
                    val effectiveTotal = if (total > 0) total else totalSizeHint
                    if (effectiveTotal > 0) {
                        onProgress?.invoke(downloaded.toFloat() / effectiveTotal.toFloat())
                    }
                }

                result.fold(
                    onSuccess = { bytes ->
                        // SHA256 校验
                        if (expectedSha256.isNotBlank()) {
                            val actualSha256 = bytes.sha256()
                            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                                cancelDownloadNotification()
                                return@withContext Result.failure(
                                    Exception("APK 校验失败：文件可能已损坏或被篡改")
                                )
                            }
                        }

                        targetFile.outputStream().use { it.write(bytes) }
                        cancelDownloadNotification()
                    },
                    onFailure = { e ->
                        cancelDownloadNotification()
                        return@withContext Result.failure(e)
                    }
                )

                Result.success(targetFile)
            } catch (e: Exception) {
                cancelDownloadNotification()
                Result.failure(e)
            }
        }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(this)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // ── Install ────────────────────────────────────────────────

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(intent)
    }

    // ── Internal ───────────────────────────────────────────────

    private suspend fun fetchAndCompare(url: String): UpdateResult {
        val result = UpdateApi.fetchUpdateInfo(url)
        return result.fold(
            onSuccess = { remote ->
                val currentVersionCode = getCurrentVersionCode()
                if (remote.versionCode > currentVersionCode) {
                    UpdateResult.Available(remote)
                } else {
                    UpdateResult.UpToDate
                }
            },
            onFailure = { e ->
                UpdateResult.Error(e.message ?: "Unknown error")
            }
        )
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: PackageManager.NameNotFoundException) {
            0
        }
    }

    // ── Notification helpers ───────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.update_download_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateDownloadNotification(
        downloaded: Long,
        total: Long,
        fileName: String
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val sizeText = formatFileSize(downloaded) + if (total > 0) " / ${formatFileSize(total)}" else ""

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.update_downloading, fileName))
            .setContentText(sizeText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progressPercent, total <= 0)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID_DOWNLOAD, notification)
    }

    private fun cancelDownloadNotification() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID_DOWNLOAD)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
