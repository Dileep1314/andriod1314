package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_email_reset.*
import outage.atco.outageandroid.R

class EmailResetFragment(val email: String? = null): Fragment(R.layout.fragment_email_reset), BackPressFragment {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_reset_password)

        emailCodeContinue.setOnClickListener {
            (activity as? AuthActivity)?.openFragment(PasswordResetFragment(email))
        }
    }

    override fun onBackPressed(): Boolean {
        activity?.finish()
        return false
    }
}