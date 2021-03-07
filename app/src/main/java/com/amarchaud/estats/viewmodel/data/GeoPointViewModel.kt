package com.amarchaud.estats.viewmodel.data

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.estats.utils.SingleLiveEvent

class GeoPointViewModel : ViewModel() {

    private val _geoLoc = MutableLiveData<Location>()
    val geoLoc: LiveData<Location>
        get() = _geoLoc

    fun setGeloc(location: Location) {
        _geoLoc.value = location
    }
}