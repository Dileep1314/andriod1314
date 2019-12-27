package outage.atco.outageandroid.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import kotlinx.android.synthetic.main.activity_main.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.tabs.MainTabsAdapter
import outage.atco.outageandroid.screens.tabs.locations.MapFragment
import outage.atco.outageandroid.screens.tabs.settings.account.AuthActivity
import outage.atco.outageandroid.screens.tabs.settings.account.AuthFlowType
import outage.atco.outageandroid.utility.AnalyticsEvents
import outage.atco.outageandroid.utility.FirebaseManager

class MainActivity: ConnectivityActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        mainTabs.setOnNavigationItemSelectedListener { tab ->
            mainTabPager.setCurrentItem(when (tab.itemId) {
                R.id.navigation_reports -> 1
                R.id.navigation_learn -> {
                    FirebaseManager.instance?.logEvent(AnalyticsEvents.LEARN_HOME_VISITED)
                    2
                }
                R.id.navigation_settings -> 3
                else -> 0
            }, true)

            return@setOnNavigationItemSelectedListener true
        }

        mainTabPager.offscreenPageLimit = 4
        mainTabPager.adapter = MainTabsAdapter(supportFragmentManager)
        mainTabPager.beginFakeDrag()

        mainTabs.menu.performIdentifierAction(R.id.navigation_map, 0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleDeepLink(intent ?: return)

        // Show message if needed. If no message, then function will cancel itself silently
        showMessage(intent)
    }

    private fun showMessage(messageIntent: Intent) {
        messageIntent.extras?.let {
            val title = it["title"] as? String ?: getString(R.string.notification_channel)
            val message = it["body"] as? String ?: return@let

            mainTabs.menu.performIdentifierAction(R.id.navigation_map, 0)
            val item = (mainTabPager.adapter as? MainTabsAdapter)?.getItem(0)
            (item as? MapFragment)?.refreshMap()

            AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setPositiveButton(R.string.dismiss, null)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(R.mipmap.ic_launcher_round)
                    .show()
        }
    }

    private fun handleDeepLink(linkIntent: Intent){
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(linkIntent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                val linkUrl = pendingDynamicLinkData?.link ?: return@addOnSuccessListener

                var code = intent?.data?.getQueryParameter("link")
                code?.let { if (it.length != 6) code = "" }

                with (linkUrl.toString()) {
                    when {
                        contains("emailRegistration", true) -> {
                            val authIntent = Intent(applicationContext, AuthActivity::class.java)
                            code?.let { authIntent.putExtra("code", it) }
                            authIntent.putExtra("destination", AuthFlowType.Register.type)
                            startActivity(authIntent)
                        }
                        contains("resetPassword", true) -> {
                            val authIntent = Intent(applicationContext, AuthActivity::class.java)
                            code?.let { authIntent.putExtra("code", it) }
                            authIntent.putExtra("destination", AuthFlowType.Reset.type)
                            startActivity(authIntent)
                        }
                        contains("reportoutage", true) -> {
                            mainTabs.menu.performIdentifierAction(R.id.navigation_reports, 0)
                        }
                        else -> {
                            // "backtoapp", "T8Ap", "setnotification"
                            mainTabs.menu.performIdentifierAction(R.id.navigation_map, 0)
                            val item = (mainTabPager.adapter as? MainTabsAdapter)?.getItem(0)
                            (item as? MapFragment)?.refreshMap()
                        }
                    }
                }

            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    override fun onBackPressed() {
        if (mainTabPager.currentItem == 0) {
            AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setTitle(R.string.exit_app_prompt)
                    .setMessage(R.string.exit_app_prompt_details)
                    .setPositiveButton(R.string.yes) { _, _ -> finishAffinity() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            mainTabs.menu.findItem(R.id.navigation_map).isChecked = true
            mainTabPager.setCurrentItem(0, true)
        }
    }

}

