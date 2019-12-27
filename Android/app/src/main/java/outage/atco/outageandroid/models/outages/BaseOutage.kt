package outage.atco.outageandroid.models.outages

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.*

open class BaseOutage(
        var outageID: String                            = "",
        var latitude: Double?                           = null,
        var longitude: Double?                          = null,
        var gpsPointsForBoundary: MutableList<LatLng>?  = null,
        var status: String                              = "Unknown",
        var startDate: Date?                            = null,
        var finishDate: Date?                           = null,
        var customersAffected: Int                      = 0,
        var isImpactingUserLocation: Boolean            = false,
        var zoomBounds: LatLngBounds?                   = null) {

    fun saveGPSBoundaryPoints(data: ArrayList<ArrayList<Double>>, centerPointLat: Double? = null, centerPointLong: Double? = null) {

        gpsPointsForBoundary = mutableListOf()
        val builder = LatLngBounds.Builder()

        data.forEach{
            gpsPointsForBoundary?.add(LatLng(it[1],it[0]))
            builder.include(LatLng(it[1],it[0]))
        }

        zoomBounds = builder.build()
        val center = zoomBounds?.center

        if (centerPointLat == null || centerPointLong == null) {
            latitude = center?.latitude
            longitude = center?.longitude
        } else {
            latitude = centerPointLat
            longitude = centerPointLong
        }

    }

}