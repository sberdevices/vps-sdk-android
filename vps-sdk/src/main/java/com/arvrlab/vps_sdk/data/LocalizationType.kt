package com.arvrlab.vps_sdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LocalizationType : Parcelable

@Parcelize
object Photo : LocalizationType()

@Parcelize
data class MobileVps(val neuroModelUrl: String) : LocalizationType()