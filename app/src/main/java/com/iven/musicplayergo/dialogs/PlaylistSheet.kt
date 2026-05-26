package com.acerem.musicplayerar.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.acerem.musicplayerar.MusicViewModel
import com.acerem.musicplayerar.R
import com.acerem.musicplayerar.databinding.GenericItemBinding
import com.acerem.musicplayerar.databinding.ModalRvBinding
import com.acerem.musicplayerar.extensions.applyFullHeightDialog
import com.acerem.musicplayerar.extensions.disableShapeAnimation
import com.acerem.musicplayerar.extensions.handleViewVisibility
import com.acerem.musicplayerar.models.Playlist

class PlaylistSheet : BottomSheetDialogFragment() {

    private var _binding: ModalRvBinding? = null
    private val mMusicViewModel: MusicViewModel by activityViewModels()

    var onPlaylistSelected: ((Playlist) -> Unit)? = null
    var onCreatePlaylistRequested: (() -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dialog?.disableShapeAnimation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ModalRvBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissed?.invoke()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.disableShapeAnimation()
        dialog?.applyFullHeightDialog(requireActivity())

        _binding?.run {
            title.text = getString(R.string.playlists)
            sleepTimerElapsed.handleViewVisibility(show = false)
            btnContainer.handleViewVisibility(show = false)
            bottomDivider.handleViewVisibility(show = false)
            btnDelete.handleViewVisibility(show = false)

            val adapter = PlaylistPickerAdapter()
            modalRv.adapter = adapter

            mMusicViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
                adapter.submit(playlists)
                
                // THÊM DÒNG NÀY: Hiện chữ nếu không có playlist nào (danh sách trống)
                emptyStateText?.handleViewVisibility(show = playlists.isNullOrEmpty())
            }
        }
    }

    private inner class PlaylistPickerAdapter :
        RecyclerView.Adapter<PlaylistPickerAdapter.PlaylistHolder>() {

        private val items = mutableListOf<Playlist>()

        @Suppress("NotifyDataSetChanged")
        fun submit(playlists: List<Playlist>) {
            items.clear()
            items.addAll(playlists)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
            val binding = GenericItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PlaylistHolder(binding)
        }

        override fun getItemCount(): Int = items.size + 1

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
            if (position == items.size) {
                holder.bindCreateAction()
            } else {
                holder.bindPlaylist(items[position])
            }
        }

        inner class PlaylistHolder(private val binding: GenericItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindPlaylist(playlist: Playlist) {
                binding.albumCover.handleViewVisibility(show = false)
                binding.selector.handleViewVisibility(show = false)
                binding.title.text = playlist.name
                binding.subtitle.text = getString(R.string.folder_info, playlist.songCount)
                binding.root.setOnClickListener {
                    onPlaylistSelected?.invoke(playlist)
                    dismissAllowingStateLoss()
                }
            }

            fun bindCreateAction() {
                binding.albumCover.handleViewVisibility(show = false)
                binding.selector.handleViewVisibility(show = false)
                binding.title.text = getString(R.string.create_playlist)
                binding.subtitle.text = ""
                binding.root.setOnClickListener {
                    onCreatePlaylistRequested?.invoke()
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    companion object {
        const val TAG = "PLAYLIST_SHEET"

        @JvmStatic
        fun newInstance() = PlaylistSheet()
    }
}
