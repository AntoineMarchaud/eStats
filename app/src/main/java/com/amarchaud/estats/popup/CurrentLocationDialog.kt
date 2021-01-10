package com.amarchaud.estats.popup

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogCurrentLocationBinding
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel

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
    private val geoPointViewModel: GeoPointViewModel by activityViewModels()

    private var _binding: DialogCurrentLocationBinding? = null
    private val binding get() = _binding!!
    private var idMainStored = 0

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAT, binding.lat.text.toString())
        outState.putString(KEY_LON, binding.lon.text.toString())
        outState.putString(KEY_NAME, binding.nameEditText.text.toString())
        outState.putInt(KEY_ID_MAIN, idMainStored)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        geoPointViewModel.geoLoc.observe(this, {
            binding.lat.text = it.latitude.toString()
            binding.lon.text = it.longitude.toString()
        })

        return activity?.let {

            _binding = DialogCurrentLocationBinding.inflate(LayoutInflater.from(context))

            // recupération des data
            val latitude = requireArguments().getDouble(KEY_LAT)
            val longitude = requireArguments().getDouble(KEY_LON)
            val idMain = requireArguments().getInt(KEY_ID_MAIN)

            with(binding) {

                val builder = AlertDialog.Builder(it)

                if (savedInstanceState != null) {
                    lat.text = savedInstanceState.getString(KEY_LAT)
                    lon.text = savedInstanceState.getString(KEY_LON)
                    nameEditText.text = SpannableStringBuilder(savedInstanceState.getString(KEY_NAME))
                    idMainStored = savedInstanceState.getInt(KEY_ID_MAIN)
                } else {
                    lat.text = java.lang.String.valueOf(latitude)
                    lon.text = java.lang.String.valueOf(longitude)
                    idMainStored = idMain
                }

                builder
                    .setTitle(it.getString(R.string.addNewPositionTitle))
                    .setView(binding.root)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        val result: Bundle = Bundle().apply {
                            putDouble(KEY_LAT, java.lang.Double.parseDouble(lat.text.toString()))
                            putDouble(KEY_LON, java.lang.Double.parseDouble(lon.text.toString()))
                            putString(KEY_NAME, nameEditText.text.toString())
                            putInt(KEY_ID_MAIN, idMain)
                        }

                        // send result to Listener(s)
                        parentFragmentManager.setFragmentResult(KEY_RESULT, result)
                        dialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog?.cancel()
                    }

                builder.create()
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}