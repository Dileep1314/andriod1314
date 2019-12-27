package outage.atco.outageandroid.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.inflate

abstract class ConnectivityActivity(@LayoutRes layoutRes: Int): AppCompatActivity(layoutRes) {

    interface ConnectivityListener {
        fun onConnectionStatusUpdate(isConnected: Boolean)
    }

    private val connectivityManager by lazy { this.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    private var statusBarColor: Int? = null
    private var connectivityListeners = mutableListOf<ConnectivityListener?>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            connectivityListeners.onEach{ it?.onConnectionStatusUpdate(true) }
            hideOfflineAlert()
        }

        override fun onLost(network: Network?) {
            connectivityListeners.onEach{ it?.onConnectionStatusUpdate(false) }
            showOfflineAlert()
        }

        override fun onUnavailable() {
            connectivityListeners.onEach{ it?.onConnectionStatusUpdate(false) }
            showOfflineAlert()
        }
    }

    override fun onResume() {
        super.onResume()

        registerNetworkCallback()

        val connected = connectivityManager?.activeNetwork != null

        connectivityListeners.onEach { it?.onConnectionStatusUpdate(connected) }
        if (!connected) showOfflineAlert()
}

    override fun onPause() {
        super.onPause()

        unregisterNetworkCallback()
        hideOfflineAlert()
    }

    fun addConnectivityListener(listener: ConnectivityListener) {
        connectivityListeners.add(listener)
        listener.onConnectionStatusUpdate(connectivityManager?.activeNetwork != null)
    }

    fun removeConnectivityListener(listener: ConnectivityListener) {
        connectivityListeners.remove(listener)
    }

    private fun registerNetworkCallback() {
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)
    }

    private fun unregisterNetworkCallback() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    private fun showOfflineAlert() {
        runOnUiThread {
            val banner = activityDecorView?.inflate(R.layout.offline_banner)
            banner?.tag = "offlineBanner"
            activityDecorView?.addView(banner, -1)
            statusBarColor = window.statusBarColor
            window.statusBarColor = ContextCompat.getColor(this, R.color.primaryYellow)
        }
    }

    private fun hideOfflineAlert() {
        runOnUiThread {
            val banner = activityDecorView?.findViewWithTag<View>("offlineBanner")
            activityDecorView?.removeView(banner)
            window.statusBarColor = statusBarColor
                    ?: ContextCompat.getColor(this, R.color.primaryBlue)
        }
    }

    private val activityDecorView: ViewGroup?
        get() {
            return this.window.decorView as? ViewGroup
        }
}