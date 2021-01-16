package com.amarchaud.estats.model.database

import androidx.room.*
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.utils.Distance


@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationInfo: LocationInfo) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationInfoSub: LocationInfoSub)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(locationInfo: LocationInfo)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(locationInfoSub: LocationInfoSub)

    enum class DurationType(val value: Int) {
        DURATION_DAY(0), DURATION_WEEK(1), DURATION_MONTH(2), DURATION_YEAR(3), DURATION_ALL_TIME(4)
    }

    suspend fun resetLocationDuration(locationWithSubs: LocationWithSubs, durationType: DurationType) {

        locationWithSubs.subLocation.forEach {
            it.apply {
                if (durationType >= DurationType.DURATION_DAY) {
                    duration_day = 0
                    if (durationType >= DurationType.DURATION_WEEK) {
                        duration_week = 0
                        if (durationType >= DurationType.DURATION_MONTH) {
                            duration_month = 0
                            if (durationType >= DurationType.DURATION_YEAR) {
                                duration_year = 0
                                if (durationType == DurationType.DURATION_ALL_TIME) {
                                    duration_all_time = 0
                                }
                            }
                        }
                    }
                }
                update(this)
            }
        }

        locationWithSubs.locationInfo.apply {
            if (durationType >= DurationType.DURATION_DAY) {
                duration_day = 0
                if (durationType >= DurationType.DURATION_WEEK) {
                    duration_week = 0
                    if (durationType >= DurationType.DURATION_WEEK) {
                        duration_month = 0
                        if (durationType >= DurationType.DURATION_YEAR) {
                            duration_year = 0
                            if (durationType == DurationType.DURATION_ALL_TIME) {
                                duration_all_time = 0
                            }
                        }
                    }
                }
            }
            update(this)
        }
    }

    suspend fun updateLocationDuration(id: Int, inc: Long = 1000) {
        getOneLocation(id).apply {
            duration_day += inc
            duration_week += inc
            duration_month += inc
            duration_year += inc
            duration_all_time += inc
            update(this)
        }
    }

    @Transaction
    suspend fun updateSubLocationDuration(idSub: Int, inc: Long = 1000) {
        getOneSubLocation(idSub).apply {
            duration_day += inc
            duration_week += inc
            duration_month += inc
            duration_year += inc
            duration_all_time += inc
            update(this)
        }
    }

    @Transaction
    suspend fun delete(toDelete: LocationWithSubs) {
        toDelete.subLocation.forEach {
            delete(it)
        }
        delete(toDelete.locationInfo)
    }

    @Delete
    suspend fun delete(toDelete: LocationInfo)

    @Delete
    suspend fun delete(toDelete: LocationInfoSub)


    // Location
    @Transaction
    @Query("SELECT * FROM Locations ORDER BY id ASC")
    suspend fun getAllLocations(): List<LocationInfo>

    @Transaction
    @Query("SELECT * FROM Locations WHERE id=(SELECT MAX(id) FROM Locations) LIMIT 1")
    suspend fun getLastInsertedLocation(): LocationInfo

    @Transaction
    @Query("SELECT * from Locations WHERE id==:id LIMIT 1")
    suspend fun getOneLocation(id: Int): LocationInfo

    // SubLocation
    @Transaction
    @Query("SELECT * FROM SubLocations ORDER BY idSub ASC")
    suspend fun getAllSubLocations(): List<LocationInfoSub>

    @Transaction
    @Query("SELECT * FROM SubLocations WHERE idSub=(SELECT MAX(idSub) FROM SubLocations) LIMIT 1")
    suspend fun getLastInsertedSubLocation(): LocationInfoSub

    @Transaction
    @Query("SELECT * from SubLocations WHERE idSub==:id LIMIT 1")
    suspend fun getOneSubLocation(id: Int): LocationInfoSub

    // LocationWithSub
    @Transaction
    @Query("SELECT * FROM Locations ORDER BY id ASC")
    suspend fun getAllLocationsWithSubs(): List<LocationWithSubs>

    @Transaction
    @Query("SELECT * from Locations WHERE id==:id LIMIT 1")
    suspend fun getOneLocationWithSubs(id: Long): LocationWithSubs

// Other methods

    /**
     * Find all location where I am inside (generally only one)
     */
    private suspend fun getInsideLocations(
        latitude: Double,
        longitude: Double
    ): List<LocationInfo> {
        return getAllLocations().mapNotNull {
            if (Distance.measure(
                    it.lat,
                    it.lon,
                    latitude,
                    longitude
                ) < it.delta
            )
                it
            else
                null
        }
    }

    /**
     * Find the correction location
     */
    suspend fun getBetterLocation(
        latitude: Double,
        longitude: Double
    ): LocationInfo? {
        return getInsideLocations(latitude, longitude).minByOrNull {
            Distance.measure(it.lat, it.lon, latitude, longitude)
        }
    }

    /**
     * Find all location where I am inside (generally only one)
     */
    private suspend fun getInsideSubLocations(
        latitude: Double,
        longitude: Double
    ): List<LocationInfoSub> {
        return getAllSubLocations().mapNotNull {
            if (Distance.measure(
                    it.lat,
                    it.lon,
                    latitude,
                    longitude
                ) < it.delta
            )
                it
            else
                null
        }
    }

    /**
     * Find the correction location
     */
    suspend fun getBetterSubLocation(
        latitude: Double,
        longitude: Double
    ): LocationInfoSub? {
        return getInsideSubLocations(latitude, longitude).minByOrNull {
            Distance.measure(it.lat, it.lon, latitude, longitude)
        }
    }

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