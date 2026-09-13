package com.hackwestx.bloomscrolling.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.hackwestx.bloomscrolling.data.AppDatabase
import com.hackwestx.bloomscrolling.data.MIGRATION_1_2
import com.hackwestx.bloomscrolling.data.MIGRATION_2_3
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
            // Caminho v1 -> v2 escrito à mão: preserva os dados de teste já
            // gravados no aparelho. O Room usa a migration quando ela existe e
            // só cai no fallback abaixo para caminhos não previstos.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    fun provideStreakStateDao(db: AppDatabase) = db.streakStateDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context) =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("user_settings")
        }
}
