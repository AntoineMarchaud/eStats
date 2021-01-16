package com.amarchaud.estats.viewmodel.data

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amarchaud.estats.base.SingleLiveEvent

class GeoPointViewModel : ViewModel() {
    val geoLoc = MutableLiveData<Location>()
}