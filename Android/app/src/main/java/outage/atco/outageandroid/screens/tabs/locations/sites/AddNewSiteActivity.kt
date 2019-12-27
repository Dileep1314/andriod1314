package outage.atco.outageandroid.screens.tabs.locations.sites

import android.os.Bundle
import com.google.android.libraries.places.api.model.Place
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.screens.ConnectivityActivity

class AddNewSiteActivity: ConnectivityActivity(R.layout.activity_empty_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requireSiteID = intent?.getBooleanExtra("requireSiteID", false) ?: false
        val place = intent?.getParcelableExtra<Place>("place")

        var site = SavedLocation()
        if (place != null) {
            site = SavedLocation(
                    siteName = place.name, locality = place.address, latitude = place.latLng?.latitude,
                    longitude = place.latLng?.longitude, poiName = place.name)
        }

        val fragment = AddNewSiteFragment(site, requireSiteID)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}