package com.amarchaud.estats.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amarchaud.estats.BuildConfig
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_fragment.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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

                // map default config
                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
                setTileSource(TileSourceFactory.MAPNIK)
                mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                setMultiTouchControls(true)
                val mapController = controller
                mapController.setZoom(15.0)
                myPositionMarker = Marker(this)

                var initCenterX = 0.0
                var initCenterY = 0.0

                if (savedInstanceState != null) {
                    initCenterX = savedInstanceState.getDouble("mapCenteredX")
                    initCenterY = savedInstanceState.getDouble("mapCenteredY")

                } else {

                    initCenterX = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)))
                    initCenterY = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)))
                }

                setExpectedCenter(GeoPoint(initCenterX, initCenterY))

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
            val geoPoint = GeoPoint(location.latitude, location.longitude)

            myPositionMarker?.let { marker ->

                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                if (!mapView.overlays.contains(myPositionMarker))
                    mapView.overlays.add(marker)
            }
        })

        viewModel.allLocationsWithSub.observe(viewLifecycleOwner, { allLocationsWithSubs ->

            allLocationsWithSubs.forEach { locationWithSubs ->

                // add markers
                with(locationWithSubs) {

                    with(this.locationInfo) {
                        mapView.addMarker(lat, lon, name)

                        DrawCirleAroundPosition(GeoPoint(lat,lon), this.delta.toDouble(), 0x00FF00)
                    }

                    // todo add subitem marker ?
                    with(this.subLocation) {
                        forEach {
                            DrawCirleAroundPosition(GeoPoint(it.lat,it.lon), it.delta.toDouble(),  0xFF0000)
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

    private fun DrawCirleAroundPosition(geoLocPos : GeoPoint, radiusInMeters : Double, color : Int) {
        val circle: List<GeoPoint> = Polygon.pointsAsCircle(geoLocPos, radiusInMeters)
        val p = Polygon(mapView)
        p.points = circle
        p.title = "A circle"
        val fillPaint = p.fillPaint
        fillPaint.color = color
        mapView.overlayManager.add(p)
        mapView.invalidate()
    }
}