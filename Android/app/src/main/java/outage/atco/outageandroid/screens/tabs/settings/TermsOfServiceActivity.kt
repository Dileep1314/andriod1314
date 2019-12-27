package outage.atco.outageandroid.screens.tabs.settings

import android.os.Bundle
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.screens.tabs.settings.account.AuthFlowType
import outage.atco.outageandroid.screens.tabs.settings.account.TermsOfServiceFragment

class TermsOfServiceActivity: ConnectivityActivity(R.layout.activity_empty_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = TermsOfServiceFragment(flowType = AuthFlowType.None)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

}
