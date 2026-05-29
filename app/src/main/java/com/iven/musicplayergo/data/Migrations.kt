package com.acerem.musicplayerar.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 to version 2: Add position column to playlist_songs
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the position column to playlist_songs table
        // Set default position to be the order of insertion (based on addedAt timestamp)
        database.execSQL("""
            ALTER TABLE playlist_songs ADD COLUMN position INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        // Update positions based on addedAt timestamp (oldest first = position 0)
        database.execSQL("""
            UPDATE playlist_songs 
            SET position = (
                SELECT COUNT(*) FROM playlist_songs ps2 
                WHERE ps2.playlistId = playlist_songs.playlistId 
                AND ps2.addedAt <= playlist_songs.addedAt 
                AND (ps2.addedAt < playlist_songs.addedAt OR ps2.songId <= playlist_songs.songId)
            ) - 1
        """.trimIndent())
    }
}
