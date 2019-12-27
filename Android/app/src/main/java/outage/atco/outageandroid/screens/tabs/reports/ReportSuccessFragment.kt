package outage.atco.outageandroid.screens.tabs.reports

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_report_success.*
import outage.atco.outageandroid.R

class ReportSuccessFragment(val type: String = ReportType.General.string):
        Fragment(R.layout.fragment_report_success) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(when (type) {
            ReportType.Outage.string -> R.string.reports_power_outage
            ReportType.Streetlight.string -> R.string.reports_streetlight_title
            else -> R.string.reports_general_issue
        })

        doneButton.setOnClickListener {
            activity?.finish()
        }
    }
}