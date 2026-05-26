package com.acerem.musicplayerar.ui

import com.acerem.musicplayerar.models.Music

interface UIControlInterface {
    fun onAppearanceChanged(isThemeChanged: Boolean)
    fun onOpenNewDetailsFragment()
    fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: String)
    fun onPlaylistSelected(playlistId: Long)
    fun onAddSongsToPlaylist(songs: List<Music>)
    fun onFavoritesUpdated(clear: Boolean)
    fun onFavoriteAddedOrRemoved()
    fun onCloseActivity()
    fun onAddToFilter(stringsToFilter: List<String>?)
    fun onFiltersCleared()
    fun onDenyPermission()
    fun onOpenPlayingArtistAlbum()
    fun onOpenEqualizer()
    fun onOpenSleepTimerDialog()
    fun onEnableEqualizer()
    fun onUpdateSortings()
}
