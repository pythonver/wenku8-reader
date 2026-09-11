package com.wenku8.reader.core.util

import android.content.Context

/** Reads the *installed* app version straight from PackageManager, so the UI can
 *  show exactly what is running — the only reliable check when handing out APKs. */
object AppVersion {
    fun installed(context: Context): String = runCatching {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pi.versionName} (code ${pi.longVersionCode})"
    }.getOrElse { "?" }

    /** Raw versionName ("1.2.1"), or null if unreadable. */
    fun installedVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()
}
