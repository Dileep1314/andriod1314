package outage.atco.outageandroid.screens.tabs.reports

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_reports.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.tabs.settings.account.AuthActivity
import outage.atco.outageandroid.utility.AnalyticsScreenNames
import outage.atco.outageandroid.utility.FirebaseManager
import outage.atco.outageandroid.utility.makeCircular

class ReportsFragment: Fragment(R.layout.fragment_reports) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainTitle.text = getString(R.string.nav_reports)

        // Wait for view ready to post so that measurements are ready
        view.post {
            cardView1.makeCircular()
            cardView2.makeCircular()
            cardView3.makeCircular()
            cardView4.makeCircular()
            cardView5.makeCircular()
            cardView6.makeCircular()
        }

        reportsPowerButton.setOnClickListener {
            val intent = Intent(context, ReportIssueActivity::class.java)
            intent.putExtra("type", ReportType.Outage.string)
            startActivity(intent)
        }

        reportsGeneralButton.setOnClickListener {
            val intent = Intent(context, ReportIssueActivity::class.java)
            intent.putExtra("type", ReportType.General.string)
            startActivity(intent)
        }

        reportsLightButton.setOnClickListener {
            val intent = Intent(context, ReportIssueActivity::class.java)
            intent.putExtra("type", ReportType.Streetlight.string)
            startActivity(intent)
        }

        reportsLoginButton.setOnClickListener {
            startActivity(Intent(context, AuthActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        if (FirebaseManager.instance?.user == null) {
            notLoggedInView.visibility = View.VISIBLE
            reportsLinearLayout.visibility = View.GONE
        } else {
            notLoggedInView.visibility = View.GONE
            reportsLinearLayout.visibility = View.VISIBLE
        }

        FirebaseManager.instance?.setCurrentScreen(activity ?: return, AnalyticsScreenNames.REPORTS_TAB)
    }
}