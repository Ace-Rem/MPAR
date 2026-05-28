package com.acerem.musicplayerar.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.acerem.musicplayerar.GoConstants
import com.acerem.musicplayerar.GoPreferences
import com.acerem.musicplayerar.MusicViewModel
import com.acerem.musicplayerar.R
import com.acerem.musicplayerar.databinding.FragmentAllMusicBinding
import com.acerem.musicplayerar.databinding.MusicItemBinding
import com.acerem.musicplayerar.extensions.setTitleColor
import com.acerem.musicplayerar.extensions.toFormattedDate
import com.acerem.musicplayerar.extensions.toFormattedDuration
import com.acerem.musicplayerar.extensions.toName
import com.acerem.musicplayerar.models.Music
import com.acerem.musicplayerar.player.MediaPlayerHolder
import com.acerem.musicplayerar.ui.MediaControlInterface
import com.acerem.musicplayerar.ui.SelectionStateController
import com.acerem.musicplayerar.ui.UIControlInterface
import com.acerem.musicplayerar.utils.Lists
import com.acerem.musicplayerar.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider


/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _allMusicFragmentBinding: FragmentAllMusicBinding? = null
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    // view model
    private lateinit var mMusicViewModel: MusicViewModel

    // sorting
    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoPreferences.getPrefsInstance().allMusicSorting

    private var mAllMusic: List<Music>? = null
    private var actionMode: ActionMode? = null
    private val isActionMode get() = actionMode != null
    private val mSelectionController = SelectionStateController<Long>()

    private val sIsFastScrollerPopup get() = (mSorting == GoConstants.ASCENDING_SORTING || mSorting == GoConstants.DESCENDING_SORTING) && GoPreferences.getPrefsInstance().songsVisualization != GoConstants.FN

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _allMusicFragmentBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _allMusicFragmentBinding = FragmentAllMusicBinding.inflate(inflater, container, false)
        return _allMusicFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity())[MusicViewModel::class.java].apply {
                deviceMusic.observe(viewLifecycleOwner) { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mAllMusic = Lists.getSortedMusicListForAllMusic(
                            mSorting,
                            mMusicViewModel.deviceMusicFiltered
                        )
                        finishSetup()
                    }
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setMusicDataSource(musicList: List<Music>?) {
        musicList?.run {
            mAllMusic = this
            _allMusicFragmentBinding?.allMusicRv?.adapter?.notifyDataSetChanged()
        }
    }

    private fun finishSetup() {

        _allMusicFragmentBinding?.run {

            allMusicRv.adapter = AllMusicAdapter()

            FastScrollerBuilder(allMusicRv).useMd2Style().build()

            shuffleFab.visibility = View.GONE

            searchToolbar.let { stb ->

                stb.inflateMenu(R.menu.menu_music_search)
                stb.overflowIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_sort)
                stb.setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                with(stb.menu) {

                    mSortMenuItem = Lists.getSelectedSortingForMusic(mSorting, this).apply {
                        setTitleColor(Theming.resolveThemeColor(resources))
                    }

                    with (findItem(R.id.action_search).actionView as SearchView) {
                        setOnQueryTextListener(this@AllMusicFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                            stb.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
                        }
                    }

                    setMenuOnItemClickListener(stb.menu)
                }
            }
        }

        tintSleepTimerIcon(enabled = MediaPlayerHolder.getInstance().isSleepTimer)
    }

    fun tintSleepTimerIcon(enabled: Boolean) {
        _allMusicFragmentBinding?.searchToolbar?.run {
            Theming.tintSleepTimerMenuItem(this, enabled)
        }
    }

    private fun setMenuOnItemClickListener(menu: Menu) {

        _allMusicFragmentBinding?.searchToolbar?.setOnMenuItemClickListener {

            if (it.itemId == R.id.default_sorting || it.itemId == R.id.ascending_sorting
                || it.itemId == R.id.descending_sorting || it.itemId == R.id.date_added_sorting
                || it.itemId == R.id.date_added_sorting_inv || it.itemId == R.id.artist_sorting
                || it.itemId == R.id.artist_sorting_inv || it.itemId == R.id.album_sorting
                || it.itemId == R.id.album_sorting_inv) {

                mSorting = it.order
                mAllMusic = Lists.getSortedMusicListForAllMusic(mSorting, mAllMusic)

                setMusicDataSource(mAllMusic)

                mSortMenuItem.setTitleColor(
                    Theming.resolveColorAttr(requireContext(), android.R.attr.textColorPrimary)
                )

                mSortMenuItem = Lists.getSelectedSortingForMusic(mSorting, menu).apply {
                    setTitleColor(Theming.resolveThemeColor(resources))
                }

                GoPreferences.getPrefsInstance().allMusicSorting = mSorting

            } else if (it.itemId != R.id.action_search) {
                mUIControlInterface.onOpenSleepTimerDialog()
            }

            return@setOnMenuItemClickListener true
        }
    }

    fun onSongVisualizationChanged() = if (_allMusicFragmentBinding != null) {
        mAllMusic = Lists.getSortedMusicListForAllMusic(mSorting, mAllMusic)
        setMusicDataSource(mAllMusic)
        true
    } else {
        false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setMusicDataSource(
            Lists.processQueryForMusic(newText,
                Lists.getSortedMusicListForAllMusic(mSorting, mMusicViewModel.deviceMusicFiltered)
            ) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun startActionMode() {
        if (!isActionMode) {
            actionMode = _allMusicFragmentBinding?.searchToolbar?.startActionMode(actionModeCallback)
        }
        updateActionModeState()
    }

    private fun updateActionModeState() {

    actionMode?.title = mSelectionController.selectionCount().toString()

    actionMode?.menu?.findItem(R.id.action_hide)?.isVisible = false

    actionMode?.menu?.findItem(R.id.action_select_all)?.isVisible =
        mSelectionController.selectionCount() < (mAllMusic?.size ?: 0)

    actionMode?.menu?.findItem(R.id.action_clear_selection)?.isVisible =
        mSelectionController.hasSelection()
}

    private fun updateSongSelectionBackground(view: View, selected: Boolean) {
        if (selected) {
            view.setBackgroundColor(
                ColorUtils.setAlphaComponent(Theming.resolveThemeColor(resources), 72)
            )
            return
        }
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        view.setBackgroundResource(typedValue.resourceId)
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
                    val selectedSongs = mAllMusic.orEmpty().filter { song ->
                        song.id != null && mSelectionController.isSelected(song.id)
                    }
                    if (selectedSongs.isNotEmpty()) {
                        mUIControlInterface.onAddSongsToPlaylist(selectedSongs)
                    }
                    stopActionMode()
                    true
                }
                R.id.action_select_all -> {
                    mSelectionController.selectAll(mAllMusic.orEmpty().mapNotNull { song -> song.id })
                    _allMusicFragmentBinding?.allMusicRv?.adapter?.notifyDataSetChanged()
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

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            mSelectionController.clear()
            _allMusicFragmentBinding?.allMusicRv?.adapter?.notifyDataSetChanged()
        }
    }

    private fun stopActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    private inner class AllMusicAdapter : RecyclerView.Adapter<AllMusicAdapter.SongsHolder>(), PopupTextProvider {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsHolder {
            val binding = MusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SongsHolder(binding)
        }

        override fun getPopupText(position: Int): String {
            if (sIsFastScrollerPopup) {
                mAllMusic?.get(position)?.title?.run {
                    if (isNotEmpty()) return first().toString()
                }
            }
            return ""
        }

        override fun getItemCount(): Int {
            return mAllMusic?.size!!
        }

        override fun onBindViewHolder(holder: SongsHolder, position: Int) {
            holder.bindItems(mAllMusic?.get(holder.absoluteAdapterPosition))
        }

        inner class SongsHolder(private val binding: MusicItemBinding): RecyclerView.ViewHolder(binding.root) {

            fun bindItems(itemSong: Music?) {

                with(binding) {

                    val formattedDuration = itemSong?.duration?.toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    )

                    duration.text = getString(R.string.duration_date_added, formattedDuration,
                        itemSong?.dateAdded?.toFormattedDate())
                    title.text = itemSong.toName()
                    subtitle.text =
                        getString(R.string.artist_and_album, itemSong?.artist, itemSong?.album)

                    root.setOnClickListener {
                        itemSong?.id?.let { songId ->
                            if (isActionMode) {
                                mSelectionController.toggle(songId)
                                notifyItemChanged(absoluteAdapterPosition)
                                if (!mSelectionController.hasSelection()) {
                                    stopActionMode()
                                } else {
                                    updateActionModeState()
                                }
                                return@setOnClickListener
                            }
                        }
                        with(MediaPlayerHolder.getInstance()) {
                            if (isCurrentSongFM) currentSongFM = null
                        }
                        mMediaControlInterface.onSongSelected(
                            itemSong,
                            mAllMusic,
                            GoConstants.ARTIST_VIEW
                        )
                    }

                    root.setOnLongClickListener {
                        itemSong?.id?.let { songId ->
                            startActionMode()
                            mSelectionController.select(songId)
                            notifyItemChanged(absoluteAdapterPosition)
                            updateActionModeState()
                            return@setOnLongClickListener true
                        }
                        false
                    }

                    updateSongSelectionBackground(
                        root,
                        itemSong?.id != null && mSelectionController.isSelected(itemSong.id)
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AllMusicFragment.
         */
        @JvmStatic
        fun newInstance() = AllMusicFragment()
    }
}
