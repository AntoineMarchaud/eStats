package com.amarchaud.estats.viewmodel

import android.Manifest
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
import android.widget.Toast
import androidx.databinding.Bindable
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amarchaud.estats.BR
import com.amarchaud.estats.R
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.base.SingleLiveEvent
import com.amarchaud.estats.dialog.AddMainLocationDialog
import com.amarchaud.estats.dialog.ListContactDialog
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.other.Contact
import com.amarchaud.estats.service.PositionService
import com.amarchaud.estats.utils.GeoCoder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
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

        enum class TypeItem {
            ITEM_INSERTED,
            ITEM_DELETED
        }

        enum class TypeHeaderItem {
            ITEM_MODIFIED
        }

        enum class TypeSubItem {
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
    val oneLocation: SingleLiveEvent<Triple<LocationInfo, TypeHeaderItem, Int>> = SingleLiveEvent()
    val oneSubLocation: SingleLiveEvent<Triple<LocationInfoSub, TypeSubItem, Pair<Int, Int>>> = SingleLiveEvent()
    val oneLocationWithSub: SingleLiveEvent<Triple<LocationWithSubs, TypeItem, Int>> = SingleLiveEvent()

    val allLocationsWithSub: MutableLiveData<List<LocationWithSubs>> = MutableLiveData()

    val displayDialog: SingleLiveEvent<String?> = SingleLiveEvent()

    private var mHandler: Handler? = null
    private var refreshDatasRunnable: Runnable = object : Runnable {
        override fun run() {
            try {

                currentDate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    DateTimeFormatter
                        .ofPattern(DATE_FORMAT)
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now())
                } else {
                    val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                    val date = Date()
                    formatter.format(date)
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
                            if (pos >= 0) {
                                oneLocation.postValue(Triple(ml, TypeHeaderItem.ITEM_MODIFIED, pos))
                            } else {
                                matchingLocation = null
                                notifyPropertyChanged(BR.matchingLocation)
                            }
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

                            if (pos >= 0) {
                                val posSub = listOfLocationWithSubs[pos].subLocation.indexOfFirst {
                                    it.idSub == subloc.idSub
                                }
                                if (posSub >= 0) {
                                    oneSubLocation.postValue(Triple(subloc, TypeSubItem.ITEM_MODIFIED, Pair(pos, posSub)))
                                }
                            } else {
                                matchingSubLocation = null
                                notifyPropertyChanged(BR.matchingSubLocation)
                            }
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
         * Add all at startup or onSavedInstance/onRestoreInstance
         */
        viewModelScope.launch {
            listOfLocationWithSubs = myDao.getAllLocationsWithSubs().toMutableList()
            allLocationsWithSub.postValue(listOfLocationWithSubs)
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

    /**
     * Call when user want to add a custom position
     */
    fun onDisplayContacts() {

        Dexter
            .withContext(app)
            .withPermission(Manifest.permission.READ_CONTACTS)
            .withListener(object : PermissionListener {

                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    // display fragment
                    displayDialog.postValue(ListContactDialog::class.simpleName)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(app, "need contact permission", Toast.LENGTH_LONG).show()
                }

                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                    p1?.continuePermissionRequest();
                }

            }).check()
    }

    /**
     * Called when user want to add his current position
     */
    fun onAddCurrentPosition() {

        if (bound) {
            mPositionService?.let {
                if (it.geoLoc == null) {
                    Toast.makeText(app, app.getString(R.string.activateGPS), Toast.LENGTH_LONG).show()
                } else {
                    displayDialog.postValue(AddMainLocationDialog::class.simpleName)
                }
            }
        }
    }

    /**
     * Callback of pop-up
     */
    fun onAddNewPosition(lat: Double, lon: Double, nameChoosen: String, delta: Int, idMain: Int?) {

        // if locationInfo is null, it is a new Location
        if (idMain == null) {
            val locationInfoInserted = LocationInfo(
                name = nameChoosen,
                lat = lat,
                lon = lon,
                delta = delta
            )

            // add to Database
            viewModelScope.launch {
                myDao.insert(locationInfoInserted)

                val ls = myDao.getLastInsertedLocationWithSubs()
                Log.d(TAG, "User add new location ${ls.locationInfo.name} id ${ls.locationInfo.id}")

                listOfLocationWithSubs.add(ls)
                oneLocationWithSub.postValue(Triple(ls, TypeItem.ITEM_INSERTED, listOfLocationWithSubs.size - 1))
            }
        } else { // it is a new sub location to add !

            val locationSub = LocationInfoSub(
                name = nameChoosen,
                lat = lat,
                lon = lon,
                idMain = idMain,
                delta = delta
            )

            // add to Database
            viewModelScope.launch {
                myDao.insert(locationSub)

                // get updated element in dao
                val ls = myDao.getLastInsertedSubLocation()

                // update current LocationInfo in the list
                val mainIndex = listOfLocationWithSubs.indexOfFirst {
                    it.locationInfo.id == idMain
                }

                listOfLocationWithSubs[mainIndex].subLocation.add(ls)
                oneSubLocation.postValue(Triple(ls, TypeSubItem.ITEM_INSERTED, Pair(mainIndex, listOfLocationWithSubs[mainIndex].subLocation.size - 1)))
            }

        }
    }

    fun deleteItem(locationToDelete: LocationInfo) {

        listOfLocationWithSubs.indexOfFirst {
            it.locationInfo.id == locationToDelete.id
        }.let { position ->

            listOfLocationWithSubs[position].let {

                // remove from db
                viewModelScope.launch {
                    myDao.delete(it)

                    // put matching to null if it was the same !!
                    matchingLocation?.apply {
                        if (id == locationToDelete.id) {
                            matchingLocation = null
                            notifyPropertyChanged(BR.matchingLocation)
                            matchingSubLocation = null
                            notifyPropertyChanged(BR.matchingSubLocation)
                        }
                    }

                    // remove from local list
                    listOfLocationWithSubs.removeAt(position)

                    // refresh view
                    oneLocationWithSub.postValue(Triple(it, TypeItem.ITEM_DELETED, position))
                }
            }
        }
    }

    fun deleteSubItem(subLocationToDelete: LocationInfoSub) {

        listOfLocationWithSubs.indexOfFirst {
            it.locationInfo.id == subLocationToDelete.idMain
        }.let { mainPosition ->
            if (mainPosition >= 0) {
                listOfLocationWithSubs[mainPosition].let { locationWithSubs ->

                    locationWithSubs.subLocation.indexOfFirst {
                        it.idSub == subLocationToDelete.idSub
                    }.let { positionSub ->

                        if (positionSub >= 0) {
                            viewModelScope.launch {
                                // remove from db
                                myDao.delete(subLocationToDelete)

                                // put matching to null if it was the same !!
                                matchingSubLocation?.apply {
                                    if (idSub == subLocationToDelete.idSub) {
                                        matchingSubLocation = null
                                        notifyPropertyChanged(BR.matchingSubLocation)
                                    }
                                }

                                // remove from local list
                                locationWithSubs.subLocation.removeAt(positionSub)

                                // refresh view
                                oneSubLocation.postValue(Triple(subLocationToDelete, TypeSubItem.ITEM_DELETED, Pair(mainPosition, positionSub)))
                            }
                        }
                    }
                }
            }
        }
    }

    fun onAddContacts(allContacts: ArrayList<Contact>) {

        allContacts.forEach { oneContact ->

            val name = oneContact.name
            val addr = oneContact.addr

            viewModelScope.launch {
                // convert addr to GeoLoc !
                val geoLoc = GeoCoder.getLocationFromAddressSuspend(addr, app)
                geoLoc?.let {

                    val locationInfoInserted = LocationInfo(
                        name = name,
                        lat = it.latitude,
                        lon = it.longitude
                    )

                    // add to Database
                    myDao.insert(locationInfoInserted)

                    val ls = myDao.getLastInsertedLocationWithSubs()
                    Log.d(TAG, "Contact added ${ls.locationInfo.name} id ${ls.locationInfo.id}")

                    listOfLocationWithSubs.add(ls)
                    oneLocationWithSub.value = (Triple(ls, TypeItem.ITEM_INSERTED, listOfLocationWithSubs.size - 1))
                }
            }
        }
    }
}