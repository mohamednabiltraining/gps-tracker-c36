package com.route.gps_tracker_c36

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.route.gps_tracker_c36.base.BaseActivity

class MainActivity : BaseActivity(),OnMapReadyCallback {
    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // user granted access for fine location
            showMessage("you can now access fine location")
        }
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // user granted access
        }
        if (permissions[Manifest.permission.CAMERA] == true) {

        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isLocationPermissionGranted()) {
            // accesss user location
            getUserLocation();
        } else {
            requestLocationPermission()
        }
        val mapFragment = supportFragmentManager.
                findFragmentById(R.id.map) as SupportMapFragment;
        mapFragment.getMapAsync(this)
    }

    var googleMap :GoogleMap?=null;
    override fun onMapReady(map: GoogleMap) {
        googleMap = map;
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
            defPos,20.0f
        ))
    }
    val defPos = LatLng(30.0160932,31.1562273)
    lateinit var locationProvider:FusedLocationProviderClient
    lateinit var locationRequest :LocationRequest
    lateinit var client: SettingsClient
    lateinit var task: Task<LocationSettingsResponse>

    @SuppressLint("MissingPermission")
    fun getUserLocation(){

        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        client = LocationServices.getSettingsClient(this)
        task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            trackUserLocation()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MainActivity,
                        GPSSettingsDialogRequestCode)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }
    val GPSSettingsDialogRequestCode =283435;

    @SuppressLint("MissingPermission")
    fun trackUserLocation(){
        locationProvider.requestLocationUpdates(
            locationRequest,
            object :LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    for (location in result.locations) {
                        updateUserLocationOnMap(location);
                    }
                }
            },
            Looper.getMainLooper())
    }

    var userMarker:Marker? =null
    private fun updateUserLocationOnMap(location: Location?) {
        if (location==null||googleMap==null){
            return
        }
        if(userMarker==null){
            userMarker =  googleMap?.addMarker(MarkerOptions()
                .position(LatLng(location.latitude,location.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                .title("user location"))
        }else {
            userMarker?.position= LatLng(location.latitude,location.longitude)
;
        }
        googleMap?.animateCamera(CameraUpdateFactory
            .newLatLngZoom(LatLng(location.latitude,location.longitude),
                20.0f
        ))
    }

    fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show explanation to user
                showMessage("we want to access location " +
                        "to get the nearest driver to you",
                    posActionTitle = "Confirm",
                    posAction = { dialogInterface, i ->
                        dialogInterface.dismiss()
                        locationPermissionRequest.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    negActionTitle = "deny",
                    negAction = { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }
                )
            }
            else {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }

    }

    fun isLocationPermissionGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true;
        }
        return false;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==GPSSettingsDialogRequestCode){
            if(resultCode== RESULT_OK){
                trackUserLocation()
            }else{
                showMessage("please enable gps service to be able ")
            }
        }
    }
}