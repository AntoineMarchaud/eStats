package com.amarchaud.estats.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.amarchaud.estats.R
import com.amarchaud.estats.base.BaseViewModel
import com.amarchaud.estats.base.SingleLiveEvent
import com.amarchaud.estats.view.RequestPositionFragmentDirections
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class RequestPositionViewModel(val app: Application) : BaseViewModel(app) {

    val actionShowSettingsDialog: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val actionLiveData: MutableLiveData<NavDirections> = MutableLiveData()

    fun onAskPermission(v: View) {

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
                        actionLiveData.postValue(RequestPositionFragmentDirections.actionRequestPositionFragmentToMainFragment())
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        actionShowSettingsDialog.postValue(true)
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