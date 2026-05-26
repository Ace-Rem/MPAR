package com.acerem.musicplayerar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.acerem.musicplayerar.data.playlists.PlaylistDao
import com.acerem.musicplayerar.data.playlists.PlaylistEntity
import com.acerem.musicplayerar.data.playlists.PlaylistSongEntity

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_player_go.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
