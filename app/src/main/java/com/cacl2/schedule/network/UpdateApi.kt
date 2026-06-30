package com.cacl2.schedule.network

import com.cacl2.schedule.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateApi {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    suspend fun fetchUpdateInfo(url: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Server returned HTTP $responseCode")
                )
            }

            val json = BufferedReader(InputStreamReader(connection.inputStream))
                .useLines { lines -> lines.joinToString("\n") }

            connection.disconnect()

            val updateInfo = UpdateInfo.fromJson(json)
            if (updateInfo.downloadUrl.isBlank()) {
                return@withContext Result.failure(
                    Exception("Invalid response: missing download URL")
                )
            }
            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        url: String,
        totalSizeHint: Long = 0,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = 120_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive")

            // Follow redirects
            connection.instanceFollowRedirects = true

            val contentType = connection.contentType ?: ""
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Server returned HTTP $responseCode")
                )
            }

            // 优先用服务器返回的 Content-Length，否则用传入的 totalSizeHint
            val totalSize = connection.contentLengthLong.let {
                if (it > 0) it else totalSizeHint
            }
            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            val output = java.io.ByteArrayOutputStream()

            var downloaded = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (totalSize > 0) {
                    onProgress(downloaded, totalSize)
                }
            }

            inputStream.close()
            connection.disconnect()

            val bytes = output.toByteArray()
            // 验证：APK 文件以 ZIP magic bytes (PK) 开头
            if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4B.toByte()) {
                return@withContext Result.failure(
                    Exception("Downloaded file is not a valid APK (wrong file type, possibly an HTML error page)")
                )
            }

            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
