package com.acerem.musicplayerar.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.acerem.musicplayerar.GoConstants
import com.acerem.musicplayerar.GoPreferences
import com.acerem.musicplayerar.MusicViewModel
import com.acerem.musicplayerar.R
import com.acerem.musicplayerar.databinding.FragmentMusicContainersBinding
import com.acerem.musicplayerar.databinding.GenericItemBinding
import com.acerem.musicplayerar.dialogs.Dialogs
import com.acerem.musicplayerar.extensions.handleViewVisibility
import com.acerem.musicplayerar.extensions.loadWithError
import com.acerem.musicplayerar.extensions.setTitleColor
import com.acerem.musicplayerar.extensions.waitForCover
import com.acerem.musicplayerar.models.Music
import com.acerem.musicplayerar.models.Playlist
import com.acerem.musicplayerar.player.MediaPlayerHolder
import com.acerem.musicplayerar.ui.SelectionStateController
import com.acerem.musicplayerar.ui.UIControlInterface
import com.acerem.musicplayerar.utils.Lists
import com.acerem.musicplayerar.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider

class MusicContainersFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _musicContainerListBinding: FragmentMusicContainersBinding? = null

    private lateinit var mMusicViewModel: MusicViewModel

    private var mLaunchedBy = GoConstants.ARTIST_VIEW

    private var mList: MutableList<String>? = null
    private var mPlaylists: List<Playlist>? = null

    private lateinit var mListAdapter: MusicContainersAdapter

    private lateinit var mUiControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoConstants.DESCENDING_SORTING

    private val sLaunchedByArtistView get() = mLaunchedBy == GoConstants.ARTIST_VIEW
    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW
    private val sLaunchedByPlaylistView get() = mLaunchedBy == GoConstants.PLAYLIST_VIEW

    private val sIsFastScrollerPopup get() =
        mSorting == GoConstants.ASCENDING_SORTING || mSorting == GoConstants.DESCENDING_SORTING

    private var actionMode: ActionMode? = null
    private val isActionMode get() = actionMode != null
    private val mSelectionController = SelectionStateController<String>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_LAUNCHED_BY)?.let { launchedBy ->
            mLaunchedBy = launchedBy
        }

        try {
            mUiControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _musicContainerListBinding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _musicContainerListBinding = FragmentMusicContainersBinding.inflate(inflater, container, false)
        return _musicContainerListBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        mSorting = getSortingMethodFromPrefs()

        if (sLaunchedByPlaylistView) {
            mMusicViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
                mPlaylists = Lists.getSortedPlaylists(mSorting, playlists)
                finishSetup()
            }
        } else {
            mMusicViewModel.deviceMusic.observe(viewLifecycleOwner) { returnedMusic ->
                if (!returnedMusic.isNullOrEmpty()) {
                    mList = getSortedList()
                    finishSetup()
                }
            }
        }
    }

    private fun finishSetup() {
        _musicContainerListBinding?.artistsFoldersRv?.run {
            setHasFixedSize(true)
            itemAnimator = null
            if (!::mListAdapter.isInitialized) {
                mListAdapter = MusicContainersAdapter()
                adapter = mListAdapter
                FastScrollerBuilder(this).useMd2Style().build()
                if (sLaunchedByAlbumView) recycledViewPool.setMaxRecycledViews(0, 0)
            } else {
                mListAdapter.notifyDataSetChanged()
            }
        }

        _musicContainerListBinding?.searchToolbar?.let { stb ->
            if (stb.menu.size() == 0) {
                stb.inflateMenu(R.menu.menu_search)
                stb.overflowIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_sort)
                stb.setNavigationOnClickListener {
                    mUiControlInterface.onCloseActivity()
                }

                with(stb.menu) {
                    mSortMenuItem = Lists.getSelectedSorting(mSorting, this).apply {
                        setTitleColor(Theming.resolveThemeColor(resources))
                    }

                    with(findItem(R.id.action_search).actionView as SearchView) {
                        setOnQueryTextListener(this@MusicContainersFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                            stb.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
                        }
                    }
                }
                setMenuOnItemClickListener()
            }
            stb.title = getFragmentTitle()
        }

        tintSleepTimerIcon(enabled = MediaPlayerHolder.getInstance().isSleepTimer)
    }

    fun tintSleepTimerIcon(enabled: Boolean) {
        _musicContainerListBinding?.searchToolbar?.run {
            Theming.tintSleepTimerMenuItem(this, enabled)
        }
    }

    private fun getSortedList(): MutableList<String>? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                Lists.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceAlbumsByArtist?.keys?.toMutableList()
                )
            GoConstants.FOLDER_VIEW ->
                Lists.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceMusicByFolder?.keys?.toMutableList()
                )
            else ->
                Lists.getSortedListWithNull(
                    mSorting,
                    mMusicViewModel.deviceMusicByAlbum?.keys?.toMutableList()
                )
        }
    }

    private fun getSortingMethodFromPrefs(): Int {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                GoPreferences.getPrefsInstance().artistsSorting
            GoConstants.FOLDER_VIEW ->
                GoPreferences.getPrefsInstance().foldersSorting
            GoConstants.ALBUM_VIEW ->
                GoPreferences.getPrefsInstance().albumsSorting
            else ->
                GoConstants.DEFAULT_SORTING
        }
    }

    private fun getFragmentTitle(): String {
        val stringId = when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                R.string.artists
            GoConstants.FOLDER_VIEW ->
                R.string.folders
            GoConstants.PLAYLIST_VIEW ->
                R.string.playlists
            else ->
                R.string.albums
        }
        return getString(stringId)
    }

    private fun setListDataSource(selectedList: List<String>?) {
        mList = selectedList?.toMutableList()
        trimSelection()
        mListAdapter.notifyDataSetChanged()
    }

    private fun setPlaylistDataSource(selectedList: List<Playlist>?) {
        mPlaylists = selectedList
        mListAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onUpdateCoverOption() {
        _musicContainerListBinding?.artistsFoldersRv?.adapter?.notifyDataSetChanged()
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (sLaunchedByPlaylistView) {
            setPlaylistDataSource(
                Lists.processQueryForPlaylists(
                    newText,
                    Lists.getSortedPlaylists(mSorting, mMusicViewModel.playlists.value)
                ) ?: mPlaylists
            )
        } else {
            setListDataSource(
                Lists.processQueryForStringsLists(newText, getSortedList()) ?: mList
            )
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun setMenuOnItemClickListener() {
        _musicContainerListBinding?.searchToolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.sleeptimer -> mUiControlInterface.onOpenSleepTimerDialog()
                else -> if (it.itemId != R.id.action_search) {
                    mSorting = it.order

                    if (sLaunchedByPlaylistView) {
                        mPlaylists = Lists.getSortedPlaylists(mSorting, mMusicViewModel.playlists.value)
                        setPlaylistDataSource(mPlaylists)
                    } else {
                        mList = getSortedList()
                        setListDataSource(mList)
                    }

                    mSortMenuItem.setTitleColor(
                        Theming.resolveColorAttr(requireContext(), android.R.attr.textColorPrimary)
                    )

                    mSortMenuItem = Lists.getSelectedSorting(mSorting, _musicContainerListBinding?.searchToolbar?.menu!!).apply {
                        setTitleColor(Theming.resolveThemeColor(resources))
                    }

                    saveSortingMethodToPrefs(mSorting)
                }
            }
            true
        }
    }

    private fun saveSortingMethodToPrefs(sortingMethod: Int) {
        with(GoPreferences.getPrefsInstance()) {
            when (mLaunchedBy) {
                GoConstants.ARTIST_VIEW -> artistsSorting = sortingMethod
                GoConstants.FOLDER_VIEW -> foldersSorting = sortingMethod
                GoConstants.ALBUM_VIEW -> albumsSorting = sortingMethod
            }
        }
    }

    fun stopActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    private fun trimSelection() {
        val validKeys = currentItems().toSet()
        val retainedKeys = mSelectionController.getSelectedKeys().filter { validKeys.contains(it) }
        if (retainedKeys.size != mSelectionController.selectionCount()) {
            mSelectionController.clear()
            retainedKeys.forEach { retainedKey ->
                mSelectionController.select(retainedKey)
            }
            if (mSelectionController.selectionCount() == 0) {
                stopActionMode()
            } else {
                updateActionModeState()
            }
        }
    }

    private fun currentItems(): List<String> {
        return if (sLaunchedByPlaylistView) {
            mPlaylists?.map { playlist -> playlist.name }.orEmpty()
        } else {
            mList.orEmpty()
        }
    }

    private fun startActionMode() {
        if (!isActionMode) {
            actionMode = _musicContainerListBinding?.searchToolbar?.startActionMode(actionModeCallback)
        }
        updateActionModeState()
    }

    private fun updateActionModeState() {
        actionMode?.title = mSelectionController.selectionCount().toString()
        actionMode?.menu?.findItem(R.id.action_hide)?.isVisible = !sLaunchedByPlaylistView
        actionMode?.menu?.findItem(R.id.action_select_all)?.isVisible =
            mSelectionController.selectionCount() < currentItems().size
        actionMode?.menu?.findItem(R.id.action_clear_selection)?.isVisible =
            mSelectionController.hasSelection()
    }

    private fun collectSelectedSongs(): List<Music> {
        val selectedSongs = mSelectionController.getSelectedKeys().flatMap { item ->
            getSongsForItem(item).orEmpty()
        }
        return selectedSongs.distinctBy { song -> song.id to song.albumId }
    }

    private fun getSongsForItem(item: String): List<Music>? {
        return when {
            sLaunchedByArtistView -> mMusicViewModel.deviceSongsByArtist?.get(item)
            mLaunchedBy == GoConstants.FOLDER_VIEW -> mMusicViewModel.deviceMusicByFolder?.get(item)
            else -> mMusicViewModel.deviceMusicByAlbum?.get(item)
        }
    }

    private fun showPlaylistPopup(anchor: View, playlist: Playlist) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.popup_playlist)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename_playlist -> {
                        Dialogs.showPlaylistNameDialog(
                            context = requireContext(),
                            titleRes = R.string.playlist_rename,
                            initialValue = playlist.name
                        ) { playlistName ->
                            mMusicViewModel.renamePlaylist(playlist.id, playlistName)
                        }
                        true
                    }
                    R.id.action_delete_playlist -> {
                        Dialogs.showDeletePlaylistDialog(requireContext(), playlist) {
                            mMusicViewModel.deletePlaylist(playlist)
                        }
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun handleContainerClick(item: String, position: Int) {
        if (isActionMode) {
            mSelectionController.toggle(item)
            mListAdapter.notifyItemChanged(position)
            if (!mSelectionController.hasSelection()) {
                stopActionMode()
            } else {
                updateActionModeState()
            }
            return
        }
        mUiControlInterface.onArtistOrFolderSelected(item, mLaunchedBy)
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            requireActivity().menuInflater.inflate(R.menu.menu_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val iconColor = Theming.resolveColorAttr(requireContext(), android.R.attr.textColorPrimary)
        
        menu?.let {
            for (i in 0 until it.size()) {
                it.getItem(i).icon?.mutate()?.setTint(iconColor)
            }
        }
        return true
    }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_add_to_playlist -> {
                    val songs = collectSelectedSongs()
                    if (songs.isNotEmpty()) {
                        mUiControlInterface.onAddSongsToPlaylist(songs)
                    }
                    stopActionMode()
                    true
                }
                R.id.action_hide -> {
                    mUiControlInterface.onAddToFilter(mSelectionController.getSelectedKeys().toList())
                    stopActionMode()
                    true
                }
                R.id.action_select_all -> {
                    mSelectionController.selectAll(currentItems())
                    mListAdapter.notifyDataSetChanged()
                    updateActionModeState()
                    true
                }
                R.id.action_clear_selection -> {
                    stopActionMode()
                    true
                }
                else -> false
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            mSelectionController.clear()
            mListAdapter.notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG_LAUNCHED_BY = "SELECTED_FRAGMENT"

        @JvmStatic
        fun newInstance(launchedBy: String) = MusicContainersFragment().apply {
            arguments = bundleOf(TAG_LAUNCHED_BY to launchedBy)
        }
    }

    private inner class MusicContainersAdapter :
        RecyclerView.Adapter<MusicContainersAdapter.ArtistHolder>(),
        PopupTextProvider {

        override fun getPopupText(position: Int): String {
            if (sIsFastScrollerPopup) {
                val itemName = if (sLaunchedByPlaylistView) {
                    mPlaylists?.get(position)?.name
                } else {
                    mList?.get(position)
                }
                itemName?.run {
                    if (isNotEmpty()) return first().toString()
                }
            }
            return ""
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
            val binding = GenericItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ArtistHolder(binding)
        }

        override fun getItemCount(): Int {
            return if (sLaunchedByPlaylistView) {
                mPlaylists?.size ?: 0
            } else {
                mList?.size ?: 0
            }
        }

        override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
            if (sLaunchedByPlaylistView) {
                holder.bindPlaylist(mPlaylists?.get(holder.absoluteAdapterPosition))
            } else {
                holder.bindItem(mList?.get(holder.absoluteAdapterPosition)!!)
            }
        }

        inner class ArtistHolder(private val binding: GenericItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindItem(item: String) {
                with(binding) {
                    if (sLaunchedByAlbumView) {
                        albumCover.background.alpha = Theming.getAlbumCoverAlpha(requireContext())
                        mMusicViewModel.deviceMusicByAlbum?.get(item)?.first()?.albumId?.waitForCover(requireContext()) { bmp, error ->
                            albumCover.loadWithError(bmp, error, R.drawable.ic_music_note_cover_alt)
                        }
                    } else {
                        albumCover.handleViewVisibility(show = false)
                    }

                    title.text = item
                    subtitle.text = getItemsSubtitle(item)
                    selector.handleViewVisibility(show = mSelectionController.isSelected(item))

                    root.setOnClickListener {
                        handleContainerClick(item, absoluteAdapterPosition)
                    }
                    root.setOnLongClickListener {
                        startActionMode()
                        mSelectionController.select(item)
                        notifyItemChanged(absoluteAdapterPosition)
                        updateActionModeState()
                        true
                    }
                }
            }

            fun bindPlaylist(playlist: Playlist?) {
                if (playlist == null) return
                with(binding) {
                    albumCover.handleViewVisibility(show = false)
                    selector.handleViewVisibility(show = false)
                    title.text = playlist.name
                    subtitle.text = getString(R.string.folder_info, playlist.songCount)

                    root.setOnClickListener {
                        mUiControlInterface.onPlaylistSelected(playlist.id)
                    }
                    root.setOnLongClickListener {
                        showPlaylistPopup(root, playlist)
                        true
                    }
                }
            }
        }

        private fun getItemsSubtitle(item: String): String? {
            return when (mLaunchedBy) {
                GoConstants.ARTIST_VIEW ->
                    getString(
                        R.string.artist_info,
                        mMusicViewModel.deviceAlbumsByArtist?.getValue(item)?.size,
                        mMusicViewModel.deviceSongsByArtist?.getValue(item)?.size
                    )
                GoConstants.FOLDER_VIEW ->
                    getString(
                        R.string.folder_info,
                        mMusicViewModel.deviceMusicByFolder?.getValue(item)?.size
                    )
                else ->
                    mMusicViewModel.deviceMusicByAlbum?.get(item)?.firstOrNull()?.artist
            }
        }
    }
}
