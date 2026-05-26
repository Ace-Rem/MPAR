package com.acerem.musicplayerar.models

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<Music>
) {
    val songCount: Int
        get() = songs.size
}
