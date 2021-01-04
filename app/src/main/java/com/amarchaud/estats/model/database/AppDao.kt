package com.amarchaud.estats.model.database

import androidx.room.Dao
import androidx.room.Insert
import com.amarchaud.estats.model.entity.LocationInfo

@Dao
interface AppDao {
    @Insert
    fun insertUser(locationInfo: LocationInfo)
}