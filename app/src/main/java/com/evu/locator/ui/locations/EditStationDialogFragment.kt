package com.evu.locator.ui.locations

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.evu.locator.data.LocalStationStore
import com.evu.locator.data.models.Station
import com.evu.locator.databinding.DialogEditStationBinding
import com.evu.locator.geofence.GeofenceHelper
import com.evu.locator.utils.cleanTrim
import com.evu.locator.utils.isBlankAfterClean
import com.google.android.gms.location.LocationServices
import java.util.UUID

class EditStationDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"
        private const val ARG_LAT = "lat"
        private const val ARG_LNG = "lng"
        private const val ARG_RADIUS = "radius"
        private const val ARG_OVERRIDES_CLOUD_ID = "overridesCloudId"

        fun newInstance(initial: Station, overridesCloudId: String? = null): EditStationDialogFragment {
            val fragment = EditStationDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_ID, initial.id)
                putString(ARG_NAME, initial.name)
                putDouble(ARG_LAT, initial.lat)
                putDouble(ARG_LNG, initial.lng)
                putFloat(ARG_RADIUS, initial.radius)
                putString(ARG_OVERRIDES_CLOUD_ID, overridesCloudId)
            }
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val overridesCloudId = args.getString(ARG_OVERRIDES_CLOUD_ID)
        val initial = Station(
            id = args.getString(ARG_ID, ""),
            name = args.getString(ARG_NAME, ""),
            lat = args.getDouble(ARG_LAT, 0.0),
            lng = args.getDouble(ARG_LNG, 0.0),
            radius = args.getFloat(ARG_RADIUS, 360f)
        )

        val binding = DialogEditStationBinding.inflate(layoutInflater)
        binding.nameInput.setText(initial.name)
        binding.latInput.setText(initial.lat.toString())
        binding.lngInput.setText(initial.lng.toString())
        binding.radiusInput.setText(initial.radius.toInt().toString())

        binding.setLocationHereBtn.setOnClickListener {
            val client = LocationServices.getFusedLocationProviderClient(requireContext())
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    binding.latInput.setText(location.latitude.toString())
                    binding.lngInput.setText(location.longitude.toString())
                }
            }
        }

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.saveBtn.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val lat = binding.latInput.text.toString().toDoubleOrNull()
            val lng = binding.lngInput.text.toString().toDoubleOrNull()
            val radius = binding.radiusInput.text.toString().toFloatOrNull()

            if (name.isBlankAfterClean() || lat == null || lng == null || radius == null || radius <= 0) {
                return@setOnClickListener
            }

            val station = initial.copy(
                id = initial.id.ifBlank { UUID.randomUUID().toString() },
                name = name.cleanTrim(),
                lat = lat,
                lng = lng,
                radius = radius,
                overridesCloudId = overridesCloudId
            )

            if (initial.id.isBlank()) LocalStationStore.add(requireContext(), station)
            else LocalStationStore.update(requireContext(), station)

            GeofenceHelper(requireContext()).registerAllStations()
            (parentFragment as? ManageLocationsFragment)?.refreshList()
            dismiss()
        }

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}