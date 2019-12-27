package outage.atco.outageandroid.screens.startup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.screens.MainActivity
import outage.atco.outageandroid.utility.AppDataManager

class StartupActivity : ConnectivityActivity(R.layout.activity_startup) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDataManager.setupInstance(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val notificationChannel = NotificationChannel(
                    getString(R.string.notification_channel),
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { notificationChannel.setShowBadge(false) }

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(notificationChannel)
        }
    }

    override fun onResume() {
        super.onResume()

        if (AppDataManager.instance?.hasUserCompletedOnboarding == true) {
            val mainIntent = Intent(this, MainActivity::class.java)

            // Get Data object from parent Intent extras. This data object is sent when FCM displays a notification while app is in background and the user taps on it
            // Note: Currently, our notifications do not send extra data, so this is not currently used but may be in the future.
            if (intent?.extras?.containsKey("data") == true) {
                mainIntent.putExtras(intent)
            }

            return startActivity(mainIntent)
        }

        startActivity(Intent(this, OnboardingActivity::class.java))
    }
}
