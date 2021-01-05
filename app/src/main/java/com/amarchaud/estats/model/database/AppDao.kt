package com.amarchaud.estats.model.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.utils.Distance
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers


@Dao
interface AppDao {

    enum class Type {
        DAY, WEEK, MONTH, YEAR, ALL_TIME
    }

    @Insert
    fun insertUser(locationInfo: LocationInfo): Completable

    @Update
    fun update(locationInfo: LocationInfo): Completable

    @Query("UPDATE locations SET duration_day = (duration_day + 1) AND duration_week = (duration_week + 1) AND duration_month = (duration_month + 1) AND duration_year = (duration_year + 1 ) AND duration_all_time = (duration_all_time + 1) WHERE id == :id")
    fun incrementAllDurations(id: Int): Completable

    @Query("UPDATE locations SET duration_day = 0 WHERE id == :id")
    fun resetDayDuration(id: Int): Completable

    @Query("UPDATE locations SET duration_week = 0 WHERE id == :id")
    fun resetWeekDuration(id: Int): Completable

    @Query("UPDATE locations SET duration_month = 0 WHERE id == :id")
    fun resetMonthDuration(id: Int): Completable

    @Query("UPDATE locations SET duration_year = 0 WHERE id == :id")
    fun resetYearDuration(id: Int): Completable

    @Query("UPDATE locations SET duration_all_time = 0 WHERE id == :id")
    fun resetAllTimeDuration(id: Int): Completable

    @RawQuery
    fun resetTimeByTime(id: Int, type: Type): Completable {
        return when (type) {
            Type.DAY -> resetDayDuration(id)
            Type.WEEK -> resetWeekDuration(id)
            Type.MONTH -> resetMonthDuration(id)
            Type.YEAR -> resetYearDuration(id)
            Type.ALL_TIME -> resetAllTimeDuration(id)
        }
    }

    @Query("SELECT * from Locations ORDER BY id ASC")
    fun getAllLocations(): Flowable<List<LocationInfo>>

    /**
     * Find the closest location from my position
     */
    @RawQuery
    fun getClosestLocation(latitude: Double, longitude: Double): Flowable<LocationInfo> {

        return getAllLocations()
            .flatMap { locationInfos: List<LocationInfo> ->

                val min = locationInfos.minByOrNull {
                    Distance.measure(it.lat, it.lon, latitude, longitude)
                }

                if (min == null) {
                    Flowable.just(LocationInfo())
                } else {
                    Flowable.just(min)
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Find all location where I am inside (generally only one)
     */
    @RawQuery
    fun getInsideLocations(latitude: Double, longitude: Double): Flowable<List<LocationInfo>> {

        return getAllLocations()
            .flatMap { locationInfos: List<LocationInfo> ->
                Flowable.just(locationInfos.filter {
                    Distance.measure(it.lat, it.lon, latitude, longitude) < it.delta
                })
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Find the correction location
     */
    @RawQuery
    fun getBetterLocation(latitude: Double, longitude: Double): Flowable<LocationInfo> {

        return getInsideLocations(latitude, longitude)
            .flatMap { locationInfos: List<LocationInfo> ->
                val min = locationInfos.minByOrNull {
                    Distance.measure(it.lat, it.lon, latitude, longitude)
                }
                if (min == null) {
                    Flowable.just(LocationInfo())
                } else {
                    Flowable.just(min)
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
    }
}