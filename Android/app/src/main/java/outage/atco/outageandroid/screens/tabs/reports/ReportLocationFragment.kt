package outage.atco.outageandroid.screens.tabs.reports

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_report_location.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.reports.GeneralIssueReport
import outage.atco.outageandroid.screens.tabs.locations.MapViewModel
import outage.atco.outageandroid.utility.*

class ReportLocationFragment(val type: String, private val report: GeneralIssueReport): Fragment(R.layout.fragment_report_location) {

    private val viewModel = MapViewModel(this)
    private var searchPredictionList = listOf<AutocompletePrediction>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(when (type) {
            ReportType.Streetlight.string -> R.string.reports_streetlight_title
            else -> R.string.reports_general_issue
        })

        viewModel.buildMapView(0f) {
            setupMap()
        }

        report.address?.let {
            reportLocationAddressInput?.setText(it)
        }

        reportLocationAddressInput?.afterTextChangedWithDebounce { string ->
            reportLocationAddressInput?.validateRequired()

            if (string.isEmpty()) return@afterTextChangedWithDebounce clearMap()

            if (string == report.address) return@afterTextChangedWithDebounce reportLocationAddressInput?.dismissDropDown() ?: Unit

            val context = context ?: return@afterTextChangedWithDebounce

            if (searchPredictionList.isEmpty()) {
                reportLocationAddressInput?.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, listOf(getString(R.string.searching))))
                reportLocationAddressInput?.showDropDown()
            }

            PlacesQueryManager.instance?.sendQuery(string) { results, _ ->
                searchPredictionList = results ?: emptyList()

                val adapterList = results.toDropdownDisplay() ?: listOf(getString(R.string.places_autocomplete_no_results_for_query, string))
                reportLocationAddressInput?.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, adapterList))
                reportLocationAddressInput?.showDropDown()
            }
        }

        reportLocationAddressInput?.setOnItemClickListener { _, _, position, _ ->
            hideKeyboard()
            if (searchPredictionList.isNullOrEmpty()) return@setOnItemClickListener reportLocationAddressInput?.setText("") ?: Unit
            PlacesQueryManager.instance?.fetchPlace(searchPredictionList[position].placeId) { place, exception ->
                if (place == null || exception != null) {
                    return@fetchPlace showErrorMessage(resources.getString(R.string.locations_error_place_search), activity)
                }
                addMarker(place.latLng ?: return@fetchPlace reportLocationAddressInput?.setText("") ?: Unit)
            }
        }

        reportIssueNextButton.setOnClickListener {
            hideKeyboard()

            if (!reportLocationAddressInput.validateRequired()) {
                return@setOnClickListener showErrorMessage(getString(R.string.locations_error_address_required), activity)
            }

            val fragment = ReportDetailsFragment(type, report)

            fragmentManager?.beginTransaction()
                    ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    ?.replace(R.id.fragment_container, fragment)
                    ?.addToBackStack(fragment.tag)
                    ?.commit()
        }
    }

    private fun setupMap() {
        clearMap()
        viewModel.googleMap?.setOnMapLongClickListener {
            addMarker(it)
        }

        viewModel.googleMap?.setOnMapLoadedCallback {
            report.latitude?.let {
                addMarker(LatLng(it, report.longitude ?: return@let))
            }
        }
    }

    private fun clearMap() {
        viewModel.googleMap?.clear()
        viewModel.addTerritoriesToMap()
        viewModel.expandToProvince(callback = null)
    }

    private fun addMarker(location: LatLng) {
        if (!OutageManager.instance.isLocationInServiceTerritory(location)) {
            return showErrorMessage(getString(R.string.locations_error_territory), activity)
        }

        clearMap()
        if (!viewModel.didDropPinOnMap(location)) return
        viewModel.zoomToLocation(location, viewModel.standardZoomInLevel, callback = null)

        val address = LatLng(location.latitude, location.longitude).getAddress(context)
                ?: return showErrorMessage(getString(R.string.locations_error_convert_gps), activity)
        report.address = address.getAddressLine(0) ?: ""
        report.city = address.locality
        report.prov = address.adminArea
        report.latitude = location.latitude
        report.longitude = location.longitude
        reportLocationAddressInput?.setText(report.address)
    }
}