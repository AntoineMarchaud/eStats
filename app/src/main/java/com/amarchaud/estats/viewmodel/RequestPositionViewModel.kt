package com.amarchaud.estats.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.amarchaud.estats.view.RequestPositionFragmentDirections
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RequestPositionViewModel @Inject constructor(val app: Application) : AndroidViewModel(app) {

    private var _actionShowSettingsDialog = MutableLiveData<Boolean>()
    val actionShowSettingsDialog: LiveData<Boolean>
        get() = _actionShowSettingsDialog

    private var _actionLiveData = MutableLiveData<NavDirections>()
    val actionLiveData: LiveData<NavDirections>
        get() = _actionLiveData


    fun onAskPermission() {

        val listPermissionsToAsk = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            listPermissionsToAsk.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        Dexter
            .withContext(app)
            .withPermissions(listPermissionsToAsk)
            .withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport) {

                    if (report.areAllPermissionsGranted()) {
                        _actionLiveData.postValue(RequestPositionFragmentDirections.actionRequestPositionFragmentToMainFragment())
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        _actionShowSettingsDialog.postValue(true)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }
}