package com.amarchaud.estats.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.amarchaud.estats.R
import com.amarchaud.estats.view.SplashFragmentDirections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(app: Application) : AndroidViewModel(app) {

    private val sharedPref =
        app.getSharedPreferences(app.getString(R.string.globalPref), Context.MODE_PRIVATE)


    private var _actionLiveData = MutableLiveData<NavDirections>()
    val actionLiveData: LiveData<NavDirections>
        get() = _actionLiveData


    init {

        viewModelScope.launch {
            delay(1000L)

            sharedPref.getBoolean(app.getString(R.string.isFirstLaunch), true).let {
                if (it) {
                    with(sharedPref.edit()) {
                        putBoolean(app.getString(R.string.isFirstLaunch), false)
                        apply()
                    }

                    // go to request permission GPS Fragment
                    _actionLiveData.postValue(SplashFragmentDirections.actionSplashFragmentToRequestPositionFragment())
                } else {
                    // go to main Fragment
                    _actionLiveData.postValue(SplashFragmentDirections.actionSplashFragmentToMainFragment())
                }
            }
        }
    }
}