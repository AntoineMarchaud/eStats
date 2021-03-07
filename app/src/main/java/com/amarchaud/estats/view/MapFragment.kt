package com.amarchaud.estats.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.amarchaud.estats.R
import com.amarchaud.estats.adapter.LocationInfoItem
import com.amarchaud.estats.adapter.LocationInfoSubItem
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.dialog.AddMainLocationDialog
import com.amarchaud.estats.extension.addCircle
import com.amarchaud.estats.extension.addMarker
import com.amarchaud.estats.extension.createCircle
import com.amarchaud.estats.extension.initMapView
import com.amarchaud.estats.viewmodel.MapViewModel
import com.amarchaud.estats.viewmodel.data.NewPositionViewModel
import com.amarchaud.estats.viewmodel.data.NumberPickerViewModel
import com.xwray.groupie.ExpandableGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


@AndroidEntryPoint
class MapFragment : Fragment() {

    companion object {
        const val MODE = "Mode"

        const val MODE_NORMAL = 0 // no circle
        const val MODE_MAIN = 1 // add red circle around my position
        const val MODE_SUB = 2 // add green circle around my position

        const val MODE_MAIN_CUSTOM_POSITION = 3 // like MODE_MAIN, but lon / lat is fixed
        const val LAT_FIXED = "LAT_FIXED"
        const val LON_FIXED = "LON_FIXED"

        // for classic Fragment declaration
        fun newInstance(mode: Int = MODE_NORMAL): MapFragment {

            val fragment = MapFragment()

            val args = Bundle()
            args.putInt(MODE, mode)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(mode: Int = MODE_MAIN_CUSTOM_POSITION, lat: Double, lon: Double): MapFragment {

            val fragment = MapFragment()

            val args = Bundle()
            args.putInt(MODE, mode)
            args.putDouble(LAT_FIXED, lat)
            args.putDouble(LON_FIXED, lon)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    // custome viewmodel
    private val newPositionViewModel: NewPositionViewModel by activityViewModels()
    private val numberPickerViewModel: NumberPickerViewModel by activityViewModels()

    private var myPositionCircle: Polygon? = null
    private var myPositionMarker: Marker? = null
    private lateinit var sharedPref: SharedPreferences

    private var initCenterX: Double = 0.0
    private var initCenterY: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(AddMainLocationDialog.KEY_RESULT_MAIN) { _, bundle ->

            println("CONNARD 2")

            /*
            val lat = bundle.getDouble(AddMainLocationDialog.KEY_LAT_RETURNED)
            val lon = bundle.getDouble(AddMainLocationDialog.KEY_LON_RETURNED)
            val nameChoosen = bundle.getString(AddMainLocationDialog.KEY_NAME_RETURNED)
            val delta = bundle.getInt(AddMainLocationDialog.KEY_DELTA_RETURNED)

            lifecycleScope.launch {
                // add in DB
                viewModel.onAddNewPosition(lat, lon, nameChoosen!!, delta).collect {
                    binding.mapView.addMarker(lat, lon, nameChoosen, id)
                    binding.mapView.addCircle(GeoPoint(lat, lon), delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor), id)
                }
            }*/
        }
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

                with(requireArguments()) {

                    when (getInt(MODE)) {
                        MODE_MAIN_CUSTOM_POSITION -> {
                            initCenterX = getDouble(LAT_FIXED)
                            initCenterY = getDouble(LON_FIXED)
                        }
                        MODE_NORMAL -> {
                            // add click listener
                            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    val customPopup = AddMainLocationDialog.newInstance(p.latitude, p.longitude)
                                    customPopup.show(parentFragmentManager, "add new position")
                                    return false
                                }

                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    return false
                                }
                            }
                            overlayManager.add(MapEventsOverlay(mReceive))

                            initCenterX = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)))
                            initCenterY = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)))
                        }
                        else -> {
                            initCenterX = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lat), java.lang.Double.doubleToLongBits(0.0)))
                            initCenterY = java.lang.Double.longBitsToDouble(sharedPref.getLong(requireContext().getString(R.string.saved_location_lon), java.lang.Double.doubleToLongBits(0.0)))
                        }
                    }
                    initMapView(GeoPoint(initCenterX, initCenterY))
                }

                myPositionMarker = Marker(this)
                myPositionMarker?.let { marker ->
                    val geoPoint = GeoPoint(initCenterX, initCenterY)
                    marker.position = geoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    if (!overlayManager.contains(myPositionMarker))
                        overlayManager.add(marker)
                }

                when (requireArguments().getInt(MODE)) {
                    MODE_MAIN_CUSTOM_POSITION -> {
                        myPositionCircle =
                            createCircle(
                                GeoPoint(initCenterX, initCenterY),
                                numberPickerViewModel.pickerValueMutableLiveData.value?.toDouble() ?: 0.0, requireContext().getColor(R.color.mainLocationCircleColor), -1
                            )
                    }
                    MODE_MAIN -> {
                        myPositionCircle =
                            createCircle(
                                GeoPoint(initCenterX, initCenterY),
                                numberPickerViewModel.pickerValueMutableLiveData.value?.toDouble() ?: 0.0, requireContext().getColor(R.color.mainLocationCircleColor), -1
                            )
                    }
                    MODE_SUB -> {
                        myPositionCircle =
                            createCircle(
                                GeoPoint(initCenterX, initCenterY), 7.toDouble(), requireContext().getColor(R.color.subLocationCircleColor), -1
                            )
                    }
                }
            }

            with(centerView) {
                setOnClickListener {
                    with(requireArguments()) {
                        if (getInt(MODE) == MODE_MAIN_CUSTOM_POSITION) {
                            val geoPoint = GeoPoint(initCenterX, initCenterY)
                            mapView.controller.animateTo(geoPoint)
                        } else {
                            viewModel.myGeoLoc.value?.apply {
                                val geoPoint = GeoPoint(latitude, longitude)
                                mapView.controller.animateTo(geoPoint)
                            }
                        }
                    }
                }
            }
        }

        with(requireArguments()) {
            val mode = getInt(MODE)

            if (mode != MODE_MAIN_CUSTOM_POSITION) {

                // update current geoloc
                viewModel.myGeoLoc.observe(viewLifecycleOwner, { location ->
                    // update map
                    myPositionMarker?.let { marker ->
                        marker.position = GeoPoint(location.latitude, location.longitude)
                        if (!binding.mapView.overlayManager.contains(marker))
                            binding.mapView.overlayManager.add(marker)
                    }

                    when (mode) {
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
            }
        }

        if (requireArguments().getInt(MODE) != MODE_NORMAL) {
            numberPickerViewModel.pickerValueMutableLiveData.observe(viewLifecycleOwner, {
                // update map
                with(requireArguments()) {
                    if (getInt(MODE) == MODE_MAIN_CUSTOM_POSITION) {
                        myPositionCircle?.points = Polygon.pointsAsCircle(GeoPoint(initCenterX, initCenterY), it.toDouble())
                    } else {
                        myPositionCircle?.points =
                            Polygon.pointsAsCircle(GeoPoint(viewModel.myGeoLoc.value?.latitude ?: initCenterX, viewModel.myGeoLoc.value?.longitude ?: initCenterY), it.toDouble())
                    }
                }

                if (!binding.mapView.overlayManager.contains(myPositionCircle))
                    binding.mapView.overlayManager.add(myPositionCircle)
                binding.mapView.invalidate()
            })
        }

        viewModel.allLocationsWithSub.observe(viewLifecycleOwner, { allLocationsWithSubs ->

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



        newPositionViewModel.newPositionLiveData.observe(viewLifecycleOwner, {
            with(it) {
                lifecycleScope.launch {
                    viewModel.onAddNewPosition(lat, lon, name, delta).collect {
                        binding.mapView.addMarker(lat, lon, name, id)
                        binding.mapView.addCircle(GeoPoint(lat, lon), delta.toDouble(), requireContext().getColor(R.color.mainLocationCircleColor), id)
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