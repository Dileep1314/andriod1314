package outage.atco.outageandroid.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import outage.atco.outageandroid.R

typealias OutageType = Pair<Int?, Int>
data class LocationOutageType(
    val active: OutageType              = Pair(R.drawable.ic_map_impact_outage, R.color.impactingOutageBoundaryColor),
    val activePlanned: OutageType       = Pair(R.drawable.ic_map_active_scheduled_outage, R.color.activeOutageBoundaryColor),
    val noOutage: OutageType            = Pair(R.drawable.ic_map_saved_location, R.color.primaryBlue),
    val planned: OutageType             = Pair(R.drawable.ic_map_planned_outage, R.color.plannedOutageBoundaryColor)
)

val VerificationStatusStrings = listOf("Not Verified", "Verified", "Mismatch", "Site ID Not Found", "Unregistered")

@Parcelize
class SavedLocation(
        var uniqueID: String?           = null,
        var siteID: String?             = null,
        var siteName: String?           = null,
        var locality: String?           = null,
        var latitude: Double?           = null,
        var longitude: Double?          = null,
        var outageType: OutageType?     = LocationOutageType().noOutage,
        var impactingOutageID: String?  = null,
        var isEmailEnabled: Boolean?    = null,
        var isPushEnabled: Boolean?     = null,
        var isTextEnabled: Boolean?     = null,
        var isVerboseMsg: Boolean?      = null,
        var verified: Int?              = null,
        @SerializedName("name")
        var poiName: String?            = null
): Cloneable, Parcelable {

        public override fun clone(): SavedLocation {
                return super.clone() as SavedLocation
        }
}