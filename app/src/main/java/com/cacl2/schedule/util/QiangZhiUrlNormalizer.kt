package com.cacl2.schedule.util

import java.net.URI

object QiangZhiUrlNormalizer {

    fun normalizeOrNull(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        return try {
            val uri = URI(withScheme)
            val parsedScheme = uri.scheme?.lowercase() ?: return null
            if (parsedScheme != "http" && parsedScheme != "https") return null

            val host = uri.host?.trim().orEmpty()
            if (host.isBlank()) return null

            URI(
                parsedScheme,
                uri.userInfo,
                host,
                uri.port,
                uri.path,
                uri.query,
                uri.fragment
            ).toString()
        } catch (_: Exception) {
            null
        }
    }
}
