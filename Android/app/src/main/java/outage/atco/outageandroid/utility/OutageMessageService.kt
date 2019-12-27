package outage.atco.outageandroid.utility

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.MainActivity


class OutageMessageService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.

        sendNotification(remoteMessage)
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: getString(R.string.notification_channel)
        val body = remoteMessage.notification?.body ?: return

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("title", title)
        intent.putExtra("body", body)

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Channel previously setup in StartupActivity
        val channelID = getString(R.string.notification_channel)
        val bitmap = ContextCompat.getDrawable(this, R.mipmap.ic_launcher_round)?.toBitmap()

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelID)
                .setLargeIcon(bitmap)
                .setContentText(body.substringBefore("-"))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.primaryBlue))
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(R.drawable.atco_logo)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())

    }

    override fun onNewToken(token: String) {
        FirebaseManager.instance?.savePushToken(token)
    }
}