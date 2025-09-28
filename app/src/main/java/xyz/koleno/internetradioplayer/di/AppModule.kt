package xyz.koleno.internetradioplayer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import xyz.koleno.internetradioplayer.data.AppDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun privateAppDatabase(
        @ApplicationContext app: Context
    ) = AppDatabase.getInstance(app)

    @Singleton
    @Provides
    fun provideStationDao(database: AppDatabase) = database.stationDao()

}