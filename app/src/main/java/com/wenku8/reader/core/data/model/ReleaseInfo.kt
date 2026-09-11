package com.wenku8.reader.core.data.model

/** One GitHub release relevant to updating the app. */
data class ReleaseInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSize: Long = -1L,
) {
    val hasApk: Boolean get() = apkUrl != null
}
