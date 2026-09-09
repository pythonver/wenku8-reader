package com.wenku8.reader.core.data

import android.util.Base64
import com.wenku8.reader.core.data.model.ChapterInfo
import com.wenku8.reader.core.data.model.NovelContentItem
import com.wenku8.reader.core.data.model.NovelMeta
import com.wenku8.reader.core.data.model.NovelSearchItem
import com.wenku8.reader.core.data.model.VolumeInfo
import com.wenku8.reader.core.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * wenku8 wire protocol (verified live 2026-09-08):
 *   POST {BASE_URL}
 *   body: request = Base64( UTF-8("action=…&do=…") )
 * Base URL is `http://app.wenku8.cn/android.php` (HTTP only; .com is dead, HTTPS empty).
 */
@Singleton
class WenkuApi @Inject constructor(
    private val client: OkHttpClient,
) {
    companion object {
        const val BASE_URL = "http://app.wenku8.cn/android.php"
        private const val IMAGE_ENTRY = "<!--image-->"
    }

    // ---------- low level ----------

    private fun post(protocol: String): ByteArray? {
        AppLog.d("Api", "POST $protocol")
        val encoded = Base64.encodeToString(protocol.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val body = FormBody.Builder().add("request", encoded).build()
        val request = Request.Builder().url(BASE_URL).post(body).build()
        return runCatching {
            client.newCall(request).execute().use { resp ->
                val bytes = if (resp.isSuccessful) resp.body?.bytes() else null
                AppLog.d("Api", "  -> HTTP ${resp.code}, ${bytes?.size ?: 0} bytes")
                bytes
            }
        }.getOrElse { e ->
            AppLog.e("Api", "请求失败: $protocol", e)
            null
        }
    }

    private suspend fun postText(protocol: String): String? = withContext(Dispatchers.IO) {
        post(protocol)?.toString(Charsets.UTF_8)
    }

    // ---------- endpoints ----------

    /** `action=search` — anonymous, returns inline info per hit. */
    suspend fun searchNovel(keyword: String): List<NovelSearchItem> = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(keyword, "UTF-8")
        val xml = postText("action=search&searchtype=articlename&searchkey=$enc&t=0")
        if (xml == null) {
            AppLog.e("Api", "search「$keyword」网络失败/空响应")
            return@withContext emptyList()
        }
        AppLog.d("Api", "search「$keyword」响应前 200 字: ${xml.take(200)}")
        val items = parseSearch(xml)
        AppLog.i("Api", "search「$keyword」→ 解析到 ${items.size} 条")
        items
    }

    /** `book&do=meta` — full metadata. */
    suspend fun fetchMeta(aid: Int): NovelMeta? = withContext(Dispatchers.IO) {
        val xml = postText("action=book&do=meta&aid=$aid&t=0") ?: return@withContext null
        parseMeta(xml)
    }

    /** `book&do=intro` — full introduction text. */
    suspend fun fetchIntro(aid: Int): String? = withContext(Dispatchers.IO) {
        postText("action=book&do=intro&aid=$aid&t=0")
    }

    /** `book&do=list` — volume/chapter tree. */
    suspend fun fetchIndex(aid: Int): List<VolumeInfo> = withContext(Dispatchers.IO) {
        val xml = postText("action=book&do=list&aid=$aid&t=0") ?: return@withContext emptyList()
        parseIndex(xml)
    }

    /** `book&do=text` — chapter body: paragraphs + `<!--image-->` markers. */
    suspend fun fetchChapter(aid: Int, cid: Int): List<NovelContentItem> = withContext(Dispatchers.IO) {
        val xml = postText("action=book&do=text&aid=$aid&cid=$cid&t=0") ?: return@withContext emptyList()
        parseChapter(xml)
    }

    /** `book&do=cover` — cover image as JPEG bytes (binary response). */
    suspend fun fetchCover(aid: Int): ByteArray? = withContext(Dispatchers.IO) {
        post("action=book&do=cover&aid=$aid&t=0")
    }

    // ---------- parsers ----------

    private fun newParser(xml: String): XmlPullParser? = try {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        parser
    } catch (_: Exception) {
        null
    }

    /** Value lives either in the `value` attribute or in the element text/CDATA. */
    private fun readDataText(parser: XmlPullParser): String {
        val v = parser.getAttributeValue(null, "value")
        return v ?: parser.nextText().orEmpty()
    }

    private fun parseSearch(xml: String): List<NovelSearchItem> {
        val parser = newParser(xml) ?: return emptyList()
        val result = mutableListOf<NovelSearchItem>()
        var current: NovelSearchItem? = null
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> current = NovelSearchItem(aid = parser.getAttributeValue(null, "aid")?.toIntOrNull() ?: 0)
                        "data" -> {
                            when (parser.getAttributeValue(null, "name")) {
                                "Title" -> current = current?.copy(title = readDataText(parser))
                                "Author" -> current = current?.copy(author = readDataText(parser))
                                "TotalHitsCount" -> current = current?.copy(totalHitsCount = readDataText(parser).toLongOrNull() ?: 0)
                                "PushCount" -> current = current?.copy(pushCount = readDataText(parser).toLongOrNull() ?: 0)
                                "FavCount" -> current = current?.copy(favCount = readDataText(parser).toLongOrNull() ?: 0)
                            }
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "item") {
                    current?.let { if (it.aid > 0) result.add(it) }
                    current = null
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun parseMeta(xml: String): NovelMeta? {
        val parser = newParser(xml) ?: return null
        var meta = NovelMeta()
        var foundTitle = false
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "data") {
                    when (parser.getAttributeValue(null, "name")) {
                        "Title" -> {
                            meta = meta.copy(
                                aid = parser.getAttributeValue(null, "aid")?.toIntOrNull() ?: meta.aid,
                                title = readDataText(parser),
                            )
                            foundTitle = true
                        }
                        "Author" -> meta = meta.copy(author = readDataText(parser))
                        "DayHitsCount" -> meta = meta.copy(dayHitsCount = readDataText(parser).toLongOrNull() ?: 0)
                        "TotalHitsCount" -> meta = meta.copy(totalHitsCount = readDataText(parser).toLongOrNull() ?: 0)
                        "PushCount" -> meta = meta.copy(pushCount = readDataText(parser).toLongOrNull() ?: 0)
                        "FavCount" -> meta = meta.copy(favCount = readDataText(parser).toLongOrNull() ?: 0)
                        "PressId" -> meta = meta.copy(pressId = readDataText(parser))
                        "BookStatus" -> meta = meta.copy(bookStatus = readDataText(parser))
                        "BookLength" -> meta = meta.copy(bookLength = readDataText(parser).toLongOrNull() ?: 0)
                        "LastUpdate" -> meta = meta.copy(lastUpdate = readDataText(parser))
                        "LatestSection" -> meta = meta.copy(
                            latestSectionCid = parser.getAttributeValue(null, "cid")?.toIntOrNull() ?: 0,
                            latestSectionName = readDataText(parser),
                        )
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return if (foundTitle) meta else null
    }

    private fun parseIndex(xml: String): List<VolumeInfo> {
        val parser = newParser(xml) ?: return emptyList()
        val volumes = mutableListOf<VolumeInfo>()
        var currentChapters = mutableListOf<ChapterInfo>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "volume" -> {
                            val vid = parser.getAttributeValue(null, "vid")?.toIntOrNull() ?: 0
                            // KXmlParser.nextText() requires the text to be followed by END_TAG,
                            // but <volume>CDATA is followed by <chapter> children — so read the
                            // text manually by advancing one event.
                            event = parser.next()
                            val name = if (event == XmlPullParser.TEXT) parser.text.orEmpty() else ""
                            currentChapters = mutableListOf()
                            volumes.add(VolumeInfo(vid, name, currentChapters))
                        }
                        "chapter" -> {
                            val cid = parser.getAttributeValue(null, "cid")?.toIntOrNull() ?: 0
                            event = parser.next()
                            val name = if (event == XmlPullParser.TEXT) parser.text.orEmpty() else ""
                            currentChapters.add(ChapterInfo(cid, name))
                        }
                        else -> event = parser.next()
                    }
                } else {
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            AppLog.e("Parse", "parseIndex 异常", e)
        }
        return volumes
    }

    private fun parseChapter(xml: String): List<NovelContentItem> {
        val items = mutableListOf<NovelContentItem>()
        for (line in xml.split("\n")) {
            val t = line.trim()
            if (t.isEmpty()) continue
            if (!t.contains(IMAGE_ENTRY)) {
                items.add(NovelContentItem(NovelContentItem.ContentType.TEXT, t))
                continue
            }
            var searchFrom = 0
            var parsedAny = false
            while (true) {
                val start = t.indexOf(IMAGE_ENTRY, searchFrom)
                if (start == -1) break
                val end = t.indexOf(IMAGE_ENTRY, start + IMAGE_ENTRY.length)
                if (end == -1) break
                val url = t.substring(start + IMAGE_ENTRY.length, end)
                if (url.isNotEmpty()) {
                    items.add(NovelContentItem(NovelContentItem.ContentType.IMAGE, url))
                    parsedAny = true
                }
                searchFrom = end + IMAGE_ENTRY.length
            }
            if (!parsedAny) items.add(NovelContentItem(NovelContentItem.ContentType.TEXT, t))
        }
        return items
    }
}
