package outage.atco.outageandroid.screens.tabs.locations.sites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.app_bar_general.*
import outage.atco.outageandroid.R

class SiteVerificationExplanationActivity: AppCompatActivity(R.layout.activity_verification_explanation) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generalTitle.text = getString(R.string.locations_verification_status)
    }
}