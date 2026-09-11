package com.wenku8.reader.core.data

import com.wenku8.reader.BuildConfig

/** Central configuration for the GitHub-release based in-app update check. */
object UpdateConfig {
    /** False builds make NO network requests for update checks. */
    val enabled: Boolean = BuildConfig.UPDATE_CHECK_ENABLED

    const val REPO = "pythonver/wenku8-reader"
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO/releases/latest"

    /** Auto-check at most once per interval; failures back off for [FAIL_BACKOFF_MS]. */
    const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    const val FAIL_BACKOFF_MS = 1L * 60 * 60 * 1000

    const val TAG = "Update"
}
