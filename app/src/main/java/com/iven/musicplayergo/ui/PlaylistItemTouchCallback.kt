package com.acerem.musicplayerar.ui

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class PlaylistItemTouchCallback(
    private val songs: MutableList<com.acerem.musicplayerar.models.Music>,
    private val onReorder: (fromPosition: Int, toPosition: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.absoluteAdapterPosition
        val toPosition = target.absoluteAdapterPosition

        if (fromPosition < songs.size && toPosition < songs.size) {
            // Swap items in the list
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(songs, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(songs, i, i - 1)
                }
            }
            // Notify adapter of the move
            recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
            return true
        }
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No swipe functionality for playlist reordering
    }

    override fun isLongPressDragEnabled() = false  // Drag only from handle

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                viewHolder?.itemView?.alpha = 0.7f  // Highlight dragged item
            }
            else -> {
                viewHolder?.itemView?.alpha = 1.0f  // Restore opacity
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        val fromPosition = viewHolder.absoluteAdapterPosition
        // Notify callback that reordering is complete
        onReorder(fromPosition, fromPosition)
    }
}
