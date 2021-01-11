package com.amarchaud.estats.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.DialogCurrentLocationBinding
import com.amarchaud.estats.extension.createCircle
import com.amarchaud.estats.extension.initMapView
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


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

    private var myPositionMarker: Marker? = null
    private var myCircle: Polygon? = null

    private val geoPointViewModel: GeoPointViewModel by activityViewModels()

    private var _binding: DialogCurrentLocationBinding? = null
    private val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAT, binding.lat.text.toString())
        outState.putString(KEY_LON, binding.lon.text.toString())
        outState.putString(KEY_NAME_RETURNED, binding.nameEditText.text.toString())
        outState.putInt(KEY_NAME_RETURNED, binding.numberPickerDelta.value)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        geoPointViewModel.geoLoc.observe(this) { currentLocation ->
            // update dialog
            binding.lat.text = currentLocation.latitude.toString()
            binding.lon.text = currentLocation.longitude.toString()

            // update map
            val g = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            binding.mapView.controller.animateTo(g)

            myPositionMarker?.let { marker ->
                marker.position = g
                if (!binding.mapView.overlays.contains(marker))
                    binding.mapView.overlays.add(marker)
            }

            myCircle?.points = Polygon.pointsAsCircle(GeoPoint(currentLocation.latitude, currentLocation.longitude), binding.numberPickerDelta.value.toDouble())
            binding.mapView.invalidate()
        }

        return activity?.let {

            _binding = DialogCurrentLocationBinding.inflate(LayoutInflater.from(context))


            with(binding) {

                numberPickerDelta.minValue = 7
                numberPickerDelta.maxValue = 50

                val builder = AlertDialog.Builder(it)

                if (savedInstanceState != null) {
                    lat.text = savedInstanceState.getString(KEY_LAT)
                    lon.text = savedInstanceState.getString(KEY_LON)
                    nameEditText.text = SpannableStringBuilder(savedInstanceState.getString(KEY_NAME_RETURNED))
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


                mapView.apply {

                    val initCenterX: Double
                    val initCenterY: Double
                    if (savedInstanceState != null) {
                        initCenterX = savedInstanceState.getDouble(KEY_LAT)
                        initCenterY = savedInstanceState.getDouble(KEY_LON)
                    } else {
                        initCenterX = requireArguments().getDouble(KEY_LAT)
                        initCenterY = requireArguments().getDouble(KEY_LON)
                    }

                    initMapView(GeoPoint(initCenterX, initCenterY))

                    myPositionMarker = Marker(this)
                    myPositionMarker?.let { marker ->
                        val geoPoint = GeoPoint(initCenterX, initCenterY)
                        marker.position = geoPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        if (!mapView.overlays.contains(myPositionMarker))
                            overlays.add(marker)
                    }

                    myCircle = createCircle(GeoPoint(initCenterX, initCenterY), numberPickerDelta.value.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor))
                    overlayManager.add(myCircle)
                    invalidate()
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
                        parentFragmentManager.setFragmentResult(KEY_RESULT_MAIN, result)
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