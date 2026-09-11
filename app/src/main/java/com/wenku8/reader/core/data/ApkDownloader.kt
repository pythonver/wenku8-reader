package com.wenku8.reader.core.data

import android.content.Context
import com.wenku8.reader.core.util.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams the release APK into the app-private cache and verifies the archive
 * matches our package before handing it to the installer.
 */
@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val updatesDir: File
        get() = (context.externalCacheDir ?: context.cacheDir).resolve("updates")

    fun apkFileFor(tag: String): File {
        val safe = tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return updatesDir.resolve("wenku8-reader-$safe.apk")
    }

    /**
     * @return the finished APK file, or null on failure. Progress reports
     * read/total bytes (total may be -1 when unknown).
     */
    suspend fun download(
        url: String,
        tag: String,
        expectedSize: Long,
        onProgress: (read: Long, total: Long) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val dir = updatesDir
        dir.mkdirs()
        // Remove downloads from previous releases to avoid cache buildup.
        dir.listFiles()?.filter { it.name.endsWith(".apk") || it.name.endsWith(".part") }
            ?.forEach { runCatching { it.delete() } }

        val target = apkFileFor(tag)
        if (target.exists() && (expectedSize <= 0 || target.length() == expectedSize) &&
            matchesOurPackage(target)
        ) {
            AppLog.i(UpdateConfig.TAG, "APK 已存在，直接复用：${target.absolutePath}")
            onProgress(target.length(), target.length())
            return@withContext target
        }

        val part = File(dir, target.name + ".part")
        runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    AppLog.w(UpdateConfig.TAG, "下载 APK HTTP ${resp.code}")
                    return@withContext null
                }
                val body = resp.body ?: return@withContext null
                val total = body.contentLength().takeIf { it > 0 } ?: expectedSize
                var read = 0L
                body.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n == -1) break
                            output.write(buffer, 0, n)
                            read += n
                            onProgress(read, total)
                        }
                        output.flush()
                    }
                }
            }

            if (!part.renameTo(target)) {
                runCatching { target.delete() }
                if (!part.renameTo(target)) {
                    AppLog.e(UpdateConfig.TAG, "APK 重命名失败", null)
                    return@withContext null
                }
            }

            if (!matchesOurPackage(target)) {
                AppLog.w(UpdateConfig.TAG, "下载文件包名校验失败，丢弃：${target.absolutePath}")
                target.delete()
                return@withContext null
            }
            AppLog.i(UpdateConfig.TAG, "APK 下载完成：${target.absolutePath}（${target.length()} bytes）")
            target
        }.getOrElse { e ->
            if (e is CancellationException) {
                runCatching { part.delete() }
                throw e
            }
            runCatching { part.delete() }
            AppLog.e(UpdateConfig.TAG, "下载 APK 失败", e)
            null
        }
    }

    /** PackageManager can parse the archive and it claims our applicationId. */
    private fun matchesOurPackage(file: File): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        info?.packageName == context.packageName
    }.getOrDefault(false)
}
