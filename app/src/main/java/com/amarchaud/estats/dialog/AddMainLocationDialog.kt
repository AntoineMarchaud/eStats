package com.amarchaud.estats.dialog

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.*
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogAddMainLocationBinding
import com.amarchaud.estats.view.MapFragment
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.amarchaud.estats.viewmodel.data.NewPositionViewModel
import com.amarchaud.estats.viewmodel.data.NumberPickerViewModel


class AddMainLocationDialog : DialogFragment() {

    companion object {
        // in
        const val KEY_MODE_IS_FIXED = "KEY_MODE_IS_FIXED"
        const val KEY_LAT_FIXED = "KEY_LAT_FIXED"
        const val KEY_LON_FIXED = "KEY_LON_FIXED"

        //out
        const val KEY_RESULT_MAIN = "KEY_RESULT_MAIN"

        const val KEY_LAT_RETURNED = "KEY_LAT_RETURNED"
        const val KEY_LON_RETURNED = "KEY_LON_RETURNED"
        const val KEY_DELTA_RETURNED = "KEY_DELTA"
        const val KEY_NAME_RETURNED = "KEY_NAME_RETURNED"

        val valuesPicker = mutableListOf("10m", "15m", "20m", "25m", "30m", "35m", "40m", "50m", "60m", "70m", "80m", "90m", "100m")
        fun NumberPicker.positionToRadiusInMeter() = valuesPicker[this.value].replace("m", "").toInt()

        fun newInstance(): AddMainLocationDialog {

            val fragment = AddMainLocationDialog()

            val args = Bundle()
            args.putBoolean(KEY_MODE_IS_FIXED, false)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(lat: Double, lon: Double): AddMainLocationDialog {

            val fragment = AddMainLocationDialog()

            val args = Bundle()
            args.putBoolean(KEY_MODE_IS_FIXED, true)
            args.putDouble(KEY_LAT_FIXED, lat)
            args.putDouble(KEY_LON_FIXED, lon)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var sharedPref: SharedPreferences

    //viewmodel custom
    private val newPositionViewModel: NewPositionViewModel by activityViewModels()
    private val geoPointViewModel: GeoPointViewModel by activityViewModels()
    private val numberPickerViewModel: NumberPickerViewModel by activityViewModels()

    private var _binding: DialogAddMainLocationBinding? = null
    private val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())
        outState.putInt(KEY_DELTA_RETURNED, binding.numberPickerDelta.value) // index !
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)


        sharedPref = requireContext().getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )

        _binding = DialogAddMainLocationBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // add MapViewFragment
        if (savedInstanceState == null) {
            with(requireArguments()) {

                val childFragment: Fragment = if (getBoolean(KEY_MODE_IS_FIXED)) {
                    MapFragment.newInstance(MapFragment.MODE_MAIN_CUSTOM_POSITION, getDouble(KEY_LAT_FIXED), getDouble(KEY_LON_FIXED))
                } else {
                    MapFragment.newInstance(MapFragment.MODE_MAIN)
                }
                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.mapViewContainer, childFragment).commit()
            }
        }

        with(binding) {

            numberPickerDelta.minValue = 0
            numberPickerDelta.maxValue = valuesPicker.size - 1
            numberPickerDelta.displayedValues = valuesPicker.toTypedArray()

            // 2 modes : fix or dynamic
            with(requireArguments()) {
                if (getBoolean(KEY_MODE_IS_FIXED)) {
                    lat.text = getDouble(KEY_LAT_FIXED).toString()
                    lon.text = getDouble(KEY_LON_FIXED).toString()
                } else {
                    lat.text = sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)).toString()
                    lon.text = sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)).toString()
                }
            }

            if (savedInstanceState != null) {
                savedInstanceState.getString(KEY_NAME_RETURNED)?.let {
                    nameEditText.text = SpannableStringBuilder(it)
                }
                numberPickerDelta.value = savedInstanceState.getInt(KEY_DELTA_RETURNED) // index !
            } else {
                numberPickerDelta.value = 0 // index !
            }

            // update ViewModel
            numberPickerViewModel.setPickerValue(numberPickerDelta.positionToRadiusInMeter())
            numberPickerDelta.setOnValueChangedListener { _, _, _ ->
                numberPickerViewModel.setPickerValue(numberPickerDelta.positionToRadiusInMeter())
            }

            dialogTitle.text = requireContext().getString(R.string.addNewPositionTitle)

            okButton.setOnClickListener {

                if (nameEditText.text.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.nameEmpty), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                val result: Bundle = Bundle().apply {
                    putDouble(KEY_LAT_RETURNED, java.lang.Double.parseDouble(lat.text.toString()))
                    putDouble(KEY_LON_RETURNED, java.lang.Double.parseDouble(lon.text.toString()))
                    putString(KEY_NAME_RETURNED, nameEditText.text.toString())
                    putInt(KEY_DELTA_RETURNED, numberPickerDelta.positionToRadiusInMeter())
                }

                // send result to parent Listener
                setFragmentResult(KEY_RESULT_MAIN, result)

                // same thing by viewModel method
                newPositionViewModel.setNewPosition(
                    NewPositionViewModel.NewPosition(
                        java.lang.Double.parseDouble(lat.text.toString()),
                        java.lang.Double.parseDouble(lon.text.toString()),
                        nameEditText.text.toString(),
                        numberPickerDelta.positionToRadiusInMeter()
                    )
                )

                dismiss()
            }
            cancelButton.setOnClickListener {
                dismiss()
            }
        }

        with(requireArguments()) {
            if (!getBoolean(KEY_MODE_IS_FIXED)) {
                geoPointViewModel.geoLoc.observe(this@AddMainLocationDialog) { currentLocation ->
                    // update dialog
                    binding.lat.text = currentLocation.latitude.toString()
                    binding.lon.text = currentLocation.longitude.toString()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}