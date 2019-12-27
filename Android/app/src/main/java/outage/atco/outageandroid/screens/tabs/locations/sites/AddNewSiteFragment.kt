package outage.atco.outageandroid.screens.tabs.locations.sites

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_add_new_site.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.screens.tabs.locations.MapViewModel
import outage.atco.outageandroid.utility.*

class AddNewSiteFragment(private val location: SavedLocation = SavedLocation(), private val requireSiteID: Boolean = false): Fragment(R.layout.fragment_add_new_site) {

    private val viewModel = MapViewModel(this)
    private var searchPredictionList = listOf<AutocompletePrediction>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle?.text = getString(R.string.locations_add_site_title)

        viewModel.buildMapView(0f) {
            setupMap()
        }

        if (requireSiteID) siteInputTitle?.text = getString(R.string.locations_your_site_id_required)

        siteInput?.afterTextChanged {
            siteInputLayout?.error = if (it.length < 13 && it.isNotBlank()) getString(R.string.locations_site_id_error_length) else null
            location.siteID = it
        }

        addressInput?.afterTextChangedWithDebounce { string ->
            if (string == location.locality) return@afterTextChangedWithDebounce
            location.locality = string
            addressInputLayout?.error = null

            if (string.isBlank()) return@afterTextChangedWithDebounce clearMap()
            val context = context ?: return@afterTextChangedWithDebounce

            if (searchPredictionList.isEmpty()) {
                addressInput.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, listOf(getString(R.string.searching))))
                addressInput.showDropDown()
            }

            PlacesQueryManager.instance?.sendQuery(string) { results, _ ->
                searchPredictionList = results ?: emptyList()

                val adapterList = results.toDropdownDisplay() ?: listOf(getString(R.string.places_autocomplete_no_results_for_query, string))
                addressInput.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, adapterList))
                addressInput.showDropDown()
            }
        }

        addressInput?.setOnItemClickListener { _, _, position, _ ->
            hideKeyboard()
            if (searchPredictionList.isNullOrEmpty()) return@setOnItemClickListener addressInput.setText("")
            PlacesQueryManager.instance?.fetchPlace(searchPredictionList[position].placeId) { place, exception ->
                if (place == null || exception != null) {
                    return@fetchPlace showErrorMessage(resources.getString(R.string.locations_error_place_search), activity)
                }
                addMarker(place.latLng ?: return@fetchPlace addressInput.setText(""))
            }
        }

        nextButton?.setOnClickListener {
            submit()

        }

    }

    private fun submit() {
        hideKeyboard()
        if (location.siteID?.length != 13 && requireSiteID) {
            siteInputLayout?.error = getString(R.string.locations_error_site_id_required)
            return showErrorMessage(getString(R.string.locations_error_site_id_required), activity)
        }

        if (location.siteID?.length != 13 && location.locality.isNullOrEmpty()) {
            addressInputLayout?.error = getString(R.string.locations_error_address_or_site_id_required)
            siteInputLayout?.error = getString(R.string.locations_error_address_or_site_id_required)
            return showErrorMessage(getString(R.string.locations_error_address_or_site_id_required), activity)
        }

        val fragment = AddNewSiteNicknameFragment(location)
        fragmentManager?.beginTransaction()
            ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(fragment.tag)
            ?.commit()
    }

    private fun setupMap() {
        clearMap()
        viewModel.googleMap?.setOnMapLongClickListener {
            addMarker(it)
        }

        addMarker(LatLng(location.latitude ?: return, location.longitude ?: return))
    }

    private fun clearMap() {
        viewModel.googleMap?.clear()
        viewModel.addTerritoriesToMap()
        viewModel.expandToProvince(callback = null)
    }

    private fun addMarker(coords: LatLng) {
        if (!OutageManager.instance.isLocationInServiceTerritory(coords)) {
            return showErrorMessage(getString(R.string.locations_error_territory), activity)
        }

        val addressString = coords.getAddressString(context) ?: return showErrorMessage(getString(R.string.locations_error_convert_gps), activity)

        clearMap()
        if (!viewModel.didDropPinOnMap(coords)) return
        viewModel.zoomToLocation(coords, viewModel.standardZoomInLevel, callback = null)

        location.locality = addressString
        location.latitude = coords.latitude
        location.longitude = coords.longitude
        addressInput.setText(location.locality)
    }


}