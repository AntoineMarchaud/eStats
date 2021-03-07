package com.amarchaud.estats.dialog

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.*
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogAddMainLocationBinding
import com.amarchaud.estats.databinding.DialogAddSubLocationBinding
import com.amarchaud.estats.extension.initMapView
import com.amarchaud.estats.utils.Distance
import com.amarchaud.estats.view.MapFragment
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

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
        const val KEY_LAT_RETURNED = "KEY_LAT_RETURNED"
        const val KEY_LON_RETURNED = "KEY_LON_RETURNED"
        const val KEY_NAME_RETURNED = "KEY_NAME_RETURNED"

        fun newInstance(name: String, lat: Double, lon: Double, delta: Int, idMain: Int): AddSubLocationDialog {

            val fragment = AddSubLocationDialog()

            val args = Bundle()

            args.putDouble(KEY_PARENT_LAT, lat)
            args.putDouble(KEY_PARENT_LON, lon)
            args.putString(KEY_PARENT_NAME, name)
            args.putInt(KEY_PARENT_DELTA, delta)
            args.putInt(KEY_PARENT_ID, idMain)

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

    private lateinit var sharedPref: SharedPreferences

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)


        sharedPref = requireContext().getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )

        _binding = DialogAddSubLocationBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // add MapViewFragment
        if (savedInstanceState == null) {
            val childFragment: Fragment = MapFragment.newInstance(MapFragment.MODE_SUB)
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            transaction.add(R.id.mapViewContainer, childFragment).commit()
        }

        with(binding) {

            lat.text = sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)).toString()
            lon.text = sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)).toString()

            if (savedInstanceState != null) {
                //out
                nameEditText.text = SpannableStringBuilder(savedInstanceState.getString(KEY_NAME_RETURNED))
            }

            with(requireArguments()) {
                parentLatStored = getDouble(KEY_PARENT_LAT)
                parentLonStored = getDouble(KEY_PARENT_LON)
                parentNameStored = getString(KEY_PARENT_NAME)
                parentDeltaStored = getInt(KEY_PARENT_DELTA)

                // in and out
                idMainStored = getInt(KEY_PARENT_ID)
            }

            dialogTitle.text = getString(R.string.addNewPositionToMainTitle, parentNameStored)

            okButton.setOnClickListener {

                if (nameEditText.text.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.nameEmpty), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val result: Bundle = Bundle().apply {
                    putDouble(KEY_LAT_RETURNED, java.lang.Double.parseDouble(lat.text.toString()))
                    putDouble(KEY_LON_RETURNED, java.lang.Double.parseDouble(lon.text.toString()))
                    putString(KEY_NAME_RETURNED, nameEditText.text.toString())
                    putInt(KEY_PARENT_ID, idMainStored)
                }

                // send result to Listener(s)
                setFragmentResult(KEY_RESULT_SUB, result)
                dialog?.dismiss()
            }

            cancelButton.setOnClickListener {
                dismiss()
            }
        }

        geoPointViewModel.geoLoc.observe(this, { currentLocation ->
            binding.lat.text = currentLocation.latitude.toString()
            binding.lon.text = currentLocation.longitude.toString()

            if (Distance.measure(currentLocation.latitude, currentLocation.longitude, parentLatStored, parentLonStored) >= parentDeltaStored) {
                binding.alertLabel.visibility = View.VISIBLE
                binding.okButton.isEnabled = false
            } else {
                binding.alertLabel.visibility = View.INVISIBLE
                binding.okButton.isEnabled = true
            }
        })

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}