package outage.atco.outageandroid.screens.tabs.locations

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*

@SuppressLint("MissingPermission")
class MapViewModel(var fragment: Fragment): ViewModel(),
        OutageManager.OutageDataListener,
        OutageManager.UserDataListener,
        FirebaseAuth.AuthStateListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnPoiClickListener {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback

    var googleMap: GoogleMap? = null
    private var lastLocation: Location? = null
    private var isMapLoaded = false

    val locationRequestCode = 1
    val standardZoomInLevel = 14f
    val standardBottomPadding = 120f
    val outageBottomPadding = 210f
    var currentBottomPadding = standardBottomPadding

    val isUserLoggedIn
        get() = FirebaseManager.instance?.user != null

    private val hasLocationPermission: Boolean
        get() {
            return ActivityCompat.checkSelfPermission(fragment.context ?: return false, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

    private val userPrefMapType: Int
        get() {
            val sharedPref = fragment.activity?.getSharedPreferences("USER_PREFERENCES", Context.MODE_PRIVATE)
            return sharedPref?.getInt("Map Type", GoogleMap.MAP_TYPE_NORMAL) ?: GoogleMap.MAP_TYPE_NORMAL
        }

    val layersVisibility = mutableMapOf(
            OutageLayer.Active.type to true,
            OutageLayer.Impact.type to true,
            OutageLayer.Scheduled.type to true,
            OutageLayer.Planned.type to true,
            OutageLayer.Saved.type to true)

    var queriesReturned = mutableMapOf(
            OutageLayer.Active.type to false,
            OutageLayer.Planned.type to false,
            OutageLayer.Impact.type to false,
            OutageLayer.PlannedImpact.type to false,
            OutageLayer.Territory.type to false,
            OutageLayer.Saved.type to false
    )

    override fun onCleared() {
        googleMap = null
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        OutageManager.instance.removeQueryListeners()
        FirebaseManager.instance?.removeAuthStateListener(this)
        super.onCleared()
    }

    fun handleLocationPermissions() {
        if (!hasLocationPermission) {
            // Check Permissions Now
            if (fragment.requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Display UI and wait for user interaction
                AlertDialog.Builder(fragment.context ?: return, R.style.AppTheme_Dialog)
                        .setTitle("Enable Location Permission")
                        .setMessage("Please enable access to your device location to help in adding sites to your saved locations and show outages in your vicinity.")
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            fragment.requestPermissions(permissions, locationRequestCode)
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
            } else {
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                fragment.requestPermissions(permissions, locationRequestCode)
            }
        } else {
            fusedLocationClient = FusedLocationProviderClient(fragment.requireActivity())
            googleMap?.isMyLocationEnabled = hasLocationPermission
            locationCallback = object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (location in locationResult.locations){
                        lastLocation = location
                        Log.d("User Location Update", location.toString())
                        (fragment as? MapFragment)?.enableLocationButton()
                    }
                }
            }
            fusedLocationClient?.requestLocationUpdates(LocationRequest(), locationCallback, null)
        }
    }

    fun buildMapView(bottomPadding: Float = standardBottomPadding, completionHandler: () -> Unit) {
        val map = SupportMapFragment.newInstance()
        fragment.childFragmentManager.beginTransaction().replace(fragment.mapView.id, map).commit()

        map?.getMapAsync {
            googleMap = it
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            googleMap?.isMyLocationEnabled = hasLocationPermission

            googleMap?.mapType = userPrefMapType

            // Adjust for bottom cards
            setMapPadding(bottomPadding)
            zoomToLocation(LatLng(55.0,-114.5), 4.4f, 1, null) // Start with center on province while map finishes laying out

            // Listeners
            googleMap?.setOnMarkerClickListener(this)
            googleMap?.setOnPoiClickListener(this)
            googleMap?.setOnMapLongClickListener(this)

            googleMap?.setOnMapLoadedCallback {
                isMapLoaded = true
                completionHandler()
            }

        }
    }

    // Google Maps takes measurements in pixels, not dp, so must do conversion before setting padding amounts
    fun setMapPadding(bottom: Float = standardBottomPadding, sides: Float = 0f) {
        if (fragment.context == null) return
        val bottomPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, bottom, fragment.resources.displayMetrics).toInt()
        val sidePixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sides, fragment.resources.displayMetrics).toInt()

        currentBottomPadding = bottom
        googleMap?.setPadding(sidePixels, sidePixels, sidePixels, bottomPixels)
    }

    fun zoomToLocation(location: LatLng, zoom: Float = standardZoomInLevel, duration: Int = 1000, callback: GoogleMap.CancelableCallback?) {
        val currentZoom = googleMap?.cameraPosition?.zoom ?: 0f
        val newZoom = if (currentZoom > zoom) currentZoom else zoom
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, newZoom), duration, callback)
    }

    fun zoomToBounds(bounds: LatLngBounds, padding: Int = 20, duration: Int = 1000, callback: GoogleMap.CancelableCallback?) {
        if (fragment.context == null) return
        val widthPixels = fragment.mapView.resources.displayMetrics.widthPixels

        val paddingPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (currentBottomPadding + padding.toFloat() * 2), fragment.resources.displayMetrics).toInt()
        val heightPixels = (fragment.mapView.resources.displayMetrics.heightPixels - paddingPixels)
        val update = CameraUpdateFactory.newLatLngBounds(bounds, widthPixels, heightPixels, padding)
        googleMap?.animateCamera(update, duration, callback)
    }

    fun zoomToUserLocation() {
        val loc = lastLocation ?: return
        zoomToLocation(LatLng(loc.latitude, loc.longitude), standardZoomInLevel, callback = null)
    }

    fun expandToProvince(duration: Int = 1000, callback: GoogleMap.CancelableCallback?) {
        if (googleMap == null) return
        val bounds = LatLngBounds.builder()
        bounds.include(LatLng(60.0, -121.0)) // Top Left corner of province
        bounds.include(LatLng(48.8, -109.0)) // Bottom right corner of province
        zoomToBounds(bounds.build(), duration = duration, callback = callback)
    }

    private fun onQueryReturned(type: OutageLayer) {
        queriesReturned[type.type] = true
        Log.d("Queries", queriesReturned.toString())
        if (!queriesReturned.all { it.value }) return

        CoroutineScope(Dispatchers.Main).launch {
            (fragment as? MapFragment)?.hideBanner()
            refreshMapOutageLayers()
        }
    }

    fun refreshMapOutageLayers() {
        if (!isMapLoaded) return
        // Reset map
        googleMap?.clear()

        addUserSavedLocations()
        addTerritoriesToMap()
        addActiveOutagesToMap()
        addPlannedOutagesToMap()

        (fragment as? MapFragment)?.updateUserBasedViewsVisibility()
    }

    private fun addActiveOutagesToMap() {
        OutageManager.instance.activeOutageList.forEach { outage ->
            val loc = LatLng(outage.latitude ?: return@forEach, outage.longitude ?: return@forEach)
            val isImpacting = outage.isImpactingUserLocation
            val isScheduled = outage.causeCode == "Scheduled Outage" // TODO Is this correct?

            // If not selected as visible, then do not add this outage
            if (isImpacting && layersVisibility[OutageLayer.Impact.type] == false) return@forEach
            if (isScheduled && layersVisibility[OutageLayer.Scheduled.type] == false) return@forEach
            if (!isImpacting && !isScheduled && layersVisibility[OutageLayer.Active.type] == false) return@forEach

            val markerOptions = MarkerOptions()
            markerOptions.position(loc)

            val polygonOptions = PolygonOptions()
            polygonOptions.strokeWidth(2.0f)
            polygonOptions.strokeColor(fragment.context?.getColor(R.color.activeOutageBoundaryColor) ?: 0)
            polygonOptions.fillColor(fragment.context?.getColor(R.color.activeOutageFillColor) ?: 0)

            when {
                isImpacting -> {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return, R.drawable.ic_map_impact_outage)?.toBitmap()))
                    polygonOptions.strokeColor(fragment.context?.getColor(R.color.impactingOutageBoundaryColor) ?: 0)
                    polygonOptions.fillColor(fragment.context?.getColor(R.color.impactingOutageFillColor) ?: 0)
                }
                isScheduled -> markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return, R.drawable.ic_map_active_scheduled_outage)?.toBitmap()))
                else -> markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return, R.drawable.ic_map_active_outage)?.toBitmap()))
            }

            val marker = googleMap?.addMarker(markerOptions)
            marker?.tag = outage.outageID

            if (outage.gpsPointsForBoundary != null) {
                polygonOptions.addAll(outage.gpsPointsForBoundary)
                googleMap?.addPolygon(polygonOptions)
            }
        }
    }

    private fun addPlannedOutagesToMap() {
        if (layersVisibility[OutageLayer.Planned.type] == false) return
        OutageManager.instance.plannedOutageList.forEach { outage ->
            val loc = LatLng(outage.latitude ?: return@forEach, outage.longitude ?: return@forEach)

            val markerOptions = MarkerOptions()
            markerOptions.position(loc)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return, R.drawable.ic_map_planned_outage)?.toBitmap()))
            val marker = googleMap?.addMarker(markerOptions)
            marker?.tag = outage.outageID

            val polygonOptions = PolygonOptions()
            polygonOptions.strokeWidth(2.0f)
            polygonOptions.strokeColor(fragment.context?.getColor(R.color.plannedOutageBoundaryColor) ?: 0)
            polygonOptions.fillColor(fragment.context?.getColor(R.color.plannedOutageFillColor) ?: 0)

            if (outage.gpsPointsForBoundary != null) {
                polygonOptions.addAll(outage.gpsPointsForBoundary)
                googleMap?.addPolygon(polygonOptions)
            }
        }
    }

    fun addTerritoriesToMap() {
        OutageManager.instance.territoryBoundariesSet?.forEach { territory ->
            if (territory.size <= 0) return
            val rectOptions = PolygonOptions()

            rectOptions.addAll(territory)

            rectOptions.fillColor(fragment.context?.getColor(R.color.territoryFillColor) ?: 0)
            rectOptions.strokeColor(fragment.context?.getColor(R.color.territoryBoundaryColor) ?: 0)
            rectOptions.strokeWidth(2.0f)

            val polygon = googleMap?.addPolygon(rectOptions)
            OutageManager.instance.territoryPolygons.add(polygon)
        }
    }

    private fun addUserSavedLocations() {
        if (layersVisibility[OutageLayer.Saved.type] == false || !isUserLoggedIn) return
        OutageManager.instance.userSavedLocations.forEach {
            val position = LatLng(it.latitude ?: return@forEach, it.longitude ?: return@forEach)

            val markerOptions = MarkerOptions()
            markerOptions.position(position)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return, R.drawable.ic_map_saved_location)?.toBitmap()))
            val marker = googleMap?.addMarker(markerOptions)
            marker?.tag = it.uniqueID ?: it.poiName

            OutageManager.instance.checkLocationPlannedImpact(it)
            OutageManager.instance.checkLocationActiveImpact(it)
        }
    }

    fun didDropPinOnMap(location: LatLng?): Boolean {
        if (location == null || !OutageManager.instance.isLocationInServiceTerritory(location)) {
            showErrorMessage(fragment.getString(R.string.locations_error_territory), fragment.activity)
            return false
        }

        val markerOptions = MarkerOptions()
        markerOptions.position(location)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getDrawable(fragment.context ?: return false, R.drawable.ic_map_poi_pin)?.toBitmap()))
        googleMap?.addMarker(markerOptions)
        return true
    }


    /// OVERRIDES & CALLBACKS
    override fun onOutageDataSuccess(layer: OutageLayer) {
        onQueryReturned(layer)
    }

    override fun onOutageDataFailure(exception: LoggedException?, layer: OutageLayer?) {
        if (layer == null) {
            CoroutineScope(Dispatchers.Main).launch {
                refreshMapOutageLayers()
                showErrorMessage(fragment.getString(R.string.network_error), fragment.activity)
            }
        } else {
            onQueryReturned(layer)
        }

        layer?.let { onQueryReturned(it) }
    }

    override fun onUserDataSuccess() {
        onQueryReturned(OutageLayer.Saved)
    }

    override fun onUserDataFailure(exception: LoggedException?) {
        onQueryReturned(OutageLayer.Saved)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        val id = marker?.tag ?: return true
        val outage = OutageManager.instance.activeOutageList.find { it.outageID == id }
                ?: OutageManager.instance.plannedOutageList.find { it.outageID == id }
        val userLocation = OutageManager.instance.userSavedLocations.find { it.uniqueID == id || it.poiName == id }

        if (outage != null) (fragment as? MapFragment)?.showOutageInfo(outage)

        if (userLocation != null) {
            val latLng = userLocation.latitude?.let { LatLng(it, userLocation.longitude ?: return true) }
            val name = userLocation.siteName ?: userLocation.poiName ?: fragment.getString(R.string.locations_unnamed)
            val place = Place.builder().setName(name).setLatLng(latLng).setAddress(userLocation.locality).setId(userLocation.uniqueID).build()
            (fragment as? MapFragment)?.showPOIInfo(place)
        }

        return true
    }

    override fun onMapLongClick(coords: LatLng?) {
        if (coords == null) return
        refreshMapOutageLayers() // Remove any previously placed pins
        if (!didDropPinOnMap(coords)) return

        // TODO: Figure out if Geocoder is good enough here
        // To get a placeID, need to geocode coords to an address string, then search that, get first result and fetch place for ID
        // Doesn't produce materially different results, and is slow as it is multiple requests, so not recommended for now
        val address = coords.getAddressString(fragment.context)
        val place = Place.builder().setName(address?.split(",")?.first() ?: fragment.getString(R.string.locations_unnamed)).setLatLng(coords).setAddress(address).build()
        (fragment as? MapFragment)?.showPOIInfo(place)
    }

    override fun onPoiClick(poi: PointOfInterest?) {
        if (poi == null) return
        PlacesQueryManager.instance?.startNewSession()

        PlacesQueryManager.instance?.fetchPlace(poi.placeId) { place, exception ->
            if (place == null || exception != null) {
                return@fetchPlace showErrorMessage(fragment.resources.getString(R.string.locations_error_place_search), fragment.activity)
            }
            (fragment as? MapFragment)?.showPOIInfo(place)
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        (fragment as? MapFragment)?.refreshMap()
    }
}