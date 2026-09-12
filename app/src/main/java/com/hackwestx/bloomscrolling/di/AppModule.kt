package com.hackwestx.bloomscrolling.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.hackwestx.bloomscrolling.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "beyond_the_feed.db")
            // Fine for a 24-hour build; never ship this to production.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSurveyDao(db: AppDatabase) = db.surveyDao()

    @Provides
    fun provideUsageDao(db: AppDatabase) = db.usageDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase) = db.settingsDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context) =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("user_settings")
        }
}
