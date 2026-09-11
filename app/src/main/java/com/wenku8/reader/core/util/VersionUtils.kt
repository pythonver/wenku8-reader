package com.wenku8.reader.core.util

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String?,
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        // A release (no suffix) is greater than the same version's pre-release.
        if (preRelease == null && other.preRelease == null) return 0
        if (preRelease == null) return 1
        if (other.preRelease == null) return -1
        return preRelease.compareTo(other.preRelease)
    }
}

/**
 * Lenient semantic-version parser for GitHub tags like "v1.2.1", "1.2",
 * "v1.3.0-beta.1". Missing numeric segments default to 0; returns null when no
 * leading numeric version can be extracted.
 */
fun parseSemVer(raw: String?): SemVer? {
    if (raw.isNullOrBlank()) return null
    var s = raw.trim().removePrefix("v").removePrefix("V").trim()
    if (s.isEmpty()) return null

    val preRelease: String?
    val dash = s.indexOf('-')
    if (dash >= 0) {
        preRelease = s.substring(dash + 1).takeIf { it.isNotBlank() }
        s = s.substring(0, dash)
    } else {
        preRelease = null
    }

    val parts = s.split('.')
    if (parts.isEmpty() || parts[0].toIntOrNull() == null) return null
    val nums = parts.map { it.toIntOrNull() ?: 0 }
    return SemVer(
        major = nums.getOrElse(0) { 0 },
        minor = nums.getOrElse(1) { 0 },
        patch = nums.getOrElse(2) { 0 },
        preRelease = preRelease,
    )
}

/** True when [latestTag] denotes a strictly newer release than the installed versionName. */
fun isNewerVersion(latestTag: String?, installedVersionName: String?): Boolean {
    val latest = parseSemVer(latestTag) ?: return false
    val installed = parseSemVer(installedVersionName) ?: return false
    return latest > installed
}
