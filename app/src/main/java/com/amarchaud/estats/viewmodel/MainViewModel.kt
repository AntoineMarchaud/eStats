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
import com.amarchaud.estats.BR
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.model.database.AppDao
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.service.PositionService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class MainViewModel @ViewModelInject constructor(
    val app: Application,
    myDao: AppDao // injected by hilt
) : BaseViewModel(app) {

    companion object {
        const val TAG = "MainViewModel"
        const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss"
    }

    private var mService: PositionService? = null
    private var bound: Boolean = false

    // Bindable properties ***************************************************************
    @Bindable
    var currentDate: String? = null

    @Bindable
    var matchingLocation: LocationInfo? = null

    // LiveData properties ***************************************************************
    val myGeoLoc: MutableLiveData<Location> = MutableLiveData()
    val locations: MutableLiveData<List<LocationInfo>> = MutableLiveData()

    private var mHandler: Handler? = null
    private var refreshDatasRunnable: Runnable = object : Runnable {
        override fun run() {
            try {

                //Java 8 java.time.Instant
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

                    mService?.let {
                        matchingLocation = it.matchingLocation
                        notifyPropertyChanged(BR.matchingLocation)

                        myGeoLoc.postValue(it.currentLocation)
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

        myDao.getAllLocations()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                locations.postValue(it)
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

    fun onAddCustomPosition(v: View) {
        println("onAddCustomPosition")
    }

    fun onAddCurrentPosition(v: View) {
        println("onAddCurrentPosition")
    }
}