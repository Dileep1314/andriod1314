package outage.atco.outageandroid.screens.tabs.reports

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_report_outage_site.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.reports.OutageReport
import outage.atco.outageandroid.screens.tabs.locations.sites.AddNewSiteActivity
import outage.atco.outageandroid.utility.OutageManager
import outage.atco.outageandroid.utility.showErrorMessage

class ReportOutageSiteFragment: Fragment(R.layout.fragment_report_outage_site) {

    private val layoutManager
        get() = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.reports_power_outage)
        reportOutageSiteRecycler.layoutManager = layoutManager

        reportAddSiteButton.setOnClickListener {
            val intent = Intent(context, AddNewSiteActivity::class.java)
            intent.putExtra("requireSiteID", true)
            startActivity(intent)
        }

    }

    fun goToOutageDetails(site: SavedLocation) {
        if (site.verified != 1) return showErrorMessage(getString(R.string.reports_error_not_verified), activity)
        val report = OutageReport(siteID = site.siteID)
        val fragment = ReportDetailsFragment(ReportType.Outage.string, report)

        fragmentManager?.beginTransaction()
            ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(fragment.tag)
            ?.commit()
    }

    override fun onResume() {
        super.onResume()

        reportOutageSiteRecycler.adapter = ReportOutageSiteAdapter(OutageManager.instance.userSavedLocations, this)
        reportOutageSiteRecycler.adapter?.notifyDataSetChanged()
    }
}

