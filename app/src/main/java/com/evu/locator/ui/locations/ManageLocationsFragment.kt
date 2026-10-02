package com.evu.locator.ui.locations

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.LocalStationStore
import com.evu.locator.data.models.Station
import com.evu.locator.databinding.FragmentManageLocationsBinding
import com.evu.locator.geofence.GeofenceHelper
import com.google.android.gms.location.LocationServices

private const val DEFAULT_RADIUS = 360f

class ManageLocationsFragment : Fragment() {

    private var _binding: FragmentManageLocationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: StationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = StationAdapter(
            onEdit = { showEditDialog(it, null) },
            onDelete = {
                LocalStationStore.delete(requireContext(), it.id)
                refreshList()
            },
            onOverride = { cloudStation ->
                // create an editable local copy that overrides this cloud station
                showEditDialog(cloudStation.copy(id = ""), cloudStation.id)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.currentLocationBtn.setOnClickListener { useCurrentLocation() }
        binding.addManuallyBtn.setOnClickListener {
            showEditDialog(Station(id = "", name = "", lat = 0.0, lng = 0.0, radius = DEFAULT_RADIUS), null)
        }

        refreshList()
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                showEditDialog(
                    Station(id = "", name = "New location", lat = location.latitude, lng = location.longitude, radius = DEFAULT_RADIUS),
                    null
                )
            }
        }
    }

    private fun showEditDialog(initial: Station, overridesCloudId: String?) {
        EditStationDialogFragment.newInstance(initial, overridesCloudId).show(childFragmentManager, "editStation")
    }

    fun refreshList() {
        val local = LocalStationStore.getAll(requireContext())
        val overriddenIds = local.mapNotNull { it.overridesCloudId }.toSet()

        FirestoreRepo.fetchCloudStations { cloud ->
            val visibleCloud = cloud.filter { it.id !in overriddenIds }
            val display = local.map { DisplayStation(it, isCloud = false) } +
                    visibleCloud.map { DisplayStation(it, isCloud = true) }
            adapter.submit(display)
        }

        GeofenceHelper(requireContext()).registerAllStations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}