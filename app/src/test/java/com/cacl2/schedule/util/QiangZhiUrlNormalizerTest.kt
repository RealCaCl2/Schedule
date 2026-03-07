package com.cacl2.schedule.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QiangZhiUrlNormalizerTest {

    @Test
    fun normalize_adds_https_when_missing_scheme() {
        val normalized = QiangZhiUrlNormalizer.normalizeOrNull("jwc.hyit.edu.cn/jsxsd")
        assertEquals("https://jwc.hyit.edu.cn/jsxsd", normalized)
    }

    @Test
    fun normalize_keeps_http_scheme() {
        val normalized = QiangZhiUrlNormalizer.normalizeOrNull("http://jwc.hyit.edu.cn/jsxsd")
        assertEquals("http://jwc.hyit.edu.cn/jsxsd", normalized)
    }

    @Test
    fun normalize_rejects_invalid_scheme() {
        val normalized = QiangZhiUrlNormalizer.normalizeOrNull("ftp://jwc.hyit.edu.cn/jsxsd")
        assertNull(normalized)
    }

    @Test
    fun normalize_rejects_empty_host() {
        val normalized = QiangZhiUrlNormalizer.normalizeOrNull("https:///jsxsd")
        assertNull(normalized)
    }
}
