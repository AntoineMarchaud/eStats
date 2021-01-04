package com.amarchaud.estats.injection

import android.content.Context
import androidx.room.Room
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.database.AppDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
class DaoModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): AppDb {
        return Room.databaseBuilder(
            appContext,
            AppDb::class.java,
            "eStats_db"
        ).build()
    }


    @Singleton
    @Provides
    fun provideDao(appDb: AppDb): AppDao {
        return appDb.AppDao()
    }
}

