package com.amarchaud.estats.model.database

import androidx.room.*
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.utils.Distance


@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationInfo: LocationInfo)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(locationInfo: LocationInfo)

    @Query("SELECT * from Locations ORDER BY id ASC")
    suspend fun getAllLocations(): List<LocationInfo>

    @Query("SELECT * from SubLocations WHERE idMain==:id ORDER BY idMain ASC")
    suspend fun getAllSubLocationsOf(id: Int): List<LocationInfoSub>

    @Query("SELECT * from Locations WHERE id==:id LIMIT 1")
    suspend fun getOneLocation(id: Int): LocationInfo

    // Join tables !
    @Transaction
    @Query("SELECT * FROM Locations ORDER BY id ASC")
    suspend fun getAllLocationsWithSubs(): List<LocationWithSubs>

    @Transaction
    @Query("SELECT * FROM Locations WHERE id=(SELECT MAX(id) FROM Locations) LIMIT 1")
    suspend fun getLastInsertedLocationWithSubs(): LocationWithSubs

    @Transaction
    @Query("SELECT * from Locations WHERE id==:id LIMIT 1")
    suspend fun getOneLocationWithSubs(id: Int): LocationWithSubs


    /**
     * Find all location where I am inside (generally only one)
     */
    private suspend fun getInsideLocationsWithSubs(
        latitude: Double,
        longitude: Double
    ): List<LocationWithSubs> {
        return getAllLocationsWithSubs().mapNotNull {
            if (Distance.measure(
                    it.locationInfo.lat,
                    it.locationInfo.lon,
                    latitude,
                    longitude
                ) < it.locationInfo.delta
            )
                it
            else
                null
        }
    }

    /**
     * Find the correction location
     */
    suspend fun getBetterLocationWithSubs(
        latitude: Double,
        longitude: Double
    ): LocationWithSubs? {
        return getInsideLocationsWithSubs(latitude, longitude).minByOrNull {
            Distance.measure(it.locationInfo.lat, it.locationInfo.lon, latitude, longitude)
        }
    }
}