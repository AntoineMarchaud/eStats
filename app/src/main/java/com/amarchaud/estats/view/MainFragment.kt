package com.amarchaud.estats.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.amarchaud.estats.BuildConfig
import com.amarchaud.estats.R
import com.amarchaud.estats.adapter.LocationInfoItem
import com.amarchaud.estats.adapter.LocationInfoSubItem
import com.amarchaud.estats.adapter.decoration.SwipeTouchCallback
import com.amarchaud.estats.databinding.MainFragmentBinding
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.extension.removeMarker
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.popup.CurrentLocationPopup
import com.amarchaud.estats.viewmodel.MainViewModel
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.TouchCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_fragment.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class MainFragment : Fragment(), CurrentLocationPopup.CurrentLocationDialogListener {

    companion object {
        const val TAG = "MainFragment"
    }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("mapCenteredX", binding.mapView.mapCenter?.latitude ?: 0.0)
        outState.putDouble("mapCenteredY", binding.mapView.mapCenter?.longitude ?: 0.0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding) {

            with(showMapFullScreen) {
                setOnClickListener {
                    val direction = MainFragmentDirections.actionMainFragmentToMapFragment()
                    Navigation.findNavController(view).navigate(direction)
                }
            }


            with(recyclerviewItems) {
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                adapter = groupAdapter

                // avoid blink when item is modified
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false;

                ItemTouchHelper(touchCallback).attachToRecyclerView(this)
            }

            with(mapView) {

                // map default config
                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
                setTileSource(TileSourceFactory.MAPNIK)
                //mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                setMultiTouchControls(true)
                val mapController = controller
                mapController.setZoom(15.0)
                myPositionMarker = Marker(this)

                if (savedInstanceState != null) {

                    val centerX = savedInstanceState.getDouble("mapCenteredX")
                    val centerY = savedInstanceState.getDouble("mapCenteredY")

                    overlays.remove(myPositionMarker)

                    myPositionMarker?.let { marker ->
                        val geoPoint = GeoPoint(centerX, centerY)
                        marker.position = geoPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        overlays.add(marker)
                    }
                }
            }

            with(mainFloatingActionButton) {
                setOnClickListener {
                    if (!isFABOpen) {
                        showFABMenu();
                    } else {
                        closeFABMenu();
                    }
                }
            }
        }

        // ********************** Refreshing LiveData

        // update current geoloc
        viewModel.myGeoLoc.observe(viewLifecycleOwner, { location ->

            // update value
            currentLatitude.text = java.lang.String.valueOf(location.latitude)
            currentLongitude.text = java.lang.String.valueOf(location.longitude)

            // update map
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            mapView.controller.setCenter(geoPoint)
            mapView.controller.animateTo(geoPoint)


            myPositionMarker?.let { marker ->
                mapView.overlays.remove(myPositionMarker)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)
            }
        })


        viewModel.allLocationsWithSub.observe(viewLifecycleOwner, { allLocationsWithSubs ->

            groupAdapter.clear()

            allLocationsWithSubs.forEach { locationWithSubs ->

                val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo)
                val expandableLocationWithSub = ExpandableGroup(header)
                locationWithSubs.subLocation.forEach {
                    expandableLocationWithSub.add(LocationInfoSubItem(it))
                }
                groupAdapter.add(expandableLocationWithSub)

                // add markers
                with(locationWithSubs) {

                    with(this.locationInfo) {
                        mapView.addMarker(lat, lon, name)
                    }

                    // todo add subitem marker ?
                    with(this.subLocation) {
                        forEach {

                        }
                    }
                }
            }
        })


        // at startup
        viewModel.oneLocationWithSub.observe(viewLifecycleOwner, { oneLocationWithSubs ->

            // first = LocationWithSubs
            // second = type
            // third = position
            val locationWithSubs = oneLocationWithSubs.first
            val type = oneLocationWithSubs.second
            val position = oneLocationWithSubs.third

            // update groupieView
            when (type) {
                MainViewModel.Companion.TypeItem.ITEM_INSERTED -> {

                    val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo)
                    val expandableLocationWithSub = ExpandableGroup(header)
                    locationWithSubs.subLocation.forEach {
                        expandableLocationWithSub.add(LocationInfoSubItem(it))
                    }
                    groupAdapter.add(expandableLocationWithSub)

                    // add markers
                    with(locationWithSubs) {

                        with(this.locationInfo) {
                            mapView.addMarker(lat, lon, name)
                        }

                        // todo add subitem marker ?
                        with(this.subLocation) {
                            forEach {

                            }
                        }
                    }
                }
                MainViewModel.Companion.TypeItem.ITEM_DELETED -> {
                    groupAdapter.remove(groupAdapter.getTopLevelGroup(position))

                    // remove markers
                    with(locationWithSubs) {

                        with(this.locationInfo) {
                            mapView.removeMarker(lat, lon, name)
                        }

                        // todo remove subitem marker ?
                        with(this.subLocation) {
                            forEach {

                            }
                        }
                    }
                }
            }
        })

        /**
         * Only modify
         */
        viewModel.oneLocation.observe(viewLifecycleOwner, { oneLocation ->

            // first = LocationInfo
            // second = type
            // third = position
            val locationInfo = oneLocation.first
            val type = oneLocation.second
            val position = oneLocation.third

            // update groupieView
            when (type) {
                MainViewModel.Companion.TypeHeaderItem.ITEM_MODIFIED -> {
                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(position) as ExpandableGroup
                    // expandableLocationWithSub.getGroup(0) = header
                    (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                        this.locationInfo = locationInfo
                        notifyChanged()
                    }
                }
            }
        })

        viewModel.oneSubLocation.observe(viewLifecycleOwner, { oneSubLocation ->
            // first = LocationSub
            // second = type
            // third = Pair(Index of main, index of sub)
            val locationInfoSub = oneSubLocation.first
            val type = oneSubLocation.second
            val indexMain = oneSubLocation.third.first
            val indexSub = oneSubLocation.third.second

            // update groupieView
            when (type) {
                MainViewModel.Companion.TypeSubItem.ITEM_INSERTED -> {
                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(indexMain) as ExpandableGroup
                    expandableLocationWithSub.add(LocationInfoSubItem(locationInfoSub))

                    // todo add subitem marker ?
                }
                MainViewModel.Companion.TypeSubItem.ITEM_MODIFIED -> {
                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(indexMain) as ExpandableGroup
                    // expandableLocationWithSub.getGroup(0) = header
                    (expandableLocationWithSub.getGroup(1 + indexSub) as LocationInfoSubItem).apply {
                        this.locationInfoSub = locationInfoSub
                        notifyChanged()
                    }
                }
                MainViewModel.Companion.TypeSubItem.ITEM_DELETED -> {
                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(indexMain) as ExpandableGroup

                    val groupToRemove = expandableLocationWithSub.getGroup(1 + indexSub) as LocationInfoSubItem
                    // expandableLocationWithSub.getGroup(0) = header
                    expandableLocationWithSub.remove(groupToRemove)

                    // todo remove subitem marker ?
                }
            }
        })

        // just display popup
        viewModel.popupAddCurrentPosition.observe(viewLifecycleOwner, { location ->
            val fragmentManager = requireActivity().supportFragmentManager
            val customPopup = CurrentLocationPopup(location.latitude, location.longitude, this)
            customPopup.show(fragmentManager, "add new position")
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
        lat: Double,
        lon: Double,
        nameChoosen: String,
        locationInfo: LocationInfo?
    ) {
        viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen, locationInfo)
        closeFABMenu()
    }

    override fun onCurrentLocationDialogListenerNegativeClick() {
        closeFABMenu()
    }


    private val touchCallback: TouchCallback by lazy {
        object : SwipeTouchCallback() {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                if (direction == ItemTouchHelper.LEFT) {
                    when (val item = groupAdapter.getItem(viewHolder.adapterPosition)) {
                        is LocationInfoItem -> viewModel.deleteItem(item.locationInfo)
                        is LocationInfoSubItem -> viewModel.deleteSubItem(item.locationInfoSub)
                    }
                }
            }
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
}