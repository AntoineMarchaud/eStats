package com.amarchaud.estats.view

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
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
import com.amarchaud.estats.dialog.ListContactDialog
import com.amarchaud.estats.extension.*
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.model.entity.LocationWithSubs
import com.amarchaud.estats.model.other.Contact
import com.amarchaud.estats.viewmodel.MainViewModel
import com.amarchaud.estats.viewmodel.data.GeoPointViewModel
import com.amarchaud.estats.viewmodel.data.NewPositionViewModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.TouchCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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

    // custom viewModel
    private val newPositionViewModel: NewPositionViewModel by activityViewModels()
    private val geoPointViewModel: GeoPointViewModel by activityViewModels()

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

            addCustomPositionActionButton.setOnClickListener {
                requestContacts()
            }

            addMyPositionActionButton.setOnClickListener {
                if (viewModel.mPositionService?.geoLoc == null) {
                    Toast.makeText(requireContext(), getString(R.string.activateGPS), Toast.LENGTH_LONG).show()
                } else {
                    val customPopup = AddMainLocationDialog.newInstance()
                    customPopup.show(requireActivity().supportFragmentManager, "add new position")
                }
            }

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

            // send geoLoc to listeners (dialogs)
            geoPointViewModel.setGeloc(location)
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
            }
        })


        /**
         * Only modify
         */
        viewModel.oneLocationToModify.observe(viewLifecycleOwner, { oneLocation ->

            val locationInfo = oneLocation.first
            val position = oneLocation.second

            // update groupieView
            val expandableLocationWithSub = groupAdapter.getTopLevelGroup(position) as ExpandableGroup
            // expandableLocationWithSub.getGroup(0) = header
            (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                this.locationInfo = locationInfo
                this.typeIndexDisplayed = pickerValueIndexStored
                //notifyChanged() // no use for parent, only for children
            }
            expandableLocationWithSub.notifyItemChanged(0)
        })

        viewModel.oneSubLocationToModify.observe(viewLifecycleOwner, { oneSubLocation ->

            val locationInfoSub = oneSubLocation.first
            val indexMain = oneSubLocation.second
            val indexSub = oneSubLocation.third

            // update groupieView
            val expandableLocationWithSub = groupAdapter.getTopLevelGroup(indexMain) as ExpandableGroup
            // expandableLocationWithSub.getGroup(0) = header
            (expandableLocationWithSub.getGroup(1 + indexSub) as LocationInfoSubItem).apply {
                this.locationInfoSubParam = locationInfoSub
                this.typeIndexDisplayed = pickerValueIndexStored
                notifyChanged()
            }
        })


        // custom ViewModel when adding from DialogFragment !
        newPositionViewModel.newPositionLiveData.observe(viewLifecycleOwner, {
            with(it) {
                lifecycleScope.launch {

                    viewModel.onAddNewPosition(lat, lon, name, delta).collect { oneLocationWithSub ->
                        with(oneLocationWithSub) {
                            val header = LocationInfoItem(
                                this@MainFragment,
                                locationInfo,
                                subLocation.size > 0, pickerValueIndexStored
                            )
                            val expandableLocationWithSub = ExpandableGroup(header)
                            subLocation.forEach {
                                expandableLocationWithSub.add(LocationInfoSubItem(it, pickerValueIndexStored))
                            }
                            groupAdapter.add(expandableLocationWithSub)
                        }
                    }
                }

            }
        })
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()

        //requireActivity().supportFragmentManager.setFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN, this, this)
        requireActivity().supportFragmentManager.setFragmentResultListener(AddSubLocationDialog.KEY_RESULT_SUB, this, this)
        requireActivity().supportFragmentManager.setFragmentResultListener(ListContactDialog.KEY_RESULT_CONTACT, this, this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()

        //requireActivity().supportFragmentManager.clearFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN)
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

                        is LocationInfoItem -> {
                            lifecycleScope.launch {

                                //delete db
                                viewModel.deleteItem(item.locationInfo)

                                //delete ui
                                groupAdapter.remove(groupAdapter.getTopLevelGroup(viewHolder.adapterPosition))
                            }
                        }
                        is LocationInfoSubItem -> {

                            // delete db
                            lifecycleScope.launch {

                                viewModel.deleteSubItem(item.locationInfoSubParam).collect {
                                    val mainPos = it.first
                                    val subPos = it.second

                                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(mainPos) as ExpandableGroup
                                    val groupToRemove = expandableLocationWithSub.getGroup(1 + subPos) as LocationInfoSubItem
                                    expandableLocationWithSub.remove(groupToRemove)

                                    (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                                        this.displayExpanded = expandableLocationWithSub.childCount > 0
                                        expandableLocationWithSub.isExpanded = expandableLocationWithSub.childCount > 0
                                    }
                                    expandableLocationWithSub.notifyItemChanged(0)
                                }
                            }
                        }
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

            lifecycleScope.launch {

                // add in DB
                viewModel.onAddNewPosition(lat, lon, nameChoosen!!, delta).collect { oneLocationWithSub ->
                    with(oneLocationWithSub) {
                        val header = LocationInfoItem(
                            this@MainFragment,
                            locationInfo,
                            subLocation.size > 0, pickerValueIndexStored
                        )
                        val expandableLocationWithSub = ExpandableGroup(header)
                        subLocation.forEach {
                            expandableLocationWithSub.add(LocationInfoSubItem(it, pickerValueIndexStored))
                        }
                        groupAdapter.add(expandableLocationWithSub)
                    }
                }
            }

        } else if (requestKey == AddSubLocationDialog.KEY_RESULT_SUB) {

            val lat = result.getDouble(AddSubLocationDialog.KEY_LAT_RETURNED)
            val lon = result.getDouble(AddSubLocationDialog.KEY_LON_RETURNED)
            val nameChoosen = result.getString(AddSubLocationDialog.KEY_NAME_RETURNED)
            val idMain = result.getInt(AddSubLocationDialog.KEY_PARENT_ID)

            lifecycleScope.launch {
                // add in DB
                viewModel.onAddNewPositionSub(lat, lon, nameChoosen!!, 7, idMain).collect { oneSub ->

                    val sub = oneSub.first
                    val posMain = oneSub.second

                    // then add visually !
                    val expandableLocationWithSub = groupAdapter.getTopLevelGroup(posMain) as ExpandableGroup
                    expandableLocationWithSub.add(LocationInfoSubItem(sub, pickerValueIndexStored))

                    (expandableLocationWithSub.getGroup(0) as LocationInfoItem).apply {
                        this.displayExpanded = true
                    }
                    expandableLocationWithSub.notifyItemChanged(0)

                }
            }

        } else if (requestKey == ListContactDialog.KEY_RESULT_CONTACT) {

            // get all contacts
            val allContacts = result.getParcelableArrayList<Contact>(ListContactDialog.KEY_RESULT_LIST_CONTACT)
            if (allContacts != null) {

                lifecycleScope.launch {
                    viewModel.onAddContacts(allContacts).collect {
                        // add each person !
                        val header = LocationInfoItem(
                            this@MainFragment,
                            it.locationInfo,
                            it.subLocation.size > 0,
                            pickerValueIndexStored
                        )
                        val expandableLocationWithSub = ExpandableGroup(header)
                        it.subLocation.forEach {
                            expandableLocationWithSub.add(LocationInfoSubItem(it, pickerValueIndexStored))
                        }
                        groupAdapter.add(expandableLocationWithSub)
                    }
                }

            }
        }

        closeFABMenu()
    }

    private fun requestContacts() {

        Dexter
            .withContext(requireContext())
            .withPermission(Manifest.permission.READ_CONTACTS)
            .withListener(object : PermissionListener {

                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val customPopup = ListContactDialog.newInstance()
                    customPopup.show(requireActivity().supportFragmentManager, "show contact")
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(requireContext(), "need contact permission", Toast.LENGTH_LONG).show()
                }

                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                    p1?.continuePermissionRequest()
                }

            }).check()
    }
}