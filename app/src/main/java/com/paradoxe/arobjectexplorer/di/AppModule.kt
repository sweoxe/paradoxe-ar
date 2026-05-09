package com.paradoxe.arobjectexplorer.di

import android.content.Context
import androidx.room.Room
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.paradoxe.arobjectexplorer.data.local.ParadoxeDatabase
import com.paradoxe.arobjectexplorer.data.local.SearchResultDao
import com.paradoxe.arobjectexplorer.data.local.SettingsManager
import com.paradoxe.arobjectexplorer.data.remote.WikipediaApi
import com.paradoxe.arobjectexplorer.data.repository.ObjectRepositoryImpl
import com.paradoxe.arobjectexplorer.domain.repository.ObjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWikipediaApi(): WikipediaApi {
        return Retrofit.Builder()
            .baseUrl("https://ru.wikipedia.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikipediaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideObjectDetector(): ObjectDetector {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        return ObjectDetection.getClient(options)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ParadoxeDatabase {
        return Room.databaseBuilder(
            context,
            ParadoxeDatabase::class.java,
            "paradoxe_db"
        ).build()
    }

    @Provides
    fun provideSearchResultDao(db: ParadoxeDatabase): SearchResultDao = db.searchResultDao()

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager = SettingsManager(context)

    @Provides
    @Singleton
    fun provideObjectRepository(
        wikipediaApi: WikipediaApi,
        dao: SearchResultDao
    ): ObjectRepository = ObjectRepositoryImpl(wikipediaApi, dao)
}
