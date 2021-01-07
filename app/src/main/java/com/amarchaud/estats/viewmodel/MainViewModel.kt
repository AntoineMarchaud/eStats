package com.amarchaud.estats.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.databinding.Bindable
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.estats.BR
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.base.SingleLiveEvent
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.service.PositionService
import com.amarchaud.estats.model.entity.LocationWithSubs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class MainViewModel @ViewModelInject constructor(
    val app: Application,
    private val myDao: AppDao // injected by hilt
) : BaseViewModel(app) {

    companion object {
        const val TAG = "MainViewModel"
        const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss"

        enum class typeItem {
            ITEM_INSERTED,
            ITEM_MODIFIED,
            ITEM_DELETED
        }
    }

    private var mService: PositionService? = null
    private var bound: Boolean = false

    // Bindable properties ***************************************************************
    @Bindable
    var currentDate: String? = null

    @Bindable
    var matchingLocation: LocationWithSubs? = null

    // LiveData properties ***************************************************************
    val myGeoLoc: MutableLiveData<Location> = MutableLiveData()

    private var listOfLocation: MutableList<LocationWithSubs> = mutableListOf()
    val oneLocation: MutableLiveData<Triple<LocationWithSubs, typeItem, Int>> =
        MutableLiveData()
    val popupAddCurrentPosition: SingleLiveEvent<Location> = SingleLiveEvent()

    private var mHandler: Handler? = null
    private var refreshDatasRunnable: Runnable = object : Runnable {
        override fun run() {
            try {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    currentDate = DateTimeFormatter
                        .ofPattern(DATE_FORMAT)
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now())
                } else {
                    val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                    val date = Date()
                    currentDate = formatter.format(date)
                }
                notifyPropertyChanged(BR.currentDate)

                if (bound) {

                    mService?.let { positionService ->

                        matchingLocation = positionService.matchingLocation
                        notifyPropertyChanged(BR.matchingLocation)

                        // si on trouve un matching location, il faut updater la liste
                        positionService.matchingLocation?.let { ml ->

                            val pos = listOfLocation.indexOfFirst {
                                it.locationInfo.id == matchingLocation?.locationInfo?.id
                            }

                            Log.d(TAG, "modify location $matchingLocation at position $pos")
                            oneLocation.postValue(
                                Triple(
                                    matchingLocation!!, typeItem.ITEM_MODIFIED, pos
                                )
                            )
                        }

                        if (positionService.currentLocation != null) {
                            myGeoLoc.postValue(positionService.currentLocation)
                        }
                    }
                }

            } finally {
                mHandler?.postDelayed(this, 1000)
            }
        }
    }


    init {
        // start service if needed
        Intent(app, PositionService::class.java).also { intent ->
            app.startService(intent)
        }

        /**
         * Add all at startup
         */
        viewModelScope.launch {
            myDao.getAllLocationsWithSubs().forEach {
                Log.d(TAG, "Init Add location : $it")
                listOfLocation.add(it)
                oneLocation.value = (Triple(it, typeItem.ITEM_INSERTED, listOfLocation.size - 1))
            }
        }
    }


    private val mConnection = object : ServiceConnection {
        // Called when the connection with the service is established
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PositionService.LocalBinder
            mService = binder.getService()
            bound = true
        }

        // Called when the connection with the service disconnects unexpectedly
        override fun onServiceDisconnected(className: ComponentName) {
            Log.e(TAG, "onServiceDisconnected")
            bound = false
        }
    }

    fun onResume() {
        mHandler = Looper.myLooper()?.let { Handler(it) }
        refreshDatasRunnable.run()

        if (!bound) {
            Intent(app, PositionService::class.java).also { intent ->
                app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun onPause() {
        mHandler?.removeCallbacks(refreshDatasRunnable)
        mHandler = null

        if (bound) {
            app.unbindService(mConnection)
            bound = false
        }
    }

    /**
     * Call when user want to add a custom position
     */
    fun onAddCustomPosition(v: View) {
        println("onAddCustomPosition")
    }

    /**
     * Called when user want to add his current position
     */
    fun onAddCurrentPosition(v: View) {

        if (bound) {
            mService?.let {
                it.currentLocation?.let { location ->
                    // just say to Fragment to display
                    popupAddCurrentPosition.postValue(location)
                }
            }
        }
    }

    /**
     * Callback of pop-up
     */
    fun onCurrentLocationDialogPositiveClick(
        currentLocation: Location,
        nameChoosen: String
    ) {
        val locationInfoInserted = LocationInfo(
            name = nameChoosen,
            lat = currentLocation.latitude,
            lon = currentLocation.longitude
        )

        // add to Database
        viewModelScope.launch {
            myDao.insert(locationInfoInserted)

            val ls  = myDao.getLastInsertedLocationWithSubs()
            Log.d(TAG, "User add new location ${ls.locationInfo.name} id ${ls.locationInfo.id}")

            listOfLocation.add(ls)
            oneLocation.postValue(
                Triple(
                    ls,
                    typeItem.ITEM_INSERTED,
                    listOfLocation.size - 1
                )
            )
        }
    }
}