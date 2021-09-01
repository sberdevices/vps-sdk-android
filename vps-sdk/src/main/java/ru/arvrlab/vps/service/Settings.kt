package ru.arvrlab.vps.service

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Settings(
    var url: String,
    var locationID: String,
    var onlyForce: Boolean = true,
    var timerInterval: Long,
    var needLocation: Boolean,
    var isNeuro: Boolean
): Parcelable