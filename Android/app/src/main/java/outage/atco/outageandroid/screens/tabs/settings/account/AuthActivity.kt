package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.utility.AppDataManager

class AuthActivity: ConnectivityActivity(R.layout.activity_empty_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = intent?.extras?.getString("destination")
        val code = intent?.extras?.getString("code")
        val email = AppDataManager.instance?.pendingVerificationEmail

        var fragment: Fragment = StartAuthFragment()

        // If destination or email is not valid, defaults to StartAuthFragment
        destination?.let {
            fragment = when (it) {
                AuthFlowType.Register.type -> SignupFragment(email ?: return@let, code)
                AuthFlowType.Reset.type -> PasswordResetFragment(email ?: return@let, code)
                AuthFlowType.Unlink.type -> LoginFragment(AppDataManager.instance?.userProfile?.email ?: return@let)
                else -> StartAuthFragment()
            }
        }

        // Don't add fragment to back stack, as its the base view
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.fragments.last()
        ((fragment as? BackPressFragment)?.onBackPressed() ?: true).let {
            if (it) super.onBackPressed()
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(fragment.tag)
                .commit()
    }
}

/**
 * Returns whether the onBackPressed default behaviour should also execute
 */
interface BackPressFragment {
    fun onBackPressed(): Boolean
}

enum class AuthFlowType(val type: String) {
    Login("login"),
    Register("register"),
    Reset("reset"),
    Unlink("unlink"),
    None("none")
}