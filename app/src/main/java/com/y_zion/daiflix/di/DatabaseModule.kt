package com.y_zion.daiflix.di

import android.content.Context
import androidx.room.Room
import com.y_zion.daiflix.data.local.AppDatabase
import com.y_zion.daiflix.data.local.MIGRATION_1_2
import com.y_zion.daiflix.data.local.VodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "daiflix.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideVodDao(database: AppDatabase): VodDao {
        return database.vodDao()
    }
}
