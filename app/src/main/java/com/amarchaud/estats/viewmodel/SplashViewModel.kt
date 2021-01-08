package com.amarchaud.estats.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.amarchaud.estats.R
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.view.SplashFragmentDirections
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SplashViewModel(app: Application) : BaseViewModel(app) {

    private val sharedPref =
        app.getSharedPreferences(app.getString(R.string.globalPref), Context.MODE_PRIVATE)


    val actionLiveData: MutableLiveData<NavDirections> = MutableLiveData()

    init {

        Observable.timer(1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {

                sharedPref.getBoolean(app.getString(R.string.isFirstLaunch), true).let {
                    if (it) {


                        with(sharedPref.edit()) {
                            putBoolean(app.getString(R.string.isFirstLaunch), false)
                            apply()
                        }

                        // go to request permission GPS Fragment
                        actionLiveData.postValue(SplashFragmentDirections.actionSplashFragmentToRequestPositionFragment())
                    } else {
                        // go to main Fragment
                        actionLiveData.postValue(SplashFragmentDirections.actionSplashFragmentToMainFragment())
                    }
                }


            }
    }
}