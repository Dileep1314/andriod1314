package outage.atco.outageandroid.screens.tabs.locations

import android.animation.LayoutTransition
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.app_bar_search.*
import kotlinx.android.synthetic.main.floating_banner.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.map_card_outage_information.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.outages.ActiveOutage
import outage.atco.outageandroid.models.outages.BaseOutage
import outage.atco.outageandroid.models.outages.PlannedOutage
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.screens.MainActivity
import outage.atco.outageandroid.screens.tabs.settings.account.AuthActivity
import outage.atco.outageandroid.utility.*
import java.util.*

class MapFragment: Fragment(R.layout.fragment_map),
        ConnectivityActivity.ConnectivityListener {

    val viewModel = MapViewModel(this)
    private val layoutManager = LinearLayoutManager(this.context, HORIZONTAL, false)
    private var currentRecyclerPosition = 0
    private var searchPredictionList = listOf<AutocompletePrediction>()
    private var selectedAddress: String? = null

    private var isNetworkConnected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutageManager.instance.queryOutageDataListener = viewModel
        OutageManager.instance.queryUserDataListener = viewModel
        FirebaseManager.instance?.addAuthStateListener(viewModel)
        (activity as? MainActivity)?.addConnectivityListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainTitle.text = getString(R.string.nav_location)
        addSearchButton()

        mapCardRecycler?.layoutManager = layoutManager
        LinearSnapHelper().attachToRecyclerView(mapCardRecycler)

        viewModel.buildMapView {
            viewModel.refreshMapOutageLayers()
            updateUserBasedViewsVisibility()
            view.post {
                viewModel.expandToProvince(500, callback = null)
            }
        }

        viewModel.handleLocationPermissions()
        outageInfoCard?.visibility = View.GONE
        loginRegisterCard?.visibility = View.GONE

        locationButton.setOnClickListener {
            transitionOutageInfo(shouldShow = false)
            viewModel.zoomToUserLocation()
        }

        expandButton.setOnClickListener {
            transitionOutageInfo(shouldShow = false)
            viewModel.expandToProvince(callback = null)
        }

        layersButton.clickWithDebounce {
            transitionOutageInfo(shouldShow = false)
            val bottomSheetFragment = LayersBottomSheetFragment(viewModel.layersVisibility, this)
            val fragmentManager = fragmentManager ?: return@clickWithDebounce
            bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
        }

        refreshButton.clickWithDebounce {
            refreshMap()
        }

        loginRegisterCard.setOnClickListener {
            startActivity(Intent(context ?: return@setOnClickListener, AuthActivity::class.java))
        }

        setupSearchBar()
    }

    override fun onResume() {
        super.onResume()

        FirebaseManager.instance?.setCurrentScreen(activity ?: return, AnalyticsScreenNames.LOCATIONS_TAB)
    }

    override fun onDestroy() {
        (activity as? MainActivity)?.removeConnectivityListener(this)

        super.onDestroy()
    }

    fun refreshMap() {
        // AuthStateListener can call this while the fragment is not attached, so do a null check to ensure there is context
        if (context == null) return
        transitionOutageInfo(false)
        if (!isNetworkConnected) return showErrorMessage(getString(R.string.offline_dialog_message), activity)

        dismissErrorDialog()
        viewModel.googleMap?.clear()
        showBanner()
        CoroutineScope(Dispatchers.IO).launch {
            val newMap = viewModel.queriesReturned.map { it.key to false }.toMap()
            viewModel.queriesReturned = newMap.toMutableMap()
            OutageManager.instance.queryOutageData()
        }
    }

    fun enableLocationButton() {
        locationButton?.isEnabled = true
    }

    fun updateUserBasedViewsVisibility() {
        if (viewModel.isUserLoggedIn) {
            loginRegisterCard?.visibility = View.GONE
            recyclerContainer?.visibility = View.VISIBLE
            refreshLocationsRecycler()
        } else {
            loginRegisterCard?.visibility = View.VISIBLE
            recyclerContainer?.visibility = View.GONE
        }
    }

    private fun refreshLocationsRecycler() {
        currentRecyclerPosition = 0
        val adapterList = OutageManager.instance.userSavedLocations.toMutableList()
        adapterList.add(SavedLocation()) // Empty Location to have an extra cell at end to show add new location cell
        mapCardRecycler?.adapter = MapCardAdapter(adapterList, this)
        mapCardRecycler?.adapter?.notifyDataSetChanged()
        mapCardRecycler?.smoothScrollToPosition(0)
    }

    fun setLayerVisibility(layer: String) {
        viewModel.layersVisibility.apply { this[layer] = !(this[layer] ?: false) }
        viewModel.refreshMapOutageLayers()

        if (OutageManager.instance.userSavedLocations.isNullOrEmpty()) return
        mapCardRecycler?.visibility = if (viewModel.layersVisibility[OutageLayer.Saved.type] == false) View.GONE else View.VISIBLE
    }

    private fun showBanner(message: String = getString(R.string.loading)) {
        bannerProgress?.animate()
        bannerText?.text = message
        refreshButton?.visibility = View.GONE
        locationButton?.visibility = View.GONE
        layersButton?.visibility = View.GONE
        expandButton?.visibility = View.GONE
        floatingBanner?.visibility = View.VISIBLE
    }

    fun hideBanner() {
        floatingBanner?.visibility = View.GONE
        refreshButton?.visibility = View.VISIBLE
        locationButton?.visibility = View.VISIBLE
        layersButton?.visibility = View.VISIBLE
        expandButton?.visibility = View.VISIBLE
    }

    fun showOutageInfo(outage: BaseOutage) {
        outageStatusText.text = outage.status
        outageStartText.text = outage.startDate?.formatDisplay()
        outageFinishText.text = outage.finishDate?.formatDisplay()
        outageCustomersText.text = if (outage.customersAffected > 10 ) outage.customersAffected.toString() else "< 10"

        when (outage) {
            is ActiveOutage -> {
                outageCauseText.text = outage.causeCode
                topBarLayout.background = ContextCompat.getDrawable(context ?: return, R.color.activeOutageBoundaryColor)
                topBarIcon.setImageResource(R.drawable.ic_active_outage_icon)
                topBarTitle.text = getString(R.string.locations_layer_active)
            }

            is PlannedOutage -> {
                topBarLayout.background = ContextCompat.getDrawable(context ?: return, R.color.plannedOutageBoundaryColor)
                topBarIcon.setImageResource(R.drawable.ic_planned_outage_icon)
                topBarTitle.text = getString(R.string.locations_layer_planned)
                outageCauseText.text = getString(R.string.locations_layer_planned)
            }
        }

        if (outage.isImpactingUserLocation && outage is ActiveOutage) {
            topBarLayout.background = ContextCompat.getDrawable(context ?: return, R.color.impactingOutageBoundaryColor)
            topBarTitle.text = getString(R.string.locations_layer_impacting)
        }

        closeButton.setOnClickListener {
            transitionOutageInfo(shouldShow = false)
        }

        transitionOutageInfo(shouldShow = true)

        CoroutineScope(Dispatchers.Main).launch {
            outage.zoomBounds?.let {
                viewModel.zoomToBounds(it, padding = 10, callback = null) }
        }
    }

    private fun transitionOutageInfo(shouldShow: Boolean = false) {
        viewModel.setMapPadding(if (shouldShow) viewModel.outageBottomPadding else viewModel.standardBottomPadding)

        val transition = Slide(Gravity.BOTTOM)
        transition.duration = 600
        transition.addTarget(R.id.outageInfoCard)
        TransitionManager.beginDelayedTransition(mapParent ?: return, transition)
        outageInfoCard.visibility = if (shouldShow) View.VISIBLE else View.GONE

        // Hide other bottom views
        if (shouldShow) {
            loginRegisterCard.visibility = View.GONE
            recyclerContainer.visibility = View.GONE
        } else {
            updateUserBasedViewsVisibility()
        }
    }

    fun showPOIInfo(poi: Place) {
        hideOrShowSearchBar(justHide = true)
        outageInfoCard.visibility = View.GONE
        loginRegisterCard.visibility = View.GONE
        recyclerContainer.visibility = View.GONE

        val bottomSheetFragment = POIBottomSheetFragment(poi, this)

        val fragmentManager = fragmentManager ?: return
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    fun onDismissBottomSheet(savedLocation: SavedLocation?) {
        viewModel.setMapPadding(viewModel.standardBottomPadding)
        recyclerContainer?.visibility = View.VISIBLE
        viewModel.refreshMapOutageLayers()

        OutageManager.instance.userSavedLocations.indexOf(savedLocation).let {
            mapCardRecycler.layoutManager?.scrollToPosition(it)
        }
    }

    private fun addSearchButton() {
        mapParent.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val searchButton = ImageButton(context)
        searchButton.setImageResource(R.drawable.ic_search)
        searchButton.background = ContextCompat.getDrawable(context ?: return, android.R.color.transparent)
        searchButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context ?: return, R.color.white))

        searchButton.clickWithDebounce {
            hideOrShowSearchBar()
        }

        searchButton.tag = "searchButton"
        mainRightButtonsContainer?.addView(searchButton)
    }

    private fun hideOrShowSearchBar(justHide: Boolean? = null) {
        val shouldBecomeVisible = justHide ?: search_container.layoutParams.height == 0

        if (shouldBecomeVisible) {
            search_container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            showKeyboard()
            searchBar.post { searchBar.requestFocus() }
        } else {
            search_container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
            searchBar.clearFocus()
            searchBar.dismissDropDown()
            hideKeyboard()
        }

        mapParent.requestLayout()
    }

    private fun setupSearchBar() {
        searchBar.setOnItemClickListener { _, _, position, _ ->
            if (searchPredictionList.isNullOrEmpty()) return@setOnItemClickListener searchBar.setText("")

            val selectedPrediction = searchPredictionList[position]
            selectedAddress = selectedPrediction.getFullText(null).toString()
            searchBar.setText(selectedAddress)
            searchBar.dismissDropDown()
            searchBar.clearFocus()
            hideKeyboard()

            PlacesQueryManager.instance?.fetchPlace(selectedPrediction.placeId) { place, exception ->
                if (place == null || exception != null) {
                    return@fetchPlace showErrorMessage(resources.getString(R.string.locations_error_place_search), activity)
                }
                viewModel.refreshMapOutageLayers()
                if (!viewModel.didDropPinOnMap(place.latLng)) return@fetchPlace
                showPOIInfo(place)
            }

        }

        searchBar.afterTextChangedWithDebounce { string ->
            if (string.isBlank()) return@afterTextChangedWithDebounce
            val context = context ?: return@afterTextChangedWithDebounce

            if (string == selectedAddress) return@afterTextChangedWithDebounce searchBar.dismissDropDown()

            if (searchPredictionList.isEmpty()) {
                searchBar.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, listOf(getString(R.string.searching))))
                searchBar.showDropDown()
            }

            PlacesQueryManager.instance?.sendQuery(string) { results, _ ->
                searchPredictionList = results ?: emptyList()

                val adapterList = results.toDropdownDisplay() ?: listOf(getString(R.string.places_autocomplete_no_results_for_query, string))
                searchBar.setAdapter(ArrayAdapter(context, R.layout.dropdown_menu_item, adapterList))
                searchBar.showDropDown()

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return
        if (requestCode == viewModel.locationRequestCode && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.handleLocationPermissions()
        }
    }

    override fun onConnectionStatusUpdate(isConnected: Boolean) {
        val wasConnected = isNetworkConnected
        isNetworkConnected = isConnected
        activity?.runOnUiThread {
            refreshButton?.isEnabled = isConnected
            // Only refresh map if connection status actually changed from not connected (connection status can fire multiple times)
            if (isConnected && !wasConnected) refreshMap()
        }
    }
}
