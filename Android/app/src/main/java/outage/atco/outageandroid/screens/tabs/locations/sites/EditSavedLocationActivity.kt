package outage.atco.outageandroid.screens.tabs.locations.sites

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.razir.progressbutton.bindProgressButton
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.android.synthetic.main.activity_edit_saved_location.*
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.VerificationStatusStrings
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.utility.*

class EditSavedLocationActivity: ConnectivityActivity(R.layout.activity_edit_saved_location) {

    private lateinit var location: SavedLocation
    private lateinit var cachedLocation: SavedLocation
    private var searchPredictionList = listOf<AutocompletePrediction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generalTitle.text = getString(R.string.locations_edit_site_title)

        location = intent.getParcelableExtra("location") ?: return {
            showErrorMessage(getString(R.string.locations_error_save_location), this)
            finish()
        }()

        // If location is a POI, then copy name to siteName, to ensure data saves correctly
        location.poiName?.let { if (location.siteName == null) location.siteName = it }
        cachedLocation = location.clone()

        location.siteID?.let { editSiteID.setText(it) }
        editSiteID.afterTextChanged {
            editSiteIDLayout?.error = if (it.length < 13 && it.isNotBlank()) getString(R.string.locations_site_id_error_length) else null
            location.siteID = it
        }

        location.locality?.let { editSiteAddress.setText(it) }

        editSiteAddress.afterTextChangedWithDebounce { string ->
            if (string == location.locality) return@afterTextChangedWithDebounce
            location.locality = string
            editSiteAddressLayout.error = null

            if (string.isEmpty()) return@afterTextChangedWithDebounce

            if (searchPredictionList.isEmpty()) {
                // TODO: Technically required to show Google logo image, but dropdown doesn't support mixed content. Do this for now and come back
                editSiteAddress.setAdapter(ArrayAdapter(this, R.layout.dropdown_menu_item, listOf(getString(R.string.places_powered_by_google))))
                editSiteAddress.showDropDown()
            }

            PlacesQueryManager.instance?.sendQuery(string) { results, _ ->
                searchPredictionList = results ?: emptyList()

                val adapterList = results.toDropdownDisplay() ?: listOf(getString(R.string.places_autocomplete_no_results_for_query, string))
                editSiteAddress.setAdapter(ArrayAdapter(this, R.layout.dropdown_menu_item, adapterList))
                editSiteAddress.showDropDown()
            }
        }

        editSiteAddress.setOnItemClickListener { _, _, position, _ ->
            hideKeyboard()
            if (searchPredictionList.isNullOrEmpty()) return@setOnItemClickListener editSiteAddress.setText("")
            PlacesQueryManager.instance?.fetchPlace(searchPredictionList[position].placeId) { place, exception ->
                val coords = place?.latLng
                if (place == null || exception != null || coords == null) {
                    return@fetchPlace showErrorMessage(resources.getString(R.string.locations_error_place_search), this)
                }
                if (!OutageManager.instance.isLocationInServiceTerritory(coords)) {
                    return@fetchPlace showErrorMessage(getString(R.string.locations_error_territory), this)
                }
                location.locality = place.address
                location.latitude = coords.latitude
                location.longitude = coords.longitude
                editSiteAddress.setText(location.locality)
                hideKeyboard()
            }
        }

        // Overrides end clear button because it is an autocompletetextview and textChangeListener doesn't call normally
        editSiteAddressLayout.setEndIconOnClickListener {
            editSiteAddress.setText("")
            location.locality = ""
        }

        editSiteNickname.setText(location.siteName ?: location.poiName ?: getString(R.string.locations_unnamed))
        editSiteNickname.afterTextChanged {
            editSiteNickname.validateRequired()
            location.siteName = it
            location.poiName = it
        }

        editSiteVerificationText.text = if (location.siteID.isNullOrEmpty()) getString(R.string.two_dash) else VerificationStatusStrings[location.verified ?: 0]
        val color = ContextCompat.getColor(this, if (location.verified == 1) R.color.compGreen else R.color.errorRed)
        if (!location.siteID.isNullOrBlank()) { editSiteVerificationText.setTextColor(color) }

        if (location.verified == 1) {
            editSiteNotificationText.visibility = View.GONE
            editSiteNotificationsLayout.visibility = View.VISIBLE
        } else {
            editSiteNotificationText.visibility = View.VISIBLE
            editSiteNotificationsLayout.visibility = View.GONE
        }

        editSiteEmailCheckbox.isChecked = location.isEmailEnabled ?: false
        editSiteTextCheckbox.isChecked = location.isTextEnabled ?: false
        editSitePushCheckbox.isChecked = location.isPushEnabled ?: false
        editSiteVerboseSwitch.isChecked = location.isVerboseMsg ?: false

        editSiteVerificationInfo.setOnClickListener {
            startActivity(Intent(this, SiteVerificationExplanationActivity::class.java))
        }

        bindProgressButton(editSiteDeleteButton)
        editSiteDeleteButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setMessage(R.string.locations_delete_site_message)
                    .setTitle(R.string.locations_delete_site)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        editSiteSaveButton.visibility = View.GONE
                        editSiteDeleteButton.showSubmitting(colorRes = R.color.darkGreyColor)
                        deleteLocation(location, deleteLocationCallback)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        bindProgressButton(editSiteSaveButton)
        editSiteSaveButton.setOnClickListener {
            submit()
        }
    }

    private fun submit() {
        hideKeyboard()

        if (location.siteName.isNullOrBlank() && location.poiName.isNullOrBlank()) {
            editSiteNickname.validateRequired()
            return showErrorMessage(getString(R.string.locations_error_site_name_required), this)
        }

        if (location.siteID?.length != 13 && location.locality.isNullOrBlank()) {
            editSiteIDLayout.error = getString(R.string.locations_error_address_or_site_id_required)
            editSiteAddressLayout.error = getString(R.string.locations_error_address_or_site_id_required)
            return showErrorMessage(getString(R.string.locations_error_edit_site_save), this)
        }

        val duplicateNameFound = OutageManager.instance.userSavedLocations.any { it.poiName == location.poiName || it.siteName == location.siteName }
        if (duplicateNameFound && location.siteName != cachedLocation.siteName) {
            editSiteNicknameLayout.error = getString(R.string.locations_error_site_name_unique)
            return showErrorMessage(getString(R.string.locations_error_site_name_unique), this)
        }

        editSiteDeleteButton.visibility = View.GONE
        editSiteSaveButton.showSubmitting()

        location.isEmailEnabled = editSiteEmailCheckbox.isChecked
        location.isTextEnabled = editSiteTextCheckbox.isChecked
        location.isPushEnabled = editSitePushCheckbox.isChecked
        location.isVerboseMsg = editSiteVerboseSwitch.isChecked


        // If location is a site with Site ID and cachedLocation is a POI, remove cached POI, then save new Site
        if (!location.siteID.isNullOrBlank() && cachedLocation.siteID.isNullOrBlank()) {
            return deleteLocation(cachedLocation, changeLocationTypeCallback)
        }

        // If location is a POI and cachedLocation is a site with ID, remove cached Site, then save new POI
        if (location.siteID.isNullOrBlank() && !cachedLocation.siteID.isNullOrBlank()) {
            return deleteLocation(cachedLocation, changeLocationTypeCallback)
        }

        // If location is a POI and name has changed, remove cached POI, then save new POI
        if (cachedLocation.poiName != location.poiName) {
            return deleteLocation(cachedLocation, changeLocationTypeCallback)
        }

        // else save normally
        saveLocation(saveLocationCallback)
    }

    private fun saveLocation(callback: (Boolean, LoggedException?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (location.siteID.isNullOrBlank()) {
                FirebaseManager.instance?.saveOrUpdatePOI(location, false, callback)
            } else {
                FirebaseManager.instance?.saveOrUpdateSite(location, callback)
            }
        }
    }

    private fun deleteLocation(locationToDelete: SavedLocation, callback: (Boolean, LoggedException?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (locationToDelete.siteID.isNullOrBlank()) {
                FirebaseManager.instance?.logEvent(AnalyticsEvents.POI_DELETED)
                FirebaseManager.instance?.deletePOI(locationToDelete, callback)
            } else {
                FirebaseManager.instance?.logEvent(AnalyticsEvents.SITE_DELETED)
                FirebaseManager.instance?.deleteSite(locationToDelete, callback)
            }
        }
    }


    /// MARK: Callbacks

    private val saveLocationCallback = { success: Boolean, _: LoggedException? ->
        CoroutineScope(Dispatchers.Main).launch {
            editSiteSaveButton.hideLoading(R.string.save)
            editSiteDeleteButton.visibility = View.VISIBLE
            if (success) {
                Toast.makeText(this@EditSavedLocationActivity, getString(R.string.locations_save_site_success), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                showErrorMessage(getString(R.string.locations_error_save_location), this@EditSavedLocationActivity)
            }
        }

        Unit // empty return value
    }

    private val deleteLocationCallback = { success: Boolean, _: LoggedException? ->
        CoroutineScope(Dispatchers.Main).launch {
            editSiteDeleteButton.hideLoading(R.string.delete)
            editSiteSaveButton.visibility = View.VISIBLE
            if (success) {
                Toast.makeText(baseContext, getString(R.string.locations_delete_site_success), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                showErrorMessage(getString(R.string.locations_error_delete_location), this@EditSavedLocationActivity)
            }
        }

        Unit // empty return value
    }

    // When changing from Site to POI, delete cached location and then save the new location info
    private val changeLocationTypeCallback = { success: Boolean, _: LoggedException? ->
        if (success) {
            saveLocation(saveLocationCallback)
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                showErrorMessage(getString(R.string.locations_error_save_location), this@EditSavedLocationActivity)
            }
            Unit // empty return value
        }
    }
}