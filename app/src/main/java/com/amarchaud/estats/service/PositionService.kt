package com.amarchaud.estats.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.database.AppDao
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

    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    // ***************** Values for Clients ******************************* //
    var matchingLocation: LocationWithSubs? = null
        private set

    var currentLocation: android.location.Location? = null
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

                Log.d(TAG, "My Location : ${location.latitude} - ${location.longitude}")


                GlobalScope.launch {
                    val bestLoc = myDao.getBetterLocationWithSubs(location.latitude, location.longitude)
                    bestLoc?.let {
                        it.locationInfo.duration_day++
                        myDao.update(it.locationInfo)

                        Log.d(TAG, "Matching Location : ${it.locationInfo.name}")
                        matchingLocation = it
                    }
                }
            }
        }

        private fun createFusedLocation() {

        }
    }
}