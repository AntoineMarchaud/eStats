package com.amarchaud.estats.view

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.amarchaud.estats.BuildConfig
import com.amarchaud.estats.R
import com.amarchaud.estats.adapter.LocationInfoItem
import com.amarchaud.estats.adapter.LocationInfoSubItem
import com.amarchaud.estats.databinding.MainFragmentBinding
import com.amarchaud.estats.popup.CurrentLocationPopup
import com.amarchaud.estats.viewmodel.MainViewModel
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_fragment.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class MainFragment : Fragment(), CurrentLocationPopup.CurrentLocationDialogListener {

    private var isFABOpen: Boolean = false
    private lateinit var binding: MainFragmentBinding

    private val viewModel: MainViewModel by viewModels() // replace ViewModelProvider

    // Marker of my position
    private var myPositionMarker: Marker? = null

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.show()

        binding = MainFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding.recyclerviewItems) {
            layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

            // pour eviter le blink quand item est modifié
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false;
            adapter = groupAdapter
        }


        // map default config
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        //mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        myPositionMarker = Marker(mapView)

        // update current geoloc
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

        viewModel.oneLocation.observe(viewLifecycleOwner, { location ->

            // first = LocationWithSubs
            // second = type
            // third = position

            // principal
            with(location.first) {

                with(this.locationInfo) {
                    val oneMarker: Marker = Marker(mapView)
                    oneMarker.position = GeoPoint(lat, lon)
                    oneMarker.title = name
                    oneMarker.setTextIcon(name) // displayed on screen
                    mapView.overlays.add(oneMarker)
                }

                // secondaire
                with(this.subLocation) {
                    forEach {
                        // todo
                    }
                }
            }

            // update groupieView
            when (location.second) {
                MainViewModel.Companion.typeItem.ITEM_INSERTED -> {

                    val header =  LocationInfoItem(this, location.first.locationInfo)
                    val expandableLocationWithSub = ExpandableGroup(header)
                    location.first.subLocation.forEach {
                        expandableLocationWithSub.add(LocationInfoSubItem(it))
                    }
                    groupAdapter.add(expandableLocationWithSub)
                    groupAdapter.notifyItemInserted(location.third)
                }
                MainViewModel.Companion.typeItem.ITEM_MODIFIED -> {
                    (groupAdapter.getItem(location.third) as LocationInfoItem).locationInfo =
                        location.first.locationInfo
                    groupAdapter.notifyItemChanged(location.third)
                }
                MainViewModel.Companion.typeItem.ITEM_DELETED -> {
                    //groupAdapter.remove
                    groupAdapter.notifyItemChanged(location.third)
                }
            }


            // todo
            /*
            // draw circle around the marker
            val circle: List<GeoPoint> =
                Polygon.pointsAsCircle(oneMarker.position, it.delta.toDouble())
            val p = Polygon(mapView)
            p.points = circle
            p.title = "A circle"
            val fillPaint = p.fillPaint
            fillPaint.color = 0xFF0000
            mapView.overlayManager.add(p)
            mapView.invalidate()*/


        })

        // just display popup
        viewModel.popupAddCurrentPosition.observe(viewLifecycleOwner, { location ->
            val fragmentManager = requireActivity().supportFragmentManager
            val customPopup = CurrentLocationPopup(location, this)
            customPopup.show(fragmentManager, "add new position")
        })

        mainFloatingActionButton.setOnClickListener {
            if (!isFABOpen) {
                showFABMenu();
            } else {
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
        addMyPositionActionButton.animate()
            .translationY(-resources.getDimension(R.dimen.floatingTranslation1))
        addCustomPositionActionButton.animate()
            .translationY(-resources.getDimension(R.dimen.floatingTranslation2))
    }

    private fun closeFABMenu() {
        isFABOpen = false
        mainFloatingActionButton.animate().rotation(-45f)
        addMyPositionActionButton.animate().translationY(0.0f)
        addCustomPositionActionButton.animate().translationY(0.0f)
    }

    /**
     * Callbacks of Popup
     */
    override fun onCurrentLocationDialogPositiveClick(
        currentLocation: Location,
        nameChoosen: String
    ) {
        viewModel.onCurrentLocationDialogPositiveClick(currentLocation, nameChoosen)
        closeFABMenu()
    }

    override fun onCurrentLocationDialogListenerNegativeClick() {
        closeFABMenu()
    }
}