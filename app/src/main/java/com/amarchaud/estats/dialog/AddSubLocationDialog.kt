package com.amarchaud.estats.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogAddSubLocationBinding
import com.amarchaud.estats.utils.Distance
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel

class AddSubLocationDialog : DialogFragment() {

    companion object {

        // in
        private const val KEY_PARENT_LAT = "KEY_PARENT_LAT"
        private const val KEY_PARENT_LON = "KEY_PARENT_LON"
        private const val KEY_PARENT_NAME = "KEY_PARENT_NAME"
        private const val KEY_PARENT_DELTA = "KEY_DELTA"

        // in and out
        const val KEY_PARENT_ID = "KEY_PARENT_ID"

        //out
        const val KEY_RESULT_SUB = "KEY_RESULT_SUB"
        const val KEY_RETURNED_LAT = "KEY_RETURNED_LAT"
        const val KEY_RETURNED_LON = "KEY_RETURNED_LON"
        const val KEY_NAME_RETURNED = "KEY_NAME_RETURNED"

        fun newInstance(name: String, lat: Double, lon: Double, delta: Int, idMain: Int): AddSubLocationDialog {

            val fragment = AddSubLocationDialog()

            val args = Bundle()

            args.putDouble(KEY_PARENT_LAT, lat)
            args.putDouble(KEY_PARENT_LON, lon)
            args.putString(KEY_PARENT_NAME, name)
            args.putInt(KEY_PARENT_ID, idMain)
            args.putInt(KEY_PARENT_DELTA, delta)

            fragment.arguments = args
            return fragment
        }
    }

    private val geoPointViewModel: GeoPointViewModel by activityViewModels()

    private var _binding: DialogAddSubLocationBinding? = null
    private val binding get() = _binding!!

    private var parentLatStored: Double = 0.0
    private var parentLonStored: Double = 0.0
    private var parentNameStored: String? = null
    private var parentDeltaStored: Int = 0
    private var idMainStored: Int = -1

    override fun onSaveInstanceState(outState: Bundle) {
        // in
        outState.putDouble(KEY_PARENT_LAT, parentLatStored)
        outState.putDouble(KEY_PARENT_LON, parentLonStored)
        outState.putString(KEY_PARENT_NAME, parentNameStored)
        outState.putInt(KEY_PARENT_DELTA, parentDeltaStored)

        // in and out
        outState.putInt(KEY_PARENT_ID, idMainStored)

        // out
        outState.putDouble(KEY_RETURNED_LAT, java.lang.Double.parseDouble(binding.lat.text.toString()))
        outState.putDouble(KEY_RETURNED_LON, java.lang.Double.parseDouble(binding.lon.text.toString()))
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())

        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        geoPointViewModel.geoLoc.observe(this, { currentLocation ->
            binding.lat.text = currentLocation.latitude.toString()
            binding.lon.text = currentLocation.longitude.toString()

            if (Distance.measure(currentLocation.latitude, currentLocation.longitude, parentLatStored, parentLonStored) >= parentDeltaStored) {
                binding.alertLabel.visibility = View.VISIBLE
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            } else {
                binding.alertLabel.visibility = View.INVISIBLE
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }
        })

        return activity?.let {

            _binding = DialogAddSubLocationBinding.inflate(LayoutInflater.from(context))

            with(binding) {

                val builder = AlertDialog.Builder(it)

                if (savedInstanceState != null) {
                    // in
                    lat.text = savedInstanceState.getDouble(KEY_RETURNED_LAT).toString()
                    lon.text = savedInstanceState.getDouble(KEY_RETURNED_LON).toString()
                    parentLatStored = savedInstanceState.getDouble(KEY_PARENT_LAT)
                    parentLonStored = savedInstanceState.getDouble(KEY_PARENT_LON)
                    parentNameStored = savedInstanceState.getString(KEY_PARENT_NAME)
                    parentDeltaStored = savedInstanceState.getInt(KEY_PARENT_DELTA)

                    // in and out
                    idMainStored = savedInstanceState.getInt(KEY_PARENT_ID)

                    //out
                    nameEditText.text = SpannableStringBuilder(savedInstanceState.getString(KEY_NAME_RETURNED))
                } else {
                    with(requireArguments()) {
                        lat.text = getDouble(KEY_PARENT_LAT).toString()
                        lon.text = getDouble(KEY_PARENT_LON).toString()
                        parentLatStored = getDouble(KEY_PARENT_LAT)
                        parentLonStored = getDouble(KEY_PARENT_LON)
                        parentNameStored = getString(KEY_PARENT_NAME)
                        parentDeltaStored = getInt(KEY_PARENT_DELTA)

                        // in and out
                        idMainStored = getInt(KEY_PARENT_ID)
                    }
                }

                builder
                    .setTitle(it.getString(R.string.addNewSubPositionTitle) + " " + parentNameStored)
                    .setView(binding.root)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        val result: Bundle = Bundle().apply {
                            putDouble(KEY_RETURNED_LAT, java.lang.Double.parseDouble(lat.text.toString()))
                            putDouble(KEY_RETURNED_LON, java.lang.Double.parseDouble(lon.text.toString()))
                            putString(KEY_NAME_RETURNED, nameEditText.text.toString())
                            putInt(KEY_PARENT_ID, idMainStored)
                        }

                        // send result to Listener(s)
                        parentFragmentManager.setFragmentResult(KEY_RESULT_SUB, result)
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