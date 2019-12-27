package outage.atco.outageandroid.screens.tabs.reports

import android.os.Bundle
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.reports.GeneralIssueReport
import outage.atco.outageandroid.screens.ConnectivityActivity

class ReportIssueActivity: ConnectivityActivity(R.layout.activity_empty_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getStringExtra("type") ?: ReportType.General.string
        val report = intent.getParcelableExtra("report") ?: GeneralIssueReport()

        val fragment = if (type == ReportType.Outage.string) ReportOutageSiteFragment() else ReportLocationFragment(type, report)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}

enum class ReportType(val string: String) {
    Outage("outage"), Streetlight("light"), General("general")
}