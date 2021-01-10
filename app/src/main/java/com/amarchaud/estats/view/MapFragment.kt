package com.amarchaud.estats.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint

class MapFragment : Fragment() {

    companion object {
        const val TAG = "MapFragment"
    }

    private lateinit var binding: MapFragmentBinding
    private val viewModel: MapViewModel by viewModels() // replace ViewModelProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MapFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapViewModel = viewModel
        binding.lifecycleOwner = this

        with(binding) {

            /*
            centerView.setOnClickListener {
                viewModel.myGeoLoc.value?.apply {
                    val geoPoint = GeoPoint(latitude, longitude)
                    mapView.controller.setCenter(geoPoint)
                }
            }*/
        }
    }
}