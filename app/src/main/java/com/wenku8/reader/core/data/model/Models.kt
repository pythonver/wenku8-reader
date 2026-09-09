package com.wenku8.reader.core.data.model

/** A search result row — the current search endpoint returns inline info. */
data class NovelSearchItem(
    val aid: Int = 0,
    val title: String = "",
    val author: String = "",
    val totalHitsCount: Long = 0,
    val pushCount: Long = 0,
    val favCount: Long = 0,
)

/** Full metadata from `book&do=meta`. */
data class NovelMeta(
    val aid: Int = 0,
    val title: String = "",
    val author: String = "",
    val dayHitsCount: Long = 0,
    val totalHitsCount: Long = 0,
    val pushCount: Long = 0,
    val favCount: Long = 0,
    val pressId: String = "",
    val bookStatus: String = "",
    val bookLength: Long = 0,
    val lastUpdate: String = "",
    val latestSectionCid: Int = 0,
    val latestSectionName: String = "",
)

data class ChapterInfo(val cid: Int, val name: String)

data class VolumeInfo(val vid: Int, val name: String, val chapters: List<ChapterInfo> = emptyList())

data class NovelDetail(
    val meta: NovelMeta,
    val intro: String,
    val volumes: List<VolumeInfo>,
)

/** One paragraph (TEXT) or one illustration (IMAGE) from a chapter. */
data class NovelContentItem(val type: ContentType, val content: String) {
    enum class ContentType { TEXT, IMAGE }
}
