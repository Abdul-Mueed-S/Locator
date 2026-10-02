package com.evu.locator.ui.traveler

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.evu.locator.MainActivity
import com.evu.locator.R
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.LocalStationStore
import com.evu.locator.databinding.FragmentTravelerBinding
import com.evu.locator.geofence.LocationCheckService
import com.evu.locator.utils.RouteStations
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

private val ROUTES = RouteStations.ROUTES

class TravelerFragment : Fragment() {

    private var _binding: FragmentTravelerBinding? = null
    private val binding get() = _binding!!
    private var wellWishers = listOf<com.evu.locator.data.models.Contact>()

    // true while we are programmatically applying Firestore data to the UI —
    // prevents Firestore's cache-then-server double emission from being
    // mistaken for a real user action.
    private var suppressListeners = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTravelerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val myUid = (activity as MainActivity).myUid ?: return

        binding.routeSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, ROUTES
        )

        binding.showQrBtn.setOnClickListener {
            QrDialogFragment(myUid).show(parentFragmentManager, "qr")
        }

        binding.manageLocationsBtn.setOnClickListener {
            findNavController().navigate(R.id.manageLocationsFragment)
        }

        binding.makeUpdateBtn.setOnClickListener {
            MakeUpdateDialogFragment(wellWishers) { update, desc, ts, targetUid ->
                FirestoreRepo.postUpdate(myUid, update, desc, ts, targetUid)
            }.show(parentFragmentManager, "makeUpdate")
        }

        binding.routeSpinner.setOnItemSelectedListenerCompat {
            if (suppressListeners) return@setOnItemSelectedListenerCompat
            val route = binding.routeSpinner.selectedItem as? String ?: return@setOnItemSelectedListenerCompat
            val forward = binding.forwardRadio.isChecked
            FirestoreRepo.setActiveRoute(myUid, route, forward)
            (activity as MainActivity).onRouteChanged()
            if (binding.sharingSwitch.isChecked) announceRouteChanged(myUid, route, forward)
            updateActiveJourneyLabel(route, forward, binding.sharingSwitch.isChecked)
        }

        binding.directionGroup.setOnCheckedChangeListener { _, _ ->
            if (suppressListeners) return@setOnCheckedChangeListener
            val route = binding.routeSpinner.selectedItem as? String ?: return@setOnCheckedChangeListener
            val forward = binding.forwardRadio.isChecked
            FirestoreRepo.setActiveRoute(myUid, route, forward)
            (activity as MainActivity).onRouteChanged()
            if (binding.sharingSwitch.isChecked) announceRouteChanged(myUid, route, forward)
            updateActiveJourneyLabel(route, forward, binding.sharingSwitch.isChecked)
        }

        binding.sharingSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressListeners) return@setOnCheckedChangeListener
            FirestoreRepo.setSharingEnabled(myUid, checked)
            val route = binding.routeSpinner.selectedItem as? String ?: ""
            val forward = binding.forwardRadio.isChecked
            if (checked) {
                announceSharingStarted(myUid, route, forward)
                LocationCheckService.start(requireContext())
            } else {
                LocationCheckService.stop(requireContext())
            }
            updateActiveJourneyLabel(route, forward, checked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenContacts(myUid, "wellWisher").collect { wellWishers = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenUserDoc(myUid).collect { user ->
                if (user != null) {
                    suppressListeners = true
                    binding.sharingSwitch.isChecked = user.sharingEnabled
                    val idx = ROUTES.indexOf(user.activeRoute)
                    if (idx >= 0) binding.routeSpinner.setSelection(idx)
                    binding.forwardRadio.isChecked = user.activeDirectionForward
                    binding.reverseRadio.isChecked = !user.activeDirectionForward
                    updateActiveJourneyLabel(user.activeRoute, user.activeDirectionForward, user.sharingEnabled)
                    suppressListeners = false
                }
            }
        }
    }

    private fun updateActiveJourneyLabel(route: String, forward: Boolean, sharing: Boolean) {
        if (!sharing || route.isBlank()) {
            binding.activeJourneyText.text = "Not sharing"
        } else {
            val dir = if (forward) "Home → College" else "College → Home"
            binding.activeJourneyText.text = "Active: $route ($dir)"
        }
    }

    private fun announceRouteChanged(myUid: String, route: String, forward: Boolean) {
        val destText = if (forward) "Leaving for College" else "Leaving for Home"
        FirestoreRepo.postUpdate(myUid, "Taking $route route. $destText", null, System.currentTimeMillis())
    }

    @SuppressLint("MissingPermission")
    private fun announceSharingStarted(myUid: String, route: String, forward: Boolean) {
        val destText = if (forward) "Leaving for College" else "Leaving for Home"
        val fused = LocationServices.getFusedLocationProviderClient(requireContext())

        fused.lastLocation.addOnSuccessListener { location ->
            FirestoreRepo.fetchCloudStations { cloud ->
                val local = LocalStationStore.getAll(requireContext())
                val all = local + cloud
                val homeStation = all.firstOrNull { it.name.contains("tanwar", ignoreCase = true) }

                val atHome = if (location != null && homeStation != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(location.latitude, location.longitude, homeStation.lat, homeStation.lng, results)
                    results[0] <= homeStation.radius
                } else false

                val prefix = if (atHome) "At Home. " else ""
                FirestoreRepo.postUpdate(myUid, "$prefix" + "Taking $route route. $destText", null, System.currentTimeMillis())
            }
        }.addOnFailureListener {
            FirestoreRepo.postUpdate(myUid, "Taking $route route. $destText", null, System.currentTimeMillis())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun android.widget.Spinner.setOnItemSelectedListenerCompat(onSelected: () -> Unit) {
    this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected()
        }
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}