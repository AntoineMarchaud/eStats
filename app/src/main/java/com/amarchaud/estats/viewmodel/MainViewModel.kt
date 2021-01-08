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
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.service.PositionService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class MainViewModel @ViewModelInject constructor(
    val app: Application,
    private val myDao: AppDao, // injected by hilt
) : BaseViewModel(app) {

    companion object {
        const val TAG = "MainViewModel"
        const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss"

        enum class typeItem {
            ITEM_INSERTED,
            ITEM_DELETED
        }

        enum class typeHeaderItem {
            ITEM_INSERTED,
            ITEM_MODIFIED,
            ITEM_DELETED
        }

        enum class typeSubItem {
            ITEM_INSERTED,
            ITEM_MODIFIED,
            ITEM_DELETED
        }
    }

    private var mPositionService: PositionService? = null
    private var bound: Boolean = false

    // Bindable properties ***************************************************************
    @Bindable
    var currentDate: String? = null

    @Bindable
    var matchingLocation: LocationInfo? = null

    @Bindable
    var matchingSubLocation: LocationInfoSub? = null

    // LiveData properties ***************************************************************
    val myGeoLoc: MutableLiveData<Location> = MutableLiveData()

    private var listOfLocationWithSubs: MutableList<LocationWithSubs> = mutableListOf()

    // we can emit 3 different datas
    val oneLocation: MutableLiveData<Triple<LocationInfo, typeHeaderItem, Int>> = MutableLiveData()
    val oneSubLocation: MutableLiveData<Triple<LocationInfoSub, typeSubItem, Pair<Int, Int>>> = MutableLiveData()
    val oneLocationWithSub: MutableLiveData<Triple<LocationWithSubs, typeItem, Int>> = MutableLiveData()

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

                    mPositionService?.let { positionService ->

                        // update view
                        matchingLocation = positionService.matchingLocation
                        notifyPropertyChanged(BR.matchingLocation)

                        // si on trouve un matching location, il faut updater la liste
                        positionService.matchingLocation?.let { ml ->

                            // update list
                            val pos = listOfLocationWithSubs.indexOfFirst {
                                it.locationInfo.id == ml.id
                            }
                            oneLocation.postValue(Triple(ml, typeHeaderItem.ITEM_MODIFIED, pos))
                        }


                        // update view
                        matchingSubLocation = positionService.matchingSubLocation
                        notifyPropertyChanged(BR.matchingSubLocation)

                        // si on trouve un matching location, il faut updater la liste
                        positionService.matchingSubLocation?.let { subloc ->

                            // update list
                            val pos = listOfLocationWithSubs.indexOfFirst {
                                it.locationInfo.id == subloc.idMain
                            }

                            val posSub = listOfLocationWithSubs[pos].subLocation.indexOfFirst {
                                it.idSub == subloc.idSub
                            }
                            oneSubLocation.postValue(Triple(subloc, typeSubItem.ITEM_MODIFIED, Pair(pos, posSub)))
                        }


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
                listOfLocationWithSubs.add(it)
                oneLocationWithSub.value = Triple(it, typeItem.ITEM_INSERTED, listOfLocationWithSubs.size - 1)
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
            mPositionService?.let {
                it.geoLoc?.let { location ->
                    // just say to Fragment to display
                    popupAddCurrentPosition.postValue(location)
                }
            }
        }
    }

    /**
     * Callback of pop-up
     */
    // TODO : add delta !
    fun onCurrentLocationDialogPositiveClick(lat: Double, lon: Double, nameChoosen: String, locationInfo: LocationInfo?) {

        // if locationInfo is null, it is a new Location
        if (locationInfo == null) {
            val locationInfoInserted = LocationInfo(
                name = nameChoosen,
                lat = lat,
                lon = lon
            )

            // add to Database
            viewModelScope.launch {
                myDao.insert(locationInfoInserted)

                val ls = myDao.getLastInsertedLocationWithSubs()
                Log.d(TAG, "User add new location ${ls.locationInfo.name} id ${ls.locationInfo.id}")

                listOfLocationWithSubs.add(ls)
                oneLocation.postValue(Triple(ls.locationInfo, typeHeaderItem.ITEM_INSERTED, listOfLocationWithSubs.size - 1))
            }
        } else { // it is a new sub location to add !

            val locationSub = LocationInfoSub(
                name = nameChoosen,
                lat = lat,
                lon = lon,
                idMain = locationInfo.id
            )

            // add to Database
            viewModelScope.launch {
                myDao.insert(locationSub)

                // get updated element in dao
                val ls = myDao.getLastInsertedSubLocation()

                // update current LocationInfo in the list
                val mainIndex = listOfLocationWithSubs.indexOfFirst {
                    it.locationInfo.id == locationInfo.id
                }


                listOfLocationWithSubs[mainIndex].subLocation.add(ls)
                oneSubLocation.postValue(Triple(ls, typeSubItem.ITEM_INSERTED, Pair(mainIndex, listOfLocationWithSubs[mainIndex].subLocation.size - 1)))
            }

        }
    }
}