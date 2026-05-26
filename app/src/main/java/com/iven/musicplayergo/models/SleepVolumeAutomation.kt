package com.acerem.musicplayerar.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SleepVolumeAutomation(
    val enabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val startVolumePercent: Int,
    val endVolumePercent: Int,
    val stepMinutes: Int
)
