package com.amarchaud.estats.view

import android.animation.Animator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.*
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
import com.amarchaud.estats.dialog.Contact
import com.amarchaud.estats.dialog.ListContactDialog
import com.amarchaud.estats.extension.*
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.utils.GeoCoder
import com.amarchaud.estats.viewmodel.MainViewModel
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.TouchCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


@AndroidEntryPoint
class MainFragment : Fragment(), FragmentResultListener {

    companion object {

        //const val TAG = "MainFragment"
        const val KEY_INDEX_PICKER = "KEY_INDEX_PICKER"

        enum class DurationType(val value: Int) {
            DAY(0), WEEK(1), MONTH(2), YEAR(3), ALL_TIME(4)
        }

        val valuesPicker = mutableListOf("Day", "Week", "Month", "Year", "All Time")
    }

    private var pickerValueIndexStored = 0

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
        outState.putInt(KEY_INDEX_PICKER, pickerValueIndexStored) // index !
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding) {

            /*
            with(showMapFullScreen) {
                setOnClickListener {
                    val direction = MainFragmentDirections.actionMainFragmentToMapFragment()
                    Navigation.findNavController(view).navigate(direction)
                }
            }*/


            with(recyclerviewItems) {
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                adapter = groupAdapter

                // avoid blink when item is modified
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

                ItemTouchHelper(swipeCallback).attachToRecyclerView(this)
            }

            with(typeDisplayedPicker) {

                minValue = 0
                maxValue = valuesPicker.size - 1
                displayedValues = valuesPicker.toTypedArray()

                setOnValueChangedListener { _, _, i2 ->
                    // i2 = index !
                    pickerValueIndexStored = i2

                    // refresh everything !
                    groupAdapter.notifyDataSetChanged()
                }

                pickerValueIndexStored = savedInstanceState?.getInt(KEY_INDEX_PICKER) ?: 0
                typeDisplayedPicker.value = pickerValueIndexStored
            }


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
            }

            with(mainFloatingActionButton) {
                setOnClickListener {
                    if (!isFABOpen) {
                        showFABMenu()
                    } else {
                        closeFABMenu()
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

                val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo, locationWithSubs.subLocation.size > 0, pickerValueIndexStored)
                val expandableLocationWithSub = ExpandableGroup(header)
                locationWithSubs.subLocation.forEach {
                    expandableLocationWithSub.add(LocationInfoSubItem(it, pickerValueIndexStored))
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

                    val header = LocationInfoItem(this@MainFragment, locationWithSubs.locationInfo, locationWithSubs.subLocation.size > 0, pickerValueIndexStored)
                    val expandableLocationWithSub = ExpandableGroup(header)
                    locationWithSubs.subLocation.forEach {
                        expandableLocationWithSub.add(LocationInfoSubItem(it, pickerValueIndexStored))
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
                        this.typeIndexDisplayed = pickerValueIndexStored
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
                    expandableLocationWithSub.add(LocationInfoSubItem(locationInfoSub, pickerValueIndexStored))

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
                        this.locationInfoSubParam = locationInfoSub
                        this.typeIndexDisplayed = pickerValueIndexStored
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
        viewModel.displayDialog.observe(viewLifecycleOwner, {
            if (it == AddMainLocationDialog::class.simpleName) {
                val customPopup = AddMainLocationDialog.newInstance()
                customPopup.show(requireActivity().supportFragmentManager, "add new position")
            } else if (it == ListContactDialog::class.simpleName) {
                val customPopup = ListContactDialog.newInstance()
                customPopup.show(requireActivity().supportFragmentManager, "show contact")
            }
        })
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()

        requireActivity().supportFragmentManager.setFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN, this, this)
        requireActivity().supportFragmentManager.setFragmentResultListener(AddSubLocationDialog.KEY_RESULT_SUB, this, this)
        requireActivity().supportFragmentManager.setFragmentResultListener(ListContactDialog.KEY_RESULT_CONTACT, this, this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()

        requireActivity().supportFragmentManager.clearFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN)
        requireActivity().supportFragmentManager.clearFragmentResultListener(AddSubLocationDialog.KEY_RESULT_SUB)
        requireActivity().supportFragmentManager.clearFragmentResultListener(ListContactDialog.KEY_RESULT_CONTACT)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    private fun showFABMenu() {
        isFABOpen = true
        with(binding) {
            mainFloatingActionButton.animate().rotation(45f)
            addMyPositionActionButton.animate().translationY(-resources.getDimension(R.dimen.floatingTranslation1)).alpha(1f).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    addMyPositionActionButton.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(p0: Animator?) {

                }

                override fun onAnimationCancel(p0: Animator?) {

                }

                override fun onAnimationRepeat(p0: Animator?) {

                }

            })
            addCustomPositionActionButton.animate().translationY(-resources.getDimension(R.dimen.floatingTranslation2)).alpha(1f).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    addCustomPositionActionButton.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(p0: Animator?) {

                }

                override fun onAnimationCancel(p0: Animator?) {

                }

                override fun onAnimationRepeat(p0: Animator?) {

                }
            })
        }
    }

    private fun closeFABMenu() {
        isFABOpen = false
        with(binding) {
            mainFloatingActionButton.animate().rotation(-45f)
            addMyPositionActionButton.animate().translationY(0.0f).alpha(0f).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {

                }

                override fun onAnimationEnd(p0: Animator?) {
                    addMyPositionActionButton.visibility = View.GONE
                }

                override fun onAnimationCancel(p0: Animator?) {

                }

                override fun onAnimationRepeat(p0: Animator?) {

                }
            })
            addCustomPositionActionButton.animate().translationY(0.0f).alpha(0f).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {

                }

                override fun onAnimationEnd(p0: Animator?) {
                    addCustomPositionActionButton.visibility = View.GONE
                }

                override fun onAnimationCancel(p0: Animator?) {

                }

                override fun onAnimationRepeat(p0: Animator?) {

                }
            })
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
                        is LocationInfoSubItem -> viewModel.deleteSubItem(item.locationInfoSubParam)
                    }
                }
            }
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        if (requestKey == AddMainLocationDialog.KEY_RESULT_MAIN) {

            val lat = result.getDouble(AddMainLocationDialog.KEY_LAT_RETURNED)
            val lon = result.getDouble(AddMainLocationDialog.KEY_LON_RETURNED)
            val nameChoosen = result.getString(AddMainLocationDialog.KEY_NAME_RETURNED)
            val delta = result.getInt(AddMainLocationDialog.KEY_DELTA_RETURNED)
            viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, delta, null)

        } else if (requestKey == AddSubLocationDialog.KEY_RESULT_SUB) {

            val lat = result.getDouble(AddSubLocationDialog.KEY_LAT_RETURNED)
            val lon = result.getDouble(AddSubLocationDialog.KEY_LON_RETURNED)
            val nameChoosen = result.getString(AddSubLocationDialog.KEY_NAME_RETURNED)
            val idMain = result.getInt(AddSubLocationDialog.KEY_PARENT_ID)

            viewModel.onCurrentLocationDialogPositiveClick(lat, lon, nameChoosen!!, 7, idMain)
        } else if (requestKey == ListContactDialog.KEY_RESULT_CONTACT) {

            // get all contacts
            val allContacts = result.getParcelableArrayList<Contact>(ListContactDialog.KEY_RESULT_LIST_CONTACT)
            if (allContacts != null) {
                viewModel.onAddContacts(allContacts)
            }
        }

        closeFABMenu()
    }
}