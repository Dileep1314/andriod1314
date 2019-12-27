package outage.atco.outageandroid.screens.tabs.locations.sites

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.github.razir.progressbutton.bindProgressButton
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_add_new_site_nickname.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.utility.*

class AddNewSiteNicknameFragment(private val site: SavedLocation = SavedLocation()):
        Fragment(R.layout.fragment_add_new_site_nickname) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.locations_add_site_title)

        notificationsLayout.visibility = if (site.siteID == null) View.GONE else View.VISIBLE

        site.siteName?.let { nicknameInput.setText(it) }

        nicknameInput.afterTextChanged {
            nicknameInputLayout.error = if (it.isBlank()) getString(R.string.locations_error_site_name_required) else null
            site.siteName = it
            site.poiName = it
        }


        bindProgressButton(nextButton)

        nextButton.setOnClickListener {
            submit()
        }
    }

    private fun submit() {
        hideKeyboard()

        if (site.siteName.isNullOrBlank()) {
            nicknameInputLayout.error = getString(R.string.locations_error_site_name_required)
            return showErrorMessage(getString(R.string.locations_error_site_name_required), activity)
        }

        if (OutageManager.instance.userSavedLocations.any { it.siteName == site.siteName }) {
            nicknameInputLayout.error = getString(R.string.locations_error_site_name_unique)
            return showErrorMessage(getString(R.string.locations_error_site_name_unique), activity)
        }

        nextButton.showSubmitting()

        site.isVerboseMsg = addSiteVerboseSwitch.isChecked
        site.isTextEnabled = addSiteTextCheckbox.isChecked
        site.isPushEnabled = addSitePushCheckbox.isChecked
        site.isEmailEnabled = addSiteEmailCheckbox.isChecked

        saveLocation()
    }

    private fun saveLocation() {
        val callback = { success: Boolean, _: LoggedException? ->
            CoroutineScope(Dispatchers.Main).launch {
                nextButton.hideLoading(R.string.save)
                if (success) {
                    Toast.makeText(context, getString(R.string.locations_save_site_success), Toast.LENGTH_SHORT).show()
                    goToDoneScreen()
                } else {
                    showErrorMessage(getString(R.string.locations_error_save_location), activity)
                }
            }

            Unit // empty return value
        }

        if (site.siteID != null) {
            FirebaseManager.instance?.logEvent(AnalyticsEvents.SITE_ADDED)
            FirebaseManager.instance?.saveOrUpdateSite(site, callback)
        } else {
            FirebaseManager.instance?.logEvent(AnalyticsEvents.POI_ADDED)
            FirebaseManager.instance?.saveOrUpdatePOI(site, false, callback)
        }

    }

    private fun goToDoneScreen() {
        val fragment = AddNewSiteDoneFragment()
        fragmentManager?.beginTransaction()
                ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(fragment.tag)
                ?.commit()
    }
}