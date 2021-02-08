package com.amarchaud.estats.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.MapFragmentBinding
import com.amarchaud.estats.databinding.SplashFragmentBinding
import com.amarchaud.estats.viewmodel.SplashViewModel

class SplashFragment : Fragment() {

    private var _binding: SplashFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = SplashFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.actionLiveData.observe(viewLifecycleOwner, {
            Navigation.findNavController(view).navigate(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}