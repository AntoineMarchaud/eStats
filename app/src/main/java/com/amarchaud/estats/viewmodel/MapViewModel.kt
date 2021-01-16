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
import androidx.fragment.app.activityViewModels
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.base.SingleLiveEvent
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.service.PositionService
import com.amarchaud.estats.viewmodel.data.NewPositionViewModel
import kotlinx.coroutines.launch

class MapViewModel @ViewModelInject constructor(
    val app: Application,
    private val myDao: AppDao, // injected by hilt
) : BaseViewModel(app) {

    companion object {
        const val TAG = "MapViewModel"
    }

    private var listOfLocationWithSubs: MutableList<LocationWithSubs> = mutableListOf()
    val allLocationsWithSub: MutableLiveData<List<LocationWithSubs>> = MutableLiveData()

    val oneLocationWithSub: SingleLiveEvent<LocationWithSubs> = SingleLiveEvent()

    val myGeoLoc: MutableLiveData<Location> = MutableLiveData()

    private var mPositionService: PositionService? = null
    private var bound: Boolean = false

    init {
        viewModelScope.launch {
            listOfLocationWithSubs = myDao.getAllLocationsWithSubs().toMutableList()
            allLocationsWithSub.postValue(listOfLocationWithSubs)
        }
    }

    private var mHandler: Handler? = null
    private var refreshDatasRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                mPositionService?.let { positionService ->
                    if (bound) {
                        if (positionService.geoLoc != null) {
                            myGeoLoc.postValue(positionService.geoLoc)
                        }
                    }
                }

            } finally {
                mHandler?.postDelayed(this, 1000)
            }
        }
    }

    private val mConnection = object : ServiceConnection {
        // Called when the connection with the service is established
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PositionService.LocalBinder
            mPositionService = binder.getService()
            bound = true
        }

        // Called when the connection with the service disconnects unexpectedly
        override fun onServiceDisconnected(className: ComponentName) {
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

    fun onAddNewPosition(lat: Double, lon: Double, nameChoosen: String, delta: Int) {
        val locationInfoInserted = LocationInfo(
            name = nameChoosen,
            lat = lat,
            lon = lon,
            delta = delta
        )

        // add to Database
        viewModelScope.launch {
            val id = myDao.insert(locationInfoInserted)
            val ls = myDao.getOneLocationWithSubs(id)
            Log.d(TAG, "User add new location ${ls.locationInfo.name} id ${ls.locationInfo.id}")

            listOfLocationWithSubs.add(ls)
            oneLocationWithSub.postValue(ls)
        }
    }
}