package net.ballmerlabs.subrosa.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun providesRoomDatabase(@ApplicationContext context: Context): RoomDatabase {
        return Room.databaseBuilder(context, RoomDatabase::class.java, "maindatabase")
            .build()
    }
    
    @Provides
    @Singleton
    fun providesNewsgroupDao(database: RoomDatabase): NewsGroupDao {
        return database.newsGroupDao()
    }
}