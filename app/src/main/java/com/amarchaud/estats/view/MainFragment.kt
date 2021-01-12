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
import com.amarchaud.estats.R
import com.amarchaud.estats.adapter.LocationInfoItem
import com.amarchaud.estats.adapter.LocationInfoSubItem
import com.amarchaud.estats.adapter.decoration.SwipeTouchCallback
import com.amarchaud.estats.databinding.MainFragmentBinding
import com.amarchaud.estats.dialog.AddMainLocationDialog
import com.amarchaud.estats.dialog.AddSubLocationDialog
import com.amarchaud.estats.extension.*
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.viewmodel.MainViewModel
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.TouchCallback
import dagger.hilt.android.AndroidEntryPoint
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

                ItemTouchHelper(swipeCallback).attachToRecyclerView(this)
            }

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

                    if (!overlayManager.contains(myPositionMarker))
                        overlayManager.add(marker)
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
            binding.currentLatitudeValue.text = location.latitude.toString()
            binding.currentLongitudeValue.text = location.longitude.toString()
            binding.currentAltitudeValue.text = location.altitude.toString()

            // update map
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            binding.mapView.controller.animateTo(geoPoint)

            // update marker
            myPositionMarker?.let { marker ->
                marker.position = geoPoint
                if (!binding.mapView.overlays.contains(myPositionMarker))
                    binding.mapView.overlays.add(marker)
            }

            // send geoLoc to listeners
            geoPointViewModel.geoLoc.value = location
        })


        viewModel.allLocationsWithSub.observe(viewLifecycleOwner, { allLocationsWithSubs ->

            groupAdapter.clear()

            allLocationsWithSubs.forEach { locationWithSubs ->

                val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo, locationWithSubs.subLocation.size > 0)
                val expandableLocationWithSub = ExpandableGroup(header)
                locationWithSubs.subLocation.forEach {
                    expandableLocationWithSub.add(LocationInfoSubItem(it))
                }
                groupAdapter.add(expandableLocationWithSub)

                // add markers
                with(locationWithSubs) {

                    with(this.locationInfo) {
                        binding.mapView.addMarker(lat, lon, name, id)
                        binding.mapView.addCircle(GeoPoint(lat, lon), delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor), id)
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

                    val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo, locationWithSubs.subLocation.size > 0)
                    val expandableLocationWithSub = ExpandableGroup(header)
                    locationWithSubs.subLocation.forEach {
                        expandableLocationWithSub.add(LocationInfoSubItem(it))
                    }
                    groupAdapter.add(expandableLocationWithSub)

                    // add markers
                    with(locationWithSubs) {

                        with(this.locationInfo) {
                            binding.mapView.addMarker(lat, lon, name, id)
                            binding.mapView.addCircle(GeoPoint(lat, lon), delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor), id)
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
                            binding.mapView.removeMarker(id)
                            binding.mapView.removeCirle(id)
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

                    (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                        this.displayExpanded = true
                    }
                    expandableLocationWithSub.notifyItemChanged(0)

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

                    (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                        this.displayExpanded = expandableLocationWithSub.childCount > 0
                        expandableLocationWithSub.isExpanded = expandableLocationWithSub.childCount > 0
                    }
                    expandableLocationWithSub.notifyItemChanged(0)

                    // todo remove subitem marker ?
                }
            }
        })

        // just display popup
        viewModel.dialogAddMainLocation.observe(viewLifecycleOwner, { location ->
            val customPopup = AddMainLocationDialog.newInstance(location.latitude, location.longitude)
            customPopup.show(requireActivity().supportFragmentManager, "add new position")
        })
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()

        requireActivity().supportFragmentManager.setFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN, this, this)
        requireActivity().supportFragmentManager.setFragmentResultListener(AddSubLocationDialog.KEY_RESULT_SUB, this, this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()

        requireActivity().supportFragmentManager.clearFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN)
        requireActivity().supportFragmentManager.clearFragmentResultListener(AddSubLocationDialog.KEY_RESULT_SUB)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    private fun showFABMenu() {
        isFABOpen = true
        with(binding) {
            mainFloatingActionButton.animate().rotation(45f)
            addMyPositionActionButton.animate()
                .translationY(-resources.getDimension(R.dimen.floatingTranslation1))
            addCustomPositionActionButton.animate()
                .translationY(-resources.getDimension(R.dimen.floatingTranslation2))
        }
    }

    private fun closeFABMenu() {
        isFABOpen = false
        with(binding) {
            mainFloatingActionButton.animate().rotation(-45f)
            addMyPositionActionButton.animate().translationY(0.0f)
            addCustomPositionActionButton.animate().translationY(0.0f)
        }
    }


    private val swipeCallback: TouchCallback by lazy {
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
        if (requestKey == AddMainLocationDialog.KEY_RESULT_MAIN) {

            val lat = result.getDouble(AddMainLocationDialog.KEY_LAT)
            val lon = result.getDouble(AddMainLocationDialog.KEY_LON)
            val nameChoosen = result.getString(AddMainLocationDialog.KEY_NAME_RETURNED)
            val delta = result.getInt(AddMainLocationDialog.KEY_DELTA_RETURNED)
            viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, delta, null)

        } else if (requestKey == AddSubLocationDialog.KEY_RESULT_SUB) {

            val lat = result.getDouble(AddSubLocationDialog.KEY_RETURNED_LAT)
            val lon = result.getDouble(AddSubLocationDialog.KEY_RETURNED_LON)
            val nameChoosen = result.getString(AddSubLocationDialog.KEY_NAME_RETURNED)
            val idMain = result.getInt(AddSubLocationDialog.KEY_PARENT_ID)

            viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, 7, idMain)
        }

        closeFABMenu()
    }
}