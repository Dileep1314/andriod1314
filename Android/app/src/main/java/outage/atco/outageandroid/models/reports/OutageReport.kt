package outage.atco.outageandroid.models.reports

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import outage.atco.outageandroid.utility.FirebaseManager

@Parcelize
class OutageReport(
        val siteID: String?,
        var comments: String?       = null,
        var isPowerOut: Boolean?    = null,
        var userID: String?         = FirebaseManager.instance?.user?.uid
): BaseReport(), Parcelable