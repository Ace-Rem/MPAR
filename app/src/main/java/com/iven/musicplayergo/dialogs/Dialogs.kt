package com.acerem.musicplayerar.dialogs

import android.app.Activity
import android.content.Context
import android.text.Spanned
import android.widget.EditText
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.widget.Button
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.acerem.musicplayerar.GoPreferences
import com.acerem.musicplayerar.R
import com.acerem.musicplayerar.extensions.toFormattedDuration
import com.acerem.musicplayerar.models.Music
import com.acerem.musicplayerar.models.Playlist
import com.acerem.musicplayerar.player.MediaPlayerHolder
import com.acerem.musicplayerar.ui.UIControlInterface
import com.acerem.musicplayerar.utils.Lists
import com.acerem.musicplayerar.models.SleepVolumeAutomation
import java.util.Locale


object Dialogs {

    @JvmStatic
    fun showPlaylistNameDialog(
        context: Context,
        titleRes: Int,
        initialValue: String = "",
        onConfirmed: (String) -> Unit
    ) {
        val input = EditText(context).apply {
            setText(initialValue)
            setSelection(text.length)
            hint = context.getString(R.string.playlist_name_hint)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val playlistName = input.text?.toString()?.trim().orEmpty()
                if (playlistName.isNotEmpty()) {
                    onConfirmed(playlistName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @JvmStatic
    fun showDeletePlaylistDialog(
        context: Context,
        playlist: Playlist,
        onConfirmed: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.playlist_delete)
            .setMessage(context.getString(R.string.playlist_delete_confirm, playlist.name))
            .setPositiveButton(R.string.delete) { _, _ -> onConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @JvmStatic
    fun showSleepVolumeAutomationDialog(
        context: Context,
        initialValue: SleepVolumeAutomation?,
        onConfirmed: (SleepVolumeAutomation) -> Unit
    ) {
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_sleep_volume_automation, null, false)

        val enabledSwitch = contentView.findViewById<SwitchMaterial>(R.id.switch_enabled)
        val selectTimeButton = contentView.findViewById<Button>(R.id.button_select_time)
        val startVolumeInput = contentView.findViewById<EditText>(R.id.input_start_volume)
        val endVolumeInput = contentView.findViewById<EditText>(R.id.input_end_volume)
        val stepMinutesInput = contentView.findViewById<EditText>(R.id.input_step_minutes)

        var selectedHour = initialValue?.startHour ?: 22
        var selectedMinute = initialValue?.startMinute ?: 0

        fun updateSelectedTimeLabel() {
            selectTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
        }

        enabledSwitch.isChecked = initialValue?.enabled ?: false
        startVolumeInput.setText((initialValue?.startVolumePercent ?: MediaPlayerHolder.getInstance().currentVolumeInPercent).toString())
        endVolumeInput.setText((initialValue?.endVolumePercent ?: 20).toString())
        stepMinutesInput.setText((initialValue?.stepMinutes ?: 1f).toString())
        updateSelectedTimeLabel()

        selectTimeButton.setOnClickListener {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    updateSelectedTimeLabel()
                },
                selectedHour,
                selectedMinute,
                true
            ).show()
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.sleep_volume_automation)
            .setView(contentView)
            .setPositiveButton(R.string.save) { _, _ ->
                val config = SleepVolumeAutomation(
                    enabled = enabledSwitch.isChecked,
                    startHour = selectedHour,
                    startMinute = selectedMinute,
                    startVolumePercent = startVolumeInput.text?.toString()?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                    endVolumePercent = endVolumeInput.text?.toString()?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                    
                    stepMinutes = stepMinutesInput.text?.toString()?.toFloatOrNull()?.coerceAtLeast(0.1f) ?: 1f
                )
                onConfirmed(config)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @JvmStatic
    fun showClearFiltersDialog(activity: Activity) {
        val uiControlInterface = (activity as UIControlInterface)
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.filter_pref_title)
                .setMessage(R.string.filters_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFiltersCleared()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFiltersCleared()
    }

    @JvmStatic
    fun showClearQueueDialog(context: Context) {

        val mediaPlayerHolder = MediaPlayerHolder.getInstance()
        val prefs = GoPreferences.getPrefsInstance()
        fun clearQueue() {
            prefs.isQueue = null
            prefs.queue = null
            with(mediaPlayerHolder) {
                queueSongs.clear()
                setQueueEnabled(enabled = false, canSkip = isQueueStarted)
            }
        }

        if (prefs.isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.queue)
                .setMessage(R.string.queue_songs_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    clearQueue()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        clearQueue()
    }

    @JvmStatic
    fun showClearFavoritesDialog(activity: Activity) {
        val uiControlInterface = activity as UIControlInterface
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.favorites)
                .setMessage(R.string.favorites_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFavoritesUpdated(clear = true)
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFavoritesUpdated(clear = true)
    }

    @JvmStatic
    fun stopPlaybackDialog(context: Context) {
        val mediaPlayerHolder = MediaPlayerHolder.getInstance()
        if (GoPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(R.string.on_close_activity)
                .setPositiveButton(R.string.yes) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = true, fromUser = true, fromFocus = false)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = false, fromUser = true, fromFocus = false)
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
            return
        }
        mediaPlayerHolder.stopPlaybackService(stopPlayback = false, fromUser = true, fromFocus = false)
    }

    @JvmStatic
    fun showSaveSortingDialog(activity: Activity, artistOrFolder: String?, launchedBy: String, sorting: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.sorting_pref)
            .setMessage(R.string.sorting_pref_save)
            .setPositiveButton(R.string.yes) { _, _ ->
                Lists.addToSortings(activity, GoPreferences.PREFS_DETAILS_SORTING, launchedBy, sorting)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                Lists.addToSortings(activity, artistOrFolder, launchedBy, sorting)
            }
            .setNeutralButton(R.string.sorting_pref_reset_neutral) { _, _ ->
                GoPreferences.getPrefsInstance().isSetDefSorting = false
            }
            .show()
    }

    @JvmStatic
    fun showResetSortingsDialog(context: Context) {
        val prefs = GoPreferences.getPrefsInstance()
        if (prefs.isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.sorting_pref)
                .setMessage(R.string.sorting_pref_reset_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    prefs.sortings = null
                    prefs.isSetDefSorting = true
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        prefs.sortings = null
        prefs.isSetDefSorting = true
    }

    @JvmStatic
    fun computeDurationText(ctx: Context, favorite: Music?): Spanned? {
        favorite?.startFrom?.let { start ->
            if (start > 0L) {
                return ctx.getString(
                    R.string.favorite_subtitle,
                    start.toLong().toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    ),
                    favorite.duration.toFormattedDuration(isAlbum = false, isSeekBar = false)
                ).parseAsHtml()
            }
        }
        return favorite?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ?.parseAsHtml()
    }
}
