package com.amarchaud.estats.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amarchaud.estats.BuildConfig
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.MainFragmentBinding
import com.amarchaud.estats.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_fragment.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


@AndroidEntryPoint
class MainFragment : Fragment() {

    private var isFABOpen : Boolean = false
    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels() // hilt

    // Marker of my position
    private var myPositionMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.show()

        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainViewModel = viewModel

        // map default config
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        //mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        myPositionMarker = Marker(mapView)

        viewModel.myGeoLoc.observe(viewLifecycleOwner, { location ->

            // update value
            currentLatitude.text = java.lang.String.valueOf(location.latitude)
            currentLongitude.text = java.lang.String.valueOf(location.longitude)

            // update map
            val geoPoint = GeoPoint(location.latitude, location.longitude)

            mapView.controller.setCenter(geoPoint)
            mapView.controller.animateTo(geoPoint)
            mapView.overlays.remove(myPositionMarker)

            myPositionMarker?.let { marker ->
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)
            }
        })


        viewModel.locations.observe(viewLifecycleOwner, { location ->
            location.forEach {
                val oneMarker: Marker = Marker(mapView)
                oneMarker.position = GeoPoint(it.lat, it.lon)
                oneMarker.title = it.name
                //oneMarker.icon = ResourcesCompat.getDrawable(resources, R.drawable.bonuspack_bubble, null)
                oneMarker.setTextIcon("text icon ?")
                mapView.overlays.add(oneMarker)

                // todo
                // draw circle around the marker
                val circle: List<GeoPoint> =
                    Polygon.pointsAsCircle(oneMarker.position, it.delta.toDouble())
                val p = Polygon(mapView)
                p.points = circle
                p.title = "A circle"
                val fillPaint = p.fillPaint
                fillPaint.color = 0xFF0000
                mapView.overlayManager.add(p)
                mapView.invalidate()
            }
        })

        mainFloatingActionButton.setOnClickListener {
            if(!isFABOpen){
                showFABMenu();
            }else{
                closeFABMenu();
            }
        }
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun showFABMenu() {
        isFABOpen = true
        mainFloatingActionButton.animate().rotation(45f)
        addMyPositionActionButton.animate().translationY(-resources.getDimension(R.dimen.floatingTranslation1))
        addCustomPositionActionButton.animate().translationY(-resources.getDimension(R.dimen.floatingTranslation2))
    }

    private fun closeFABMenu() {
        isFABOpen = false
        mainFloatingActionButton.animate().rotation(-45f)
        addMyPositionActionButton.animate().translationY(0.0f)
        addCustomPositionActionButton.animate().translationY(0.0f)
    }
}