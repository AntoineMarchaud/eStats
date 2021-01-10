package com.amarchaud.estats.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.*
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
import com.amarchaud.estats.popup.CurrentLocationDialog
import com.amarchaud.estats.viewmodel.MainViewModel
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.TouchCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_fragment.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class MainFragment : Fragment(), FragmentResultListener {

    companion object {
        const val TAG = "MainFragment"
    }

    private var isFABOpen: Boolean = false

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels() // replace ViewModelProvider
    private val geoPointViewModel: GeoPointViewModel by activityViewModels()

    // Marker of my position
    private var myPositionMarker: Marker? = null
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        sharedPref = requireContext().getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )


        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = MainFragmentBinding.inflate(inflater)
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
                setMultiTouchControls(false)
                controller.setZoom(15.0)
                myPositionMarker = Marker(this)

                val initCenterX: Double
                val initCenterY: Double

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
            binding.currentLatitude.text = java.lang.String.valueOf(location.latitude)
            binding.currentLongitude.text = java.lang.String.valueOf(location.longitude)

            // update map
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            mapView.controller.animateTo(geoPoint)

            myPositionMarker?.let { marker ->

                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                if (!mapView.overlays.contains(myPositionMarker))
                    mapView.overlays.add(marker)
            }

            geoPointViewModel.geoLoc.value = location
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
                        //notifyChanged() // no use for parent, only for children
                    }
                    expandableLocationWithSub.notifyItemChanged(0)
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
            val customPopup = CurrentLocationDialog.newInstance(location.latitude, location.longitude)
            requireActivity().supportFragmentManager.setFragmentResultListener(CurrentLocationDialog.KEY_RESULT, this, this) // get the result
            customPopup.show(requireActivity().supportFragmentManager, "add new position")
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

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        if (requestKey == CurrentLocationDialog.KEY_RESULT) {

            val lat = result.getDouble(CurrentLocationDialog.KEY_LAT)
            val lon = result.getDouble(CurrentLocationDialog.KEY_LON)
            val nameChoosen = result.getString(CurrentLocationDialog.KEY_NAME)
            val idMain = result.getInt(CurrentLocationDialog.KEY_ID_MAIN)

            if (idMain >= 0)
                viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, idMain)
            else
                viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, null)
            closeFABMenu()
        }
    }
}