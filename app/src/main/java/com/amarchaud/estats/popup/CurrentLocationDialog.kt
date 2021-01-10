package com.amarchaud.estats.popup

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.amarchaud.estats.R
import kotlinx.android.synthetic.main.popup_current_location.view.*

class CurrentLocationDialog : DialogFragment() {

    companion object {
        const val KEY_RESULT = "KEY_RESULT"
        const val KEY_LAT = "KEY_LAT"
        const val KEY_LON = "KEY_LON"
        const val KEY_NAME = "KEY_NAME"
        const val KEY_ID_MAIN = "KEY_ID_MAIN"

        fun newInstance(lat: Double, lon: Double, idMain: Int = -1): CurrentLocationDialog {

            val fragment = CurrentLocationDialog()

            val args = Bundle()
            args.putDouble(KEY_LAT, lat)
            args.putDouble(KEY_LON, lon)
            args.putInt(KEY_ID_MAIN, idMain)

            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var storeName : String

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putDouble(KEY_LAT, requireArguments().getDouble(KEY_LAT))
        outState.putDouble(KEY_LON, requireArguments().getDouble(KEY_LON))
        super.onSaveInstanceState(outState)
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {

            // recupération des data
            val lat = requireArguments().getDouble(KEY_LAT)
            val lon = requireArguments().getDouble(KEY_LON)
            val idMain = requireArguments().getInt(KEY_ID_MAIN)

            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            with(inflater.inflate(R.layout.popup_current_location, null)) {

                if (savedInstanceState != null) {
                    subLat.text = savedInstanceState.getString(KEY_LAT)
                    subLon.text = savedInstanceState.getString(KEY_LON)
                    //nameEditText.text =
                      //  SpannableStringBuilder(savedInstanceState.getString(KEY_NAME))
                } else {
                    subLat.text = java.lang.String.valueOf(lat)
                    subLon.text = java.lang.String.valueOf(lon)
                }

                builder
                    .setTitle(it.getString(R.string.addNewPositionTitle))
                    .setView(this)
                    .setPositiveButton(R.string.yes) { dialog, id ->

                        val result: Bundle = Bundle().apply {
                            putDouble(KEY_LAT, lat)
                            putDouble(KEY_LON, lon)
                            putString(KEY_NAME, nameEditText.text.toString())
                            putInt(KEY_ID_MAIN, idMain)
                        }

                        // send result to Listener(s)
                        parentFragmentManager.setFragmentResult(KEY_RESULT, result)
                        dialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, id ->
                        dialog?.cancel()
                    }

                builder.create()

            }


        } ?: throw IllegalStateException("Activity cannot be null")
    }
}