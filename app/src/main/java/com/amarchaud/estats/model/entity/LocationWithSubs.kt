package com.amarchaud.estats.model.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub

/**
 * Un Location associé à tous ses LocationInfoSub
 * Utilisable aussi bien dans la DAO / ViewModel / View / Adapter
 */
data class LocationWithSubs(
    @Embedded val locationInfo: LocationInfo,
    @Relation(
        parentColumn = "id",
        entityColumn = "idMain"
    ) val subLocation: List<LocationInfoSub>
)