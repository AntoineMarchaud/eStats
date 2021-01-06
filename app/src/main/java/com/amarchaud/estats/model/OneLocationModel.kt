package com.amarchaud.estats.model

import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub

/**
 * Un Location associé à tous ses LocationInfoSub
 * Utilisable aussi bien dans la DAO / ViewModel / View / Adapter
 */
class OneLocationModel(
    val locationInfo: LocationInfo,
    val subLocation: List<LocationInfoSub>
) {
    constructor(p: Pair<LocationInfo, List<LocationInfoSub>>) : this(p.first, p.second)
}