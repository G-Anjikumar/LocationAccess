package com.anji.locationaccess

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.anji.location_sdk.data.LocationUpdateCallback
import com.anji.location_sdk.viewmodel.MapViewModel
import com.anji.locationaccess.databinding.ActivityMainBinding
import com.anji.locationaccess.views.UserScreenFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mapViewModel: MapViewModel by viewModels()
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.root)
        checkAndRequestPermissions()
    }

    fun framgentTransition(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment)
            .addToBackStack("")
            .commit()
    }

    private fun allPermissionsGranted() = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        if (allPermissionsGranted()) {
            onPermissionGranted()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                onPermissionGranted()
            }

            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                onPermissionGranted()
            }

            else -> {
                Toast.makeText(this, "Location permissions are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPermissionGranted() {
        framgentTransition(UserScreenFragment())
    }
}