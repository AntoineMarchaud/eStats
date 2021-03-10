package com.amarchaud.estats.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.amarchaud.estats.R
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.utils.TimeTransformation
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class PositionService : Service() {

    @Inject
    lateinit var myApp: Application

    @Inject
     lateinit var myDao: AppDao

    companion object {
        const val TAG = "PositionService"
        const val CHANNEL_ID = "channelIdService"
        const val ONGOING_NOTIFICATION_ID = 1
        const val UPDATE_TIME = 3000L // in milli
        const val ACTION_CLOSE_FOREGROUND = "ACTION_CLOSE_FOREGROUND"
    }


    private lateinit var sharedPref: SharedPreferences

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mLocationRequest: LocationRequest = LocationRequest.create()

    // ***************** Values for Clients ******************************* //
    var geoLoc: android.location.Location? = null
        private set

    //var matchingLocationWithSubs: LocationWithSubs? = null
    //   private set

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

    init {
        mLocationRequest.interval = UPDATE_TIME
        mLocationRequest.fastestInterval = UPDATE_TIME
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }


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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.getMainLooper()
            )
        }

        createNotificationChannel()
        createWorkerResetRequest()
    }

    private fun createWorkerResetRequest() {
        ResetDurationWorker.prepareNextReset(AppDao.DurationType.DURATION_DAY, this)
        ResetDurationWorker.prepareNextReset(AppDao.DurationType.DURATION_WEEK, this)
        ResetDurationWorker.prepareNextReset(AppDao.DurationType.DURATION_MONTH, this)
        ResetDurationWorker.prepareNextReset(AppDao.DurationType.DURATION_YEAR, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        if (ACTION_CLOSE_FOREGROUND == intent?.action) {
            stopForeground(true) // remove notification, but service continues to work
        }

        // If we get killed, service is over
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


    /**
     * Launch ForegroundService (the notification on the top of the screen)
     */
    fun createNotificationForegroundService() {
        // ********************* start notification of foreground service ***************************** //
        // display MainFragment quand on clique sur la notif
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.mainFragment)
            .createPendingIntent()

        //
        val stopSelf = Intent(this, PositionService::class.java)
        stopSelf.action = ACTION_CLOSE_FOREGROUND
        val stopSelfPendingIntent = PendingIntent.getService(this, 0, stopSelf, 0) // That you should change this part in your code

        val icon = IconCompat.createWithResource(this, R.drawable.ic_expand)
        val action: NotificationCompat.Action = NotificationCompat.Action.Builder(icon, getString(R.string.foregroundActionButton), stopSelfPendingIntent).build()

        val notification: Notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationCompat.Builder(this, CHANNEL_ID) else NotificationCompat.Builder(this))
            .setContentTitle(getString(R.string.foregroundTitle))
            .setContentText(getString(R.string.foregroundContext))
            .setSmallIcon(R.drawable.ic_collapse)
            .setContentIntent(pendingIntent)
            .addAction(action)
            .setSound(null)
            .build()
        startForeground(ONGOING_NOTIFICATION_ID, notification)
        // *********************************************************************
    }

    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            Log.d(TAG, "New Location")
            for (location in locationResult.locations) {

                // update currentLocation for client side
                geoLoc = location
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

                    // update matchingLocation
                    val bestLoc = myDao.getBetterLocation(location.latitude, location.longitude)?.also { locationInfo ->

                        val lastSave = sharedPref.getLong(
                            getString(R.string.saved_current_time_ms),
                            System.currentTimeMillis()
                        )
                        val inc = System.currentTimeMillis() - lastSave

                        myDao.updateLocationDuration(locationInfo.id, inc)
                        Log.d(TAG, "Matching Location : ${locationInfo.name ?: "none"}")

                        // Update matchingSubLocation
                        val bestSubLoc = myDao.getBetterSubLocation(
                            location.latitude,
                            location.longitude
                        )?.also { locationSubInfo ->
                            myDao.updateSubLocationDuration(locationSubInfo.idSub, inc)
                            Log.d(TAG, "Matching sub Location : ${locationSubInfo.name ?: "none"}")
                        }
                        matchingSubLocation = bestSubLoc
                    }

                    with(sharedPref.edit()) {
                        putLong(
                            getString(R.string.saved_current_time_ms),
                            System.currentTimeMillis()
                        )
                        apply()
                    }

                    matchingLocation = bestLoc
                    if (bestLoc == null)
                        matchingSubLocation = null
                }
            }
        }

        private fun createFusedLocation() {

        }
    }
}