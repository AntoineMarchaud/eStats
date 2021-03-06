package com.amarchaud.estats.viewmodel.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.estats.utils.SingleLiveEvent

class NewPositionViewModel : ViewModel() {

    // use singleLiveEvent to add only one time the new position
    private val _newPositionLiveData = SingleLiveEvent<NewPosition>()
    val newPositionLiveData: LiveData<NewPosition>
        get() = _newPositionLiveData

    fun setNewPosition(newPosition: NewPosition) {
        _newPositionLiveData.value = newPosition
    }

    data class NewPosition(val lat: Double, val lon: Double, val name: String, val delta: Int)
}