package com.amarchaud.estats.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogCurrentLocationBinding
import com.amarchaud.estats.view.MapFragment
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.amarchaud.estats.viewmodel.data.NumberPickerViewModel


class AddMainLocationDialog : DialogFragment() {

    companion object {
        // in  and out
        const val KEY_LAT = "KEY_LAT"
        const val KEY_LON = "KEY_LON"

        //out
        const val KEY_DELTA_RETURNED = "KEY_DELTA"
        const val KEY_RESULT_MAIN = "KEY_RESULT_MAIN"
        const val KEY_NAME_RETURNED = "KEY_NAME_RETURNED"

        fun newInstance(lat: Double, lon: Double): AddMainLocationDialog {

            val fragment = AddMainLocationDialog()

            val args = Bundle()
            args.putDouble(KEY_LAT, lat)
            args.putDouble(KEY_LON, lon)

            fragment.arguments = args
            return fragment
        }
    }

    private val geoPointViewModel: GeoPointViewModel by activityViewModels()
    private val numberPickerViewModel: NumberPickerViewModel by activityViewModels()

    private var _binding: DialogCurrentLocationBinding? = null
    private val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAT, binding.lat.text.toString())
        outState.putString(KEY_LON, binding.lon.text.toString())
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())
        outState.putInt(KEY_DELTA_RETURNED, binding.numberPickerDelta.value)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        geoPointViewModel.geoLoc.observe(this) { currentLocation ->
            // update dialog
            binding.lat.text = currentLocation.latitude.toString()
            binding.lon.text = currentLocation.longitude.toString()
        }


        return activity?.let {

            _binding = DialogCurrentLocationBinding.inflate(LayoutInflater.from(context))
            with(binding) {

                // add MapViewFragment

                numberPickerDelta.minValue = 7
                numberPickerDelta.maxValue = 50

                val builder = AlertDialog.Builder(it)

                if (savedInstanceState != null) {
                    lat.text = savedInstanceState.getString(KEY_LAT)
                    lon.text = savedInstanceState.getString(KEY_LON)
                    savedInstanceState.getString(KEY_NAME_RETURNED)?.let{
                        nameEditText.text = SpannableStringBuilder(it)
                    }
                    numberPickerDelta.value = savedInstanceState.getInt(KEY_DELTA_RETURNED)
                } else {
                    with(requireArguments()) {
                        lat.text = getDouble(KEY_LAT).toString()
                        lon.text = getDouble(KEY_LON).toString()
                    }
                    numberPickerDelta.value = 10
                }

                numberPickerDelta.setFormatter { i ->
                    i.toString() + "m"
                }
                numberPickerDelta.setOnValueChangedListener { numberPicker, i, i2 ->
                    numberPickerViewModel.pickerValue.value = i2
                }


                builder
                    .setTitle(it.getString(R.string.addNewPositionTitle))
                    .setView(binding.root)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        val result: Bundle = Bundle().apply {
                            putDouble(KEY_LAT, java.lang.Double.parseDouble(lat.text.toString()))
                            putDouble(KEY_LON, java.lang.Double.parseDouble(lon.text.toString()))
                            putString(KEY_NAME_RETURNED, nameEditText.text.toString())
                            putInt(KEY_DELTA_RETURNED, numberPickerDelta.value)
                        }

                        // send result to Listener(s)
                        requireActivity().supportFragmentManager.setFragmentResult(KEY_RESULT_MAIN, result)
                        dialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog?.cancel()
                    }

                builder.create()
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val childFragment: Fragment = MapFragment()
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.mapViewContainer, childFragment).commit()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}