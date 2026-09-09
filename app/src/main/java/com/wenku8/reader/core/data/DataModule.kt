package com.wenku8.reader.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WenkuDatabase =
        Room.databaseBuilder(context, WenkuDatabase::class.java, "wenku8.db")
            .build()

    @Provides
    fun provideFavoriteDao(db: WenkuDatabase) = db.favoriteDao()

    @Provides
    fun provideProgressDao(db: WenkuDatabase) = db.progressDao()

    @Provides
    fun provideBookmarkDao(db: WenkuDatabase) = db.bookmarkDao()

    @Provides
    fun provideSearchHistoryDao(db: WenkuDatabase) = db.searchHistoryDao()
}
