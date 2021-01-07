package com.amarchaud.estats.popup

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.amarchaud.estats.R
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.view.MainFragment
import kotlinx.android.synthetic.main.popup_current_location.*
import kotlinx.android.synthetic.main.popup_current_location.view.*

class CurrentLocationPopup(
    private val lat: Double,
    private val lon: Double,
    private val listener: CurrentLocationDialogListener
) : DialogFragment() {

    private var locationInfo: LocationInfo? = null

    constructor(lat: Double, lon: Double, locationInfo: LocationInfo, listener: CurrentLocationDialogListener) : this(lat, lon, listener) {
        this.locationInfo = locationInfo
    }

    companion object {
        const val KEY_LAT = "KEY_LAT"
        const val KEY_LON = "KEY_LON"
        const val KEY_NAME = "KEY_NAME"
    }

    interface CurrentLocationDialogListener {
        fun onCurrentLocationDialogPositiveClick(lat: Double, lon: Double, nameChoosen: String, locationInfo: LocationInfo?)
        fun onCurrentLocationDialogListenerNegativeClick()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            with(inflater.inflate(R.layout.popup_current_location, null)) {

                if (savedInstanceState != null) {
                    subLat.text = savedInstanceState.getString(KEY_LAT)
                    subLon.text = savedInstanceState.getString(KEY_LON)
                    nameEditText.text =
                        SpannableStringBuilder(savedInstanceState.getString(KEY_NAME))
                } else {
                    subLat.text = java.lang.String.valueOf(lat)
                    subLon.text = java.lang.String.valueOf(lon)
                }

                builder
                    .setTitle(it.getString(R.string.addNewPositionTitle))
                    .setView(this)
                    .setPositiveButton(R.string.yes) { dialog, id ->
                        listener.onCurrentLocationDialogPositiveClick(
                            lat,
                            lon,
                            nameEditText.text.toString(),
                            locationInfo
                        )
                        dialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, id ->
                        listener.onCurrentLocationDialogListenerNegativeClick()
                        dialog?.cancel()
                    }

                builder.create()

            }


        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_LAT, subLat.text.toString())
        outState.putString(KEY_LON, subLon.text.toString())
        outState.putString(KEY_NAME, nameEditText.text.toString())
    }
}