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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.other.Contact
import com.amarchaud.estats.service.PositionService
import com.amarchaud.estats.utils.GeoCoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    val app: Application,
    private val myDao: AppDao // injected by hilt
) : AndroidViewModel(app) {

    companion object {
        const val TAG = "MainViewModel"
        const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss"
    }

    var mPositionService: PositionService? = null
    private var bound: Boolean = false

    // Bindable properties ***************************************************************
    private var _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String>
        get() = _currentDate

    private var _matchingLocation = MutableLiveData<LocationInfo?>()
    val matchingLocation: LiveData<LocationInfo?>
        get() = _matchingLocation

    private var _matchingSubLocation = MutableLiveData<LocationInfoSub?>()
    val matchingSubLocation: LiveData<LocationInfoSub?>
        get() = _matchingSubLocation


    // LiveData properties ***************************************************************
    private var _myGeoLoc = MutableLiveData<Location>()
    val myGeoLoc: LiveData<Location>
        get() = _myGeoLoc

    // saved all elements in that list
    private var _allLocationsWithSub = MutableLiveData<MutableList<LocationWithSubs>>()
    val allLocationsWithSub: LiveData<MutableList<LocationWithSubs>>
        get() = _allLocationsWithSub

    // ------------
    // element to modify
    private var _oneLocationToModify = MutableLiveData<Pair<LocationInfo, Int>>()
    val oneLocationToModify: LiveData<Pair<LocationInfo, Int>>
        get() = _oneLocationToModify

    private var _oneSubLocationToModify = MutableLiveData<Triple<LocationInfoSub, Int, Int>>()
    val oneSubLocationToModify: LiveData<Triple<LocationInfoSub, Int, Int>>
        get() = _oneSubLocationToModify
    // ----------------


    private var mHandler: Handler? = null
    private var refreshDatasRunnable: Runnable = object : Runnable {
        override fun run() {
            try {

                _currentDate.postValue(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        DateTimeFormatter
                            .ofPattern(DATE_FORMAT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())
                    } else {
                        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                        val date = Date()
                        formatter.format(date)
                    }
                )

                if (bound) {

                    mPositionService?.let { positionService ->

                        // update view
                        _matchingLocation.postValue(positionService.matchingLocation)

                        // si on trouve un matching location, il faut updater la liste
                        positionService.matchingLocation?.let { ml ->

                            // update list
                            _allLocationsWithSub.value?.indexOfFirst {
                                it.locationInfo.id == ml.id
                            }?.let { pos ->
                                if (pos >= 0) {
                                    _oneLocationToModify.postValue(Pair(ml, pos))
                                } else {
                                    _matchingLocation.postValue(null)
                                }
                            }
                        }


                        // update view
                        _matchingSubLocation.postValue(positionService.matchingSubLocation)

                        // si on trouve un matching location, il faut updater la liste
                        positionService.matchingSubLocation?.let { subloc ->

                            // update list
                            _allLocationsWithSub.value?.indexOfFirst {
                                it.locationInfo.id == subloc.idMain
                            }?.let { pos ->
                                if (pos >= 0) {

                                    val posSub = _allLocationsWithSub.value!![pos].subLocation.indexOfFirst {
                                        it.idSub == subloc.idSub
                                    }
                                    if (posSub >= 0) {
                                        _oneSubLocationToModify.postValue(Triple(subloc, pos, posSub))
                                    }
                                } else {
                                    _matchingSubLocation.postValue(null)
                                }
                            }


                        }


                        if (positionService.geoLoc != null) {
                            _myGeoLoc.postValue(positionService.geoLoc)
                        }
                    }
                }

            } finally {
                mHandler?.postDelayed(this, PositionService.UPDATE_TIME)
            }
        }
    }


    init {
        // start service if needed
        Intent(app, PositionService::class.java).also { intent ->
            app.startService(intent)
        }

        /**
         * Add all at startup or onSavedInstance/onRestoreInstance
         */
        viewModelScope.launch {
            _allLocationsWithSub.postValue(myDao.getAllLocationsWithSubs().toMutableList())
        }
    }


    private val mConnection = object : ServiceConnection {
        // Called when the connection with the service is established
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PositionService.LocalBinder
            mPositionService = binder.getService()
            mPositionService?.createNotificationForegroundService()
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

    suspend fun onAddNewPosition(lat: Double, lon: Double, nameChoosen: String, delta: Int) = flow {

        val locationInfoInserted = LocationInfo(name = nameChoosen, lat = lat, lon = lon, delta = delta)
        val id = myDao.insert(locationInfoInserted)
        val ls = myDao.getOneLocationWithSubs(id)
        Log.d(TAG, "User add new location ${ls.locationInfo.name} id ${ls.locationInfo.id}")

        _allLocationsWithSub.value?.add(ls)
        emit(ls)
    }

    suspend fun onAddNewPositionSub(lat: Double, lon: Double, nameChoosen: String, delta: Int, idMain: Int) = flow {
        val locationSub = LocationInfoSub(
            name = nameChoosen,
            lat = lat,
            lon = lon,
            idMain = idMain,
            delta = delta
        )

        // add to Database
        myDao.insert(locationSub)

        // get updated element in dao
        val ls = myDao.getLastInsertedSubLocation()

        // update current LocationInfo in the list
        _allLocationsWithSub.value?.indexOfFirst {
            it.locationInfo.id == idMain
        }?.let { mainIndex ->
            _allLocationsWithSub.value!![mainIndex].subLocation.add(ls)
            emit(Pair(ls, mainIndex))
        }
    }

    suspend fun deleteItem(locationToDelete: LocationInfo) {

        _allLocationsWithSub.value?.indexOfFirst {
            it.locationInfo.id == locationToDelete.id
        }?.let { position ->

            _allLocationsWithSub.value!![position].let {

                // remove from db
                myDao.delete(it)

                // put matching to null if it was the same !!
                matchingLocation.value?.apply {
                    if (id == locationToDelete.id) {
                        _matchingLocation.postValue(null)
                        _matchingSubLocation.postValue(null)
                    }
                }

                // remove from local list
                _allLocationsWithSub.value!!.removeAt(position)
            }
        }
    }

    suspend fun deleteSubItem(subLocationToDelete: LocationInfoSub) = flow {

        _allLocationsWithSub.value?.indexOfFirst {
            it.locationInfo.id == subLocationToDelete.idMain
        }?.let { mainPosition ->
            if (mainPosition >= 0) {
                _allLocationsWithSub.value!![mainPosition].let { locationWithSubs ->

                    locationWithSubs.subLocation.indexOfFirst {
                        it.idSub == subLocationToDelete.idSub
                    }.let { positionSub ->

                        if (positionSub >= 0) {

                            // remove from db
                            myDao.delete(subLocationToDelete)

                            // put matching to null if it was the same !!
                            matchingSubLocation.value?.apply {
                                if (idSub == subLocationToDelete.idSub) {
                                    _matchingSubLocation.postValue(null)
                                }
                            }

                            // remove from local list
                            locationWithSubs.subLocation.removeAt(positionSub)

                            emit(Pair(mainPosition, positionSub))
                        }

                    }
                }
            }
        }
    }

    suspend fun onAddContacts(allContacts: ArrayList<Contact>) = flow {

        allContacts.forEach { oneContact ->

            val name = oneContact.name
            val addr = oneContact.addr


            // convert addr to GeoLoc !
            val geoLoc = GeoCoder.getLocationFromAddressSuspend(addr, app)
            geoLoc?.let {

                val locationInfoInserted = LocationInfo(
                    name = name,
                    lat = it.latitude,
                    lon = it.longitude
                )

                // add to Database
                val id = myDao.insert(locationInfoInserted)
                val ls = myDao.getOneLocationWithSubs(id)
                Log.d(TAG, "Contact added ${ls.locationInfo.name} id ${ls.locationInfo.id}")

                _allLocationsWithSub.value!!.add(ls)
                emit(ls)
            }
        }
    }
}