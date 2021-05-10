package net.ballmerlabs.subrosa.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    fun providesNewsgroupDao(database: RoomDatabase): NewsGroupDao {
        return database.newsGroupDao()
    }
}