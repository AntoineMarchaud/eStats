package com.amarchaud.estats.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.extension.createCircle
import com.amarchaud.estats.extension.addCircle
import com.amarchaud.estats.extension.initMapView
import com.amarchaud.estats.viewmodel.MapViewModel
import com.amarchaud.estats.viewmodel.data.NumberPickerViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


@AndroidEntryPoint
class MapFragment : Fragment() {

    companion object {

        const val MODE_NORMAL = 0
        const val MODE_MAIN = 1
        const val MODE_SUB = 2

        fun newInstance(mode: Int = MODE_NORMAL): MapFragment {

            val fragment = MapFragment()

            val args = Bundle()
            args.putInt("mode", mode)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private val numberPickerViewModel: NumberPickerViewModel by activityViewModels()

    private var myPositionCircle: Polygon? = null
    private var myPositionMarker: Marker? = null
    private lateinit var sharedPref: SharedPreferences

    private var initCenterX: Double = 0.0
    private var initCenterY: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        sharedPref = requireContext().getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )

        _binding = MapFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding) {

            with(mapView) {

                val initCenterX = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)))
                val initCenterY = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)))
                initMapView(GeoPoint(initCenterX, initCenterY))

                myPositionMarker = Marker(this)
                myPositionMarker?.let { marker ->
                    val geoPoint = GeoPoint(initCenterX, initCenterY)
                    marker.position = geoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    if (!overlayManager.contains(myPositionMarker))
                        overlayManager.add(marker)
                }

                when (requireArguments().getInt("mode")) {
                    MODE_MAIN -> {
                        myPositionCircle =
                            createCircle(GeoPoint(initCenterX, initCenterY), numberPickerViewModel.pickerValueMutableLiveData.value?.toDouble() ?: 0.0, requireContext().getColor(R.color.mainLocationCircleColor), -1)
                    }
                    MODE_SUB -> {
                        myPositionCircle =
                            createCircle(GeoPoint(initCenterX, initCenterY), 7.toDouble(), requireContext().getColor(R.color.subLocationCircleColor), -1)
                    }
                }
            }

            with(centerView) {
                setOnClickListener {
                    viewModel.myGeoLoc.value?.apply {
                        val geoPoint = GeoPoint(latitude, longitude)
                        mapView.controller.animateTo(geoPoint)
                    }
                }
            }
        }

        // update current geoloc
        viewModel.myGeoLoc.observe(viewLifecycleOwner, { location ->
            // update map
            myPositionMarker?.let { marker ->
                marker.position = GeoPoint(location.latitude, location.longitude)
                if (!binding.mapView.overlayManager.contains(marker))
                    binding.mapView.overlayManager.add(marker)
            }

            when (requireArguments().getInt("mode")) {
                MODE_MAIN -> {
                    myPositionCircle?.points = Polygon.pointsAsCircle(
                        GeoPoint(viewModel.myGeoLoc.value?.latitude ?: initCenterX, viewModel.myGeoLoc.value?.longitude ?: initCenterY),
                        numberPickerViewModel.pickerValueMutableLiveData.value?.toDouble() ?: 10.0
                    )
                    if (!binding.mapView.overlayManager.contains(myPositionCircle))
                        binding.mapView.overlayManager.add(myPositionCircle)
                }
                MODE_SUB -> {
                    myPositionCircle?.points = Polygon.pointsAsCircle(
                        GeoPoint(viewModel.myGeoLoc.value?.latitude ?: initCenterX, viewModel.myGeoLoc.value?.longitude ?: initCenterY), 7.toDouble()
                    )
                    if (!binding.mapView.overlayManager.contains(myPositionCircle))
                        binding.mapView.overlayManager.add(myPositionCircle)
                }
            }

            binding.mapView.invalidate()
        })

        numberPickerViewModel.pickerValueMutableLiveData.observe(viewLifecycleOwner, {
            // update map
            myPositionCircle?.points = Polygon.pointsAsCircle(GeoPoint(viewModel.myGeoLoc.value?.latitude ?: initCenterX, viewModel.myGeoLoc.value?.longitude ?: initCenterY), it.toDouble())
            if (!binding.mapView.overlayManager.contains(myPositionCircle))
                binding.mapView.overlayManager.add(myPositionCircle)
            binding.mapView.invalidate()
        })

        viewModel.allLocationsWithSub.observe(viewLifecycleOwner,
            { allLocationsWithSubs ->

                allLocationsWithSubs.forEach { locationWithSubs ->

                    // add markers
                    with(locationWithSubs) {

                        with(this.locationInfo) {
                            binding.mapView.addMarker(lat, lon, name, id)
                            binding.mapView.addCircle(GeoPoint(lat, lon), this.delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor), id)
                        }

                        // todo add subitem marker ?
                        with(this.subLocation) {
                            forEach {
                                binding.mapView.addCircle(GeoPoint(it.lat, it.lon), it.delta.toDouble(), requireContext().getColor(R.color.subLocationCircleColor), it.idSub)
                            }
                        }
                    }
                }
            })

    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}