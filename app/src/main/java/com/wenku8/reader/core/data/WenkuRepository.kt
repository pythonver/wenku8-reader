package com.wenku8.reader.core.data

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.wenku8.reader.core.data.model.NovelDetail
import com.wenku8.reader.core.data.model.NovelMeta
import com.wenku8.reader.core.util.AppLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Single entry point for the app: wenku8 network + local Room + preferences. */
@Singleton
class WenkuRepository @Inject constructor(
    private val api: WenkuApi,
    private val db: WenkuDatabase,
    val preferences: UserPreferences,
) {

    // ---- network ----
    suspend fun search(keyword: String) = api.searchNovel(keyword)

    suspend fun fetchDetail(aid: Int): NovelDetail? {
        val meta = api.fetchMeta(aid) ?: run {
            AppLog.e("Repo", "fetchDetail($aid) meta 为空")
            return null
        }
        val intro = api.fetchIntro(aid).orEmpty()
        val volumes = api.fetchIndex(aid)
        AppLog.i("Repo", "fetchDetail($aid)「${meta.title}」卷数=${volumes.size} 章节数=${volumes.sumOf { it.chapters.size }}")
        return NovelDetail(meta = meta, intro = intro, volumes = volumes)
    }

    suspend fun fetchChapter(aid: Int, cid: Int) = api.fetchChapter(aid, cid)

    /** Cover is a binary POST response; decode to an ImageBitmap for display. */
    suspend fun fetchCoverBitmap(aid: Int): ImageBitmap? {
        val bytes = api.fetchCover(aid) ?: return null
        return runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
        }.getOrNull()
    }

    // In-memory cover cache so list cards don't refetch every scroll.
    private val coverCache = LruCache<Int, ImageBitmap>(200)

    suspend fun loadCoverBitmap(aid: Int): ImageBitmap? {
        coverCache.get(aid)?.let { return it }
        val bmp = fetchCoverBitmap(aid)
        if (bmp != null) coverCache.put(aid, bmp)
        return bmp
    }

    // ---- favorites ----
    fun observeFavorites(): Flow<List<FavoriteNovelEntity>> = db.favoriteDao().observeAll()

    suspend fun isFavorite(aid: Int): Boolean = db.favoriteDao().exists(aid)

    suspend fun addFavorite(meta: NovelMeta, intro: String) {
        db.favoriteDao().upsert(
            FavoriteNovelEntity(
                aid = meta.aid,
                title = meta.title,
                author = meta.author,
                bookStatus = meta.bookStatus,
                lastUpdate = meta.lastUpdate,
                intro = intro,
                latestSectionName = meta.latestSectionName,
            )
        )
    }

    suspend fun removeFavorite(aid: Int) = db.favoriteDao().delete(aid)

    // ---- reading progress ----
    fun observeRecentReads(): Flow<List<ReadingProgressEntity>> = db.progressDao().observeAll()

    suspend fun getProgress(aid: Int): ReadingProgressEntity? = db.progressDao().get(aid)

    suspend fun saveProgress(aid: Int, title: String, vid: Int, cid: Int, elementIndex: Int, charOffset: Int) {
        db.progressDao().upsert(
            ReadingProgressEntity(
                aid = aid,
                title = title,
                vid = vid,
                cid = cid,
                elementIndex = elementIndex,
                charOffset = charOffset,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    // ---- bookmarks ----
    fun observeBookmarks(aid: Int): Flow<List<BookmarkEntity>> = db.bookmarkDao().observeByAid(aid)

    suspend fun addBookmark(bookmark: BookmarkEntity) = db.bookmarkDao().insert(bookmark)

    suspend fun removeBookmark(id: Long) = db.bookmarkDao().delete(id)

    // ---- search history ----
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>> = db.searchHistoryDao().observeAll()

    suspend fun addSearchHistory(keyword: String) {
        db.searchHistoryDao().upsert(SearchHistoryEntity(keyword = keyword))
    }

    suspend fun removeSearchHistory(keyword: String) = db.searchHistoryDao().delete(keyword)

    suspend fun clearSearchHistory() = db.searchHistoryDao().clearAll()
}
