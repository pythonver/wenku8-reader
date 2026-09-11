package com.wenku8.reader.core.data

import com.wenku8.reader.core.data.model.ReleaseInfo
import com.wenku8.reader.core.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the latest GitHub release. Anonymous API; while the repo is private
 * this returns 404 and the caller treats it as "no update" (and never prompts).
 */
@Singleton
class GitHubUpdateApi @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(UpdateConfig.LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            // GitHub rejects API requests without a User-Agent.
            .header("User-Agent", "wenku8-reader")
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                when {
                    resp.code == 404 -> {
                        AppLog.w(UpdateConfig.TAG, "release 接口 404（仓库私有或尚无 release）")
                        null
                    }
                    resp.code == 403 || resp.code == 429 -> {
                        val remaining = resp.header("X-RateLimit-Remaining")
                        AppLog.w(UpdateConfig.TAG, "release 接口被限流 HTTP ${resp.code}（剩余配额=$remaining）")
                        null
                    }
                    !resp.isSuccessful || body == null -> {
                        AppLog.w(UpdateConfig.TAG, "release 接口 HTTP ${resp.code}")
                        null
                    }
                    else -> parse(body)
                }
            }
        }.getOrElse { e ->
            AppLog.e(UpdateConfig.TAG, "检查更新网络失败", e)
            null
        }
    }

    private fun parse(json: String): ReleaseInfo? = runCatching {
        val obj = JSONObject(json)
        val tag = obj.optString("tag_name").ifBlank { return null }

        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize = -1L
        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            // Prefer the exact CI artifact name; fall back to any .apk asset.
            val preferred = "wenku8-reader-$tag.apk"
            var fallbackIndex = -1
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                val url = a.optString("browser_download_url")
                if (url.isBlank() || !name.endsWith(".apk", ignoreCase = true)) continue
                when {
                    name == preferred -> {
                        apkUrl = url; apkName = name; apkSize = a.optLong("size", -1L)
                    }
                    fallbackIndex < 0 -> fallbackIndex = i
                }
                if (apkUrl != null) break
            }
            if (apkUrl == null && fallbackIndex >= 0) {
                val a = assets.getJSONObject(fallbackIndex)
                apkUrl = a.getString("browser_download_url")
                apkName = a.optString("name")
                apkSize = a.optLong("size", -1L)
            }
        }

        ReleaseInfo(
            tag = tag,
            name = obj.optString("name").ifBlank { tag },
            notes = obj.optString("body").orEmpty(),
            htmlUrl = obj.optString("html_url").orEmpty(),
            publishedAt = obj.optString("published_at").orEmpty(),
            apkUrl = apkUrl,
            apkName = apkName,
            apkSize = apkSize,
        )
    }.getOrElse { e ->
        AppLog.e(UpdateConfig.TAG, "release JSON 解析失败", e)
        null
    }
}
