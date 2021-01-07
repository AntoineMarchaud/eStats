package com.amarchaud.estats.service

import android.Manifest
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.amarchaud.estats.R
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PositionService : Service() {

    companion object {
        const val TAG = "PositionService"
        const val CHANNEL_ID = "channelIdService"
        const val UPDATE_TIME = 1000L // in milli
    }

    @Inject
    lateinit var myDao: AppDao

    @Inject
    lateinit var myApp: Application

    private lateinit var sharedPref: SharedPreferences

    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    // ***************** Values for Clients ******************************* //
    var currentLocation: android.location.Location? = null
        private set

    var matchingLocationWithSubs: LocationWithSubs? = null
        private set

    var matchingLocation: LocationInfo? = null
        private set

    var matchingSubLocation: LocationInfoSub? = null
        private set
    // ***************** Values for Clients end *************************** //

    /**
     * Manage Binding with Client
     */
    // Binder given to clients
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): PositionService = this@PositionService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    // End


    /**
     * Manage Service itself
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sharedPref = myApp.getSharedPreferences(
            getString(com.amarchaud.estats.R.string.shared_pref),
            Context.MODE_PRIVATE
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mLocationRequest = LocationRequest()
            mLocationRequest.interval = UPDATE_TIME
            mLocationRequest.fastestInterval = UPDATE_TIME
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        // If we get killed, after returning from here, restart
        return START_STICKY
    }


    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            Log.d(TAG, "New Location")
            for (location in locationResult.locations) {

                // update currentLocation for client side
                currentLocation = location
                with(sharedPref.edit()) {
                    putLong(
                        getString(R.string.saved_location_lat),
                        java.lang.Double.doubleToRawLongBits(location.latitude)
                    )
                    putLong(
                        getString(R.string.saved_location_lon),
                        java.lang.Double.doubleToRawLongBits(location.longitude)
                    )
                    apply()
                }

                Log.d(TAG, "My Location : ${location.latitude} - ${location.longitude}")


                GlobalScope.launch {
                    val bestLoc = myDao.getBetterLocation(
                        location.latitude,
                        location.longitude
                    )
                    val bestSubLoc = myDao.getBetterSubLocation(
                        location.latitude,
                        location.longitude
                    )
                    val bestLocWithSub = myDao.getBetterLocationWithSubs(
                        location.latitude,
                        location.longitude
                    )

                    bestLocWithSub?.let {

                        val lastSave = sharedPref.getLong(
                            getString(R.string.saved_current_time_ms),
                            System.currentTimeMillis()
                        )
                        val inc = System.currentTimeMillis() - lastSave

                        bestLoc?.let { locationInfo ->
                            locationInfo.duration_day += inc
                            myDao.update(locationInfo)
                        }
                        bestSubLoc?.let { locationSubInfo ->
                            locationSubInfo.duration_day += inc
                            myDao.update(locationSubInfo)
                        }

                        Log.d(TAG, "Matching Location : ${it.locationInfo.name}")
                        matchingLocationWithSubs = it

                        with(sharedPref.edit()) {
                            putLong(
                                getString(R.string.saved_current_time_ms),
                                System.currentTimeMillis()
                            )
                            apply()
                        }
                    }

                    bestLoc?.let {
                        matchingLocation = it
                    }

                    bestSubLoc?.let {
                        matchingSubLocation = it
                    }
                }
            }
        }

        private fun createFusedLocation() {

        }
    }
}