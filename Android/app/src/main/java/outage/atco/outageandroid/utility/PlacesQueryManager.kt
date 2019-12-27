package outage.atco.outageandroid.utility

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import outage.atco.outageandroid.BuildConfig

class PlacesQueryManager private constructor() {

    private val provincialBounds = RectangularBounds.newInstance(
            LatLngBounds.builder()
                    .include(LatLng(61.0, -121.0)) // Top Left corner of province
                    .include(LatLng(48.8, -109.0)) // Bottom right corner of province
                    .build()
    )

    private var sessionToken: AutocompleteSessionToken? = null

    companion object {
        var instance: PlacesQueryManager? = null
        var client: PlacesClient? = null

        fun setupInstance(appContext: Context) {
            instance = PlacesQueryManager()

            Places.initialize(appContext, BuildConfig.GEO_API_KEY)
            client = Places.createClient(appContext)
        }
    }

    fun startNewSession() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }

    // Only call when we are sure we're done with the Places Session (either Place returned or search cancelled/failed)
    private fun cancelSession() {
        sessionToken = null
    }

    fun sendQuery(query: String, callback: (List<AutocompletePrediction>?, Exception?) -> Unit) {
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        if (sessionToken == null) startNewSession()

        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request = FindAutocompletePredictionsRequest.builder()
                // Restrict to Alberta to speed up search
                .setLocationRestriction(provincialBounds)
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build()

        client?.findAutocompletePredictions(request)?.addOnSuccessListener {
            callback(it.autocompletePredictions, null)
        }?.addOnFailureListener { exception ->
            exception.log()
            callback(null, exception)
            cancelSession()
        }
    }

    fun fetchPlace(id: String, callback: (Place?, Exception?) -> Unit) {
        if (sessionToken == null) {
            return callback(null, LoggedException(cause = Throwable("Tried to Fetch Place with Invalid Token")))
        }
        val fields = listOf(
                Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ID
        )
        val request = FetchPlaceRequest.builder(id, fields)
                .setSessionToken(sessionToken)
                .build()

        client?.fetchPlace(request)?.addOnSuccessListener {
            callback(it.place, null)
            cancelSession()
        }?.addOnFailureListener { exception ->
            callback(null, exception.log())
            cancelSession()
        }
    }
}