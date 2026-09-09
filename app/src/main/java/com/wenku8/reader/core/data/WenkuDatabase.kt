package com.wenku8.reader.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ================= Entities =================

@Entity(tableName = "favorite_novels")
data class FavoriteNovelEntity(
    @PrimaryKey val aid: Int,
    val title: String,
    val author: String = "",
    val bookStatus: String = "",
    val lastUpdate: String = "",
    val intro: String = "",
    val latestSectionName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val aid: Int,
    val title: String = "",
    val vid: Int = 0,
    val cid: Int = 0,
    val elementIndex: Int = 0,
    val charOffset: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aid: Int,
    val vid: Int = 0,
    val cid: Int = 0,
    val elementIndex: Int = 0,
    val charOffset: Int = 0,
    val name: String = "",
    val note: String = "",
    val excerpt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val keyword: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

// ================= DAOs =================

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_novels ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FavoriteNovelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_novels WHERE aid = :aid)")
    suspend fun exists(aid: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteNovelEntity)

    @Query("DELETE FROM favorite_novels WHERE aid = :aid")
    suspend fun delete(aid: Int)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE aid = :aid")
    suspend fun get(aid: Int): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE aid = :aid ORDER BY createdAt DESC")
    fun observeByAid(aid: Int): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE keyword = :keyword")
    suspend fun delete(keyword: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

// ================= Database =================

@Database(
    entities = [
        FavoriteNovelEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WenkuDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
