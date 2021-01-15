package com.amarchaud.estats.model.other

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact (var name : String, var addr : String) : Parcelable