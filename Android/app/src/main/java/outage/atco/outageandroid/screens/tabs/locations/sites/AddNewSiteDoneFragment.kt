package outage.atco.outageandroid.screens.tabs.locations.sites

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_add_new_site_done.*
import outage.atco.outageandroid.R

class AddNewSiteDoneFragment: Fragment(R.layout.fragment_add_new_site_done) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.locations_add_site_title)

        doneButton.setOnClickListener {
            activity?.finish()
        }
    }
}