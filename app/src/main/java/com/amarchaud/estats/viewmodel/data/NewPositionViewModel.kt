package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.estats.base.SingleLiveEvent

class NewPositionViewModel : ViewModel() {
    data class NewPosition(val lat: Double, val lon: Double, val name: String, val delta: Int)

    val newPositionLiveData = SingleLiveEvent<NewPosition>()
}