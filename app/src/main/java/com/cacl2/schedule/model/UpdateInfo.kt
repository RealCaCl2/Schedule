package com.cacl2.schedule.model

import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val forceUpdate: Boolean = false,
    val fileSize: Long = 0,
    val sha256: String = ""
) {
    companion object {
        fun fromJson(json: String): UpdateInfo {
            val obj = JSONObject(json)
            return UpdateInfo(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.getString("versionName"),
                downloadUrl = obj.getString("downloadUrl"),
                changelog = obj.optString("changelog", ""),
                forceUpdate = obj.optBoolean("forceUpdate", false),
                fileSize = obj.optLong("fileSize", 0),
                sha256 = obj.optString("sha256", "")
            )
        }
    }
}
