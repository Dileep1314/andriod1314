package outage.atco.outageandroid.models.reports

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
class GeneralIssueReport(
        var address: String?        = null,
        var city: String?           = null,
        var prov: String?           = null,
        var comments: String?       = null,
        @SerializedName("lat")
        var latitude: Double?       = null,
        @SerializedName("long")
        var longitude: Double?      = null,
        var problemType: String?    = null
): BaseReport(), Parcelable