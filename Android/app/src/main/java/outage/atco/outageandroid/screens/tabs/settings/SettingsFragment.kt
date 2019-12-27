package outage.atco.outageandroid.screens.tabs.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.util.Colors
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import outage.atco.outageandroid.BuildConfig
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.startup.OnboardingActivity
import outage.atco.outageandroid.screens.tabs.settings.account.AuthActivity
import outage.atco.outageandroid.screens.tabs.settings.account.AuthFlowType
import outage.atco.outageandroid.screens.tabs.settings.account.EditAccountActivity
import outage.atco.outageandroid.utility.AnalyticsScreenNames
import outage.atco.outageandroid.utility.FirebaseManager
import outage.atco.outageandroid.utility.showErrorMessage

class SettingsFragment: Fragment(R.layout.fragment_settings), FirebaseAuth.AuthStateListener {

    private val unlinkLoginRequestCode = 999

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainTitle.text = getString(R.string.settings)

        FirebaseManager.instance?.addAuthStateListener(this)

        settingsVersionText.text = getString(R.string.settings_app_version, BuildConfig.VERSION_NAME)

        settingsCallATCOButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:18006682248")
            startActivity(intent)
        }

        settingsLibrariesButton.setOnClickListener {
            val blue = ContextCompat.getColor(context ?: return@setOnClickListener, R.color.primaryBlue)
            LibsBuilder().withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withAboutIconShown(false)
                    .withActivityColor(Colors(blue, blue))
                    .withActivityTitle(getString(R.string.settings_third_party_libaries))
                    .withLicenseShown(true)
                    .start(context ?: return@setOnClickListener)
        }

        settingsTermsButton.setOnClickListener {
            startActivity(Intent(context, TermsOfServiceActivity::class.java))
        }

        settingsAppTourButton.setOnClickListener {
            val tourIntent = Intent(context, OnboardingActivity::class.java)
            tourIntent.putExtra("fromSettings", true)
            startActivity(tourIntent)
        }

        settingsSignInOrOutButton.setOnClickListener {
            if (FirebaseManager.instance?.user == null) {
                startActivity(Intent(context, AuthActivity::class.java))
            } else {
                handleLogout()
            }
        }

        settingsEditAccountButton.setOnClickListener {
            startActivity(Intent(context, EditAccountActivity::class.java))
        }

        settingsUnlinkAccountButton.setOnClickListener {
            AlertDialog.Builder(context ?: return@setOnClickListener, R.style.AppTheme_Dialog)
                    .setTitle(R.string.settings_unlink_prompt)
                    .setMessage(R.string.settings_unlink_prompt_details)
                    .setPositiveButton(R.string.settings_unlink_prompt) { _, _ ->
                        val authIntent = Intent(context, AuthActivity::class.java)
                        authIntent.putExtra("destination", AuthFlowType.Unlink.type)
                        startActivityForResult(authIntent, unlinkLoginRequestCode)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    override fun onResume() {
        super.onResume()

        FirebaseManager.instance?.setCurrentScreen(activity ?: return, AnalyticsScreenNames.SETTINGS_TAB)
    }

    override fun onDestroy() {
        FirebaseManager.instance?.removeAuthStateListener(this)

        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == unlinkLoginRequestCode && resultCode == Activity.RESULT_OK) {
            FirebaseManager.instance?.deleteUser { success, _ ->
                if (success) {
                    FirebaseManager.instance?.signOut()
                    Toast.makeText(context, getString(R.string.settings_unlink_success), Toast.LENGTH_SHORT).show()
                } else {
                    showErrorMessage(getString(R.string.settings_error_unlink_account), activity)
                }
            }
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        settingsSignInOrOutButton.text = getString(if (auth.currentUser == null) R.string.register_login else R.string.settings_logout)
        settingsEditAccountButton.isEnabled = auth.currentUser != null
        settingsUnlinkAccountButton.isEnabled = auth.currentUser != null
    }

    private fun handleLogout() {
        AlertDialog.Builder(context ?: return, R.style.AppTheme_Dialog)
                .setTitle(R.string.settings_logout_prompt)
                .setMessage(R.string.settings_logout_prompt_details)
                .setPositiveButton(R.string.settings_logout) {_, _ ->
                    FirebaseManager.instance?.signOut()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

}