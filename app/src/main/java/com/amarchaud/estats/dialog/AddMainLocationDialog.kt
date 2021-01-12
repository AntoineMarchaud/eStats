package com.amarchaud.estats.dialog

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogAddMainLocationBinding
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

        val valuesPicker = mutableListOf("10m", "15m", "20m", "25m", "30m", "35m", "40m", "50m", "60m", "70m", "80m", "90m", "100m")
        fun NumberPicker.positionToValue() = valuesPicker[this.value].replace("m", "").toInt()

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

    private var _binding: DialogAddMainLocationBinding? = null
    private val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAT, binding.lat.text.toString())
        outState.putString(KEY_LON, binding.lon.text.toString())
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())
        outState.putInt(KEY_DELTA_RETURNED, binding.numberPickerDelta.value) // index !
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = DialogAddMainLocationBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // add MapViewFragment
        if (savedInstanceState == null) {
            val childFragment: Fragment = MapFragment()
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            transaction.add(R.id.mapViewContainer, childFragment).commit()
        }

        with(binding) {

            numberPickerDelta.minValue = 0
            numberPickerDelta.maxValue = valuesPicker.size - 1
            numberPickerDelta.displayedValues = valuesPicker.toTypedArray()

            if (savedInstanceState != null) {
                lat.text = savedInstanceState.getString(KEY_LAT)
                lon.text = savedInstanceState.getString(KEY_LON)
                savedInstanceState.getString(KEY_NAME_RETURNED)?.let {
                    nameEditText.text = SpannableStringBuilder(it)
                }
                numberPickerDelta.value = savedInstanceState.getInt(KEY_DELTA_RETURNED) // index !
            } else {
                with(requireArguments()) {
                    lat.text = getDouble(KEY_LAT).toString()
                    lon.text = getDouble(KEY_LON).toString()
                }
                numberPickerDelta.value = 0 // index !
            }

            // update ViewModel
            numberPickerViewModel.pickerValue.value = numberPickerDelta.positionToValue()
            numberPickerDelta.setOnValueChangedListener { numberPicker, i, i2 ->
                numberPickerViewModel.pickerValue.value = numberPickerDelta.positionToValue()
            }

            dialogTitle.text = requireContext().getString(R.string.addNewPositionTitle)

            okButton.setOnClickListener {

                if (nameEditText.text.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.nameEmpty), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                val result: Bundle = Bundle().apply {
                    putDouble(KEY_LAT, java.lang.Double.parseDouble(lat.text.toString()))
                    putDouble(KEY_LON, java.lang.Double.parseDouble(lon.text.toString()))
                    putString(KEY_NAME_RETURNED, nameEditText.text.toString())
                    putInt(KEY_DELTA_RETURNED, numberPickerDelta.positionToValue())
                }

                // send result to Listener(s)
                requireActivity().supportFragmentManager.setFragmentResult(KEY_RESULT_MAIN, result)

                dismiss()
            }
            cancelButton.setOnClickListener {
                dismiss()
            }
        }


        geoPointViewModel.geoLoc.observe(this) { currentLocation ->
            // update dialog
            binding.lat.text = currentLocation.latitude.toString()
            binding.lon.text = currentLocation.longitude.toString()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}