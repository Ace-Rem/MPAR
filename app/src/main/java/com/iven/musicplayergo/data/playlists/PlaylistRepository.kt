package com.acerem.musicplayerar.data.playlists

import com.acerem.musicplayerar.models.Music
import com.acerem.musicplayerar.models.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private val playlistDao: PlaylistDao
) {

    fun observePlaylists(deviceSongs: List<Music>): Flow<List<Playlist>> {
        return playlistDao.observePlaylistsWithEntries().map { playlists ->
            playlists.map { it.toPlaylist(deviceSongs) }
        }
    }

    fun observePlaylist(playlistId: Long, deviceSongs: List<Music>): Flow<Playlist?> {
        return playlistDao.observePlaylistWithEntries(playlistId).map { playlist ->
            playlist?.toPlaylist(deviceSongs)
        }
    }

    suspend fun createPlaylist(name: String, songs: List<Music> = emptyList()): Long {
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
        addSongsToPlaylist(playlistId, songs)
        return playlistId
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Music>) {
        val entries = songs.mapNotNull { song ->
            song.id?.let { songId ->
                PlaylistSongEntity(playlistId = playlistId, songId = songId)
            }
        }
        if (entries.isNotEmpty()) {
            playlistDao.insertPlaylistSongs(entries)
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                createdAt = playlist.createdAt
            )
        )
    }

    suspend fun deletePlaylistSongs(playlistId: Long, songIds: List<Long>) {
        playlistDao.deletePlaylistSongs(playlistId, songIds)
    }

    private fun PlaylistWithEntries.toPlaylist(deviceSongs: List<Music>): Playlist {
        val songsById = deviceSongs.associateBy { it.id }
        val sortedEntries = entries.sortedBy { it.addedAt }
        val songs = sortedEntries.mapNotNull { entry -> songsById[entry.songId] }
        return Playlist(
            id = playlist.id,
            name = playlist.name,
            createdAt = playlist.createdAt,
            songs = songs
        )
    }
}
