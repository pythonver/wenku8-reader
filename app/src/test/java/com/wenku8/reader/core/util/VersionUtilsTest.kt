package com.wenku8.reader.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionUtilsTest {

    @Test
    fun parsesPlainAndVPrefixed() {
        assertEquals(SemVer(1, 2, 1, null), parseSemVer("1.2.1"))
        assertEquals(SemVer(1, 2, 1, null), parseSemVer("v1.2.1"))
        assertEquals(SemVer(1, 2, 1, null), parseSemVer("  V1.2.1 "))
    }

    @Test
    fun missingSegmentsDefaultToZero() {
        assertEquals(SemVer(1, 2, 0, null), parseSemVer("v1.2"))
        assertEquals(SemVer(1, 0, 0, null), parseSemVer("1"))
    }

    @Test
    fun comparesNumericallyNotLexically() {
        assertTrue(isNewerVersion("v1.2.10", "1.2.9"))
        assertFalse(isNewerVersion("v1.2.9", "1.2.10"))
    }

    @Test
    fun equalVersionsAreNotNewer() {
        assertFalse(isNewerVersion("v1.2.1", "1.2.1"))
        assertFalse(isNewerVersion("1.2", "1.2.0"))
    }

    @Test
    fun preReleaseIsLowerThanRelease() {
        assertTrue(isNewerVersion("v1.3.0", "1.3.0-beta"))
        assertFalse(isNewerVersion("v1.3.0-beta", "1.3.0"))
        assertEquals("rc1", parseSemVer("1.3.0-rc1")?.preRelease)
    }

    @Test
    fun olderIsNotNewer() {
        assertFalse(isNewerVersion("v1.1.9", "1.2.0"))
    }

    @Test
    fun invalidInputReturnsNullOrFalse() {
        assertNull(parseSemVer(null))
        assertNull(parseSemVer(""))
        assertNull(parseSemVer("abc"))
        assertFalse(isNewerVersion("abc", "1.0.0"))
        assertFalse(isNewerVersion("v1.0.0", null))
    }
}
