package com.amarchaud.estats.interfaces

import com.amarchaud.estats.model.entity.LocationInfo

interface ILocationInfoClickListener {
    fun onLocationInfoClicked(locationInfo: LocationInfo)
}