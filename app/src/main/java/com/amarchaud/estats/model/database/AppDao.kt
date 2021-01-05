package com.amarchaud.estats.model.database

import androidx.room.*
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.utils.Distance


@Dao
interface AppDao {

    enum class Type {
        DAY, WEEK, MONTH, YEAR, ALL_TIME
    }

    @Insert
    fun insertUser(locationInfo: LocationInfo)

    @Update
    fun update(locationInfo: LocationInfo)

    @Query("UPDATE locations SET duration_day = (duration_day + 1) AND duration_week = (duration_week + 1) AND duration_month = (duration_month + 1) AND duration_year = (duration_year + 1 ) AND duration_all_time = (duration_all_time + 1) WHERE id == :id")
    fun incrementAllDurations(id: Int)

    @Query("UPDATE locations SET duration_day = 0 WHERE id == :id")
    fun resetDayDuration(id: Int)

    @Query("UPDATE locations SET duration_week = 0 WHERE id == :id")
    fun resetWeekDuration(id: Int)

    @Query("UPDATE locations SET duration_month = 0 WHERE id == :id")
    fun resetMonthDuration(id: Int)

    @Query("UPDATE locations SET duration_year = 0 WHERE id == :id")
    fun resetYearDuration(id: Int)

    @Query("UPDATE locations SET duration_all_time = 0 WHERE id == :id")
    fun resetAllTimeDuration(id: Int)

    @RawQuery
    fun resetTimeByTime(id: Int, type: Type) {
        when (type) {
            Type.DAY -> resetDayDuration(id)
            Type.WEEK -> resetWeekDuration(id)
            Type.MONTH -> resetMonthDuration(id)
            Type.YEAR -> resetYearDuration(id)
            Type.ALL_TIME -> resetAllTimeDuration(id)
        }
    }

    @Query("SELECT * from Locations ORDER BY id ASC")
    fun getAllLocations(): List<LocationInfo>

    /**
     * Find the closest location from my position
     */
    @RawQuery
    fun getClosestLocation(latitude: Double, longitude: Double): LocationInfo? {
        return getAllLocations().minByOrNull {
            Distance.measure(it.lat, it.lon, latitude, longitude)
        }
    }

    /**
     * Find all location where I am inside (generally only one)
     */
    @RawQuery
    fun getInsideLocations(latitude: Double, longitude: Double): List<LocationInfo>? {
        return getAllLocations().filter {
            Distance.measure(it.lat, it.lon, latitude, longitude) < it.delta
        }
    }

    /**
     * Find the correction location
     */
    @RawQuery
    fun getBetterLocation(latitude: Double, longitude: Double): LocationInfo? {
        return getInsideLocations(latitude, longitude)?.minByOrNull {
            Distance.measure(it.lat, it.lon, latitude, longitude)
        }
    }
}