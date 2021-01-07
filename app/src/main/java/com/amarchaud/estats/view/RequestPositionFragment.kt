package com.amarchaud.estats.view

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.amarchaud.estats.R
import com.amarchaud.estats.application.eStatsApplication
import com.amarchaud.estats.databinding.RequestPositionFragmentBinding
import com.amarchaud.estats.viewmodel.RequestPositionViewModel

class RequestPositionFragment : Fragment() {

    private lateinit var binding: RequestPositionFragmentBinding
    private lateinit var viewModel: RequestPositionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.show()
        binding = RequestPositionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(RequestPositionViewModel::class.java)
        binding.requestPositionViewModel = viewModel

        viewModel.actionLiveData.observe(viewLifecycleOwner, {
            Navigation.findNavController(view).navigate(it)
        })

        viewModel.actionShowSettingsDialog.observe(viewLifecycleOwner, {
            showSettingsDialog()
        })
    }

    private fun showSettingsDialog() {

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.permissionGpsTitle)
        builder.setMessage(R.string.permissionGpsMessage)
        builder.setPositiveButton(R.string.permissionGpsOk) { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(R.string.permissionGpsCancel) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}