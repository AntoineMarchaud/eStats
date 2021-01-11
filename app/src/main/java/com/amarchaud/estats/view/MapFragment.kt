package com.amarchaud.estats.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.extension.drawCircle
import com.amarchaud.estats.extension.initMapView
import com.amarchaud.estats.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polygon
import java.lang.System.gc


@AndroidEntryPoint
class MapFragment : Fragment() {

    companion object {
        const val TAG = "MapFragment"
    }

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels() // replace ViewModelProvider

    private var myPositionMarker: Marker? = null
    private lateinit var sharedPref: SharedPreferences

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("mapCenteredX", binding.mapView.mapCenter?.latitude ?: 0.0)
        outState.putDouble("mapCenteredY", binding.mapView.mapCenter?.longitude ?: 0.0)
    }

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

                val initCenterX: Double
                val initCenterY: Double

                if (savedInstanceState != null) {
                    initCenterX = savedInstanceState.getDouble("mapCenteredX")
                    initCenterY = savedInstanceState.getDouble("mapCenteredY")

                } else {

                    initCenterX = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)))
                    initCenterY = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)))
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
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                marker.position = geoPoint

                if(!binding.mapView.overlays.contains(marker))
                    binding.mapView.overlays.add(marker)
                binding.mapView.postInvalidate();
            }
        })

        viewModel.allLocationsWithSub.observe(viewLifecycleOwner, { allLocationsWithSubs ->

            allLocationsWithSubs.forEach { locationWithSubs ->

                // add markers
                with(locationWithSubs) {

                    with(this.locationInfo) {
                        binding.mapView.addMarker(lat, lon, name)
                        binding.mapView.drawCircle(GeoPoint(lat, lon), this.delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor))
                    }

                    // todo add subitem marker ?
                    with(this.subLocation) {
                        forEach {
                            binding.mapView.drawCircle(GeoPoint(it.lat, it.lon), it.delta.toDouble(), requireContext().getColor(R.color.subLocationCircleColor))
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