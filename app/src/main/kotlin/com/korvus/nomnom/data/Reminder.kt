package com.korvus.nomnom.data

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val hour: Int,        // 0..23
    val minute: Int,      // 0..59
    val text: String,
    val daily: Boolean,   // true — каждый день, false — один раз
    val enabled: Boolean = true,
)
