package com.amarchaud.estats.model.database

import androidx.compose.runtime.emit
import androidx.room.*
import com.amarchaud.estats.model.OneLocationModel
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.utils.Distance
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.flow


@Dao
interface AppDao {

    /// KOROUTINES !

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationInfoCoroutine(locationInfo: LocationInfo)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCoroutine(locationInfo: LocationInfo)

    @Query("SELECT * from Locations ORDER BY id ASC")
    suspend fun getAllLocationsCoroutine(): List<LocationInfo>

    @Query("SELECT * from SubLocations WHERE idMain==:id ORDER BY idMain ASC")
    suspend fun getAllSubLocationsOfCoroutine(id: Int): List<LocationInfoSub>

    @Query("SELECT * from Locations WHERE id==:id LIMIT 1")
    suspend fun getOneLocationCoroutine(id: Int): LocationInfo

    private suspend fun makeLocationModel(locationInfo: LocationInfo?): OneLocationModel? {
        if (locationInfo == null)
            return null

        return OneLocationModel(locationInfo, getAllSubLocationsOfCoroutine(locationInfo.id))
    }

    suspend fun getOneLocationAndSubLocCoroutine(id: Int): OneLocationModel? {
        val l: LocationInfo = getOneLocationCoroutine(id)
        return makeLocationModel(l)
    }

    suspend fun getAllLocationsAndSubLocOnNextCouroutine() = flow {
        getAllLocationsCoroutine().forEach {
            emit(makeLocationModel(it))
        }
    }

    /**
     * Retourne le dernier element inséré
     */
    suspend fun getLastInsertedLocationCoroutine(): LocationInfo {
        return getAllLocationsCoroutine().last()
    }


    /**
     * Find the closest location from my position
     */
    private suspend fun getClosestLocationCoroutine(
        latitude: Double,
        longitude: Double
    ): LocationInfo? {
        return getAllLocationsCoroutine().minByOrNull {
            Distance.measure(
                it.lat,
                it.lon,
                latitude,
                longitude
            )
        }
    }

    /**
     * Find all location where I am inside (generally only one)
     */
    private suspend fun getInsideLocationsCoroutine(
        latitude: Double,
        longitude: Double
    ): List<LocationInfo> {
        return getAllLocationsCoroutine().mapNotNull {
            if (Distance.measure(it.lat, it.lon, latitude, longitude) < it.delta)
                it
            else
                null
        }
    }

    /**
     * Find the correction location
     */
    suspend fun getBetterLocationCoroutine(
        latitude: Double,
        longitude: Double
    ): OneLocationModel? {
        return makeLocationModel(
            getInsideLocationsCoroutine(latitude, longitude).minByOrNull {
                Distance.measure(it.lat, it.lon, latitude, longitude)
            })
    }
}