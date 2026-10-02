package com.evu.locator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.ActivityMainBinding
import com.evu.locator.geofence.GeofenceHelper
import com.evu.locator.notifications.UpdateSyncService
import com.evu.locator.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var onAllPermissionsGranted: (() -> Unit)? = null
    var myUid: String? = null
        private set

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onAllPermissionsGranted?.invoke()
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) requestNotificationPermissionIfNeeded() else onAllPermissionsGranted?.invoke()
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (fineGranted) requestBackgroundLocationIfNeeded() else onAllPermissionsGranted?.invoke()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)

        FirestoreRepo.signInAnonymouslyIfNeeded { uid ->
            myUid = uid
            if (uid != null) {
                onAllPermissionsGranted = {
                    binding.statusText.visibility = android.view.View.GONE
                    binding.navHostFragment.visibility = android.view.View.VISIBLE
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    startUpdateSyncService()
                    GeofenceHelper(this).registerAllStations()
                }
                requestLocationPermissions()
            } else {
                binding.statusText.text = "Sign-in failed. Check internet / Firebase setup."
            }
        }
    }

    fun onRouteChanged() {
        GeofenceHelper(this).registerAllStations()
    }

    private fun requestLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) requestBackgroundLocationIfNeeded()
        else locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (bgGranted) requestNotificationPermissionIfNeeded()
            else {
                Toast.makeText(this, "Select 'Allow all the time' for background tracking", Toast.LENGTH_LONG).show()
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) onAllPermissionsGranted?.invoke()
            else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else onAllPermissionsGranted?.invoke()
    }

    private fun startUpdateSyncService() {
        ContextCompat.startForegroundService(this, Intent(this, UpdateSyncService::class.java))
    }
}