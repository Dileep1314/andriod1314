package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_email_verification.*
import outage.atco.outageandroid.R

class EmailVerificationFragment(val email: String? = null):
        Fragment(R.layout.fragment_email_verification), BackPressFragment {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_email_verification_title)

        emailCodeContinue.setOnClickListener {
            (activity as? AuthActivity)?.openFragment(SignupFragment(email))
        }
    }

    override fun onBackPressed(): Boolean {
        activity?.finish()
        return false
    }
}