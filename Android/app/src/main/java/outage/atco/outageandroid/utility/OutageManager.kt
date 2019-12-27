package outage.atco.outageandroid.utility

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import outage.atco.outageandroid.models.LocationOutageType
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.outages.ActiveOutage
import outage.atco.outageandroid.models.outages.BaseOutage
import outage.atco.outageandroid.models.outages.PlannedOutage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

enum class OutageLayer(val type: String) {
    Active("active"),
    Impact("impact"),
    Scheduled("scheduled"),
    Planned("planned"),
    PlannedImpact("plannedImpact"),
    Saved("saved"),
    Territory("territory")
}

class OutageManager private constructor(): ESRIDBManager.TokenRefreshResult {

    companion object {
        val instance by lazy { OutageManager() }
    }

    interface OutageDataListener {
        fun onOutageDataSuccess(layer: OutageLayer)
        fun onOutageDataFailure(exception: LoggedException?, layer: OutageLayer?)
    }

    interface UserDataListener {
        fun onUserDataSuccess()
        fun onUserDataFailure(exception: LoggedException?)
    }

    var queryOutageDataListener: OutageDataListener? = null
    var queryUserDataListener: UserDataListener? = null

    var territoryBoundariesSet: ArrayList<ArrayList<LatLng>>? = null
    var territoryPolygons = mutableListOf<Polygon?>()

    var activeOutageList = listOf<ActiveOutage>()
    var plannedOutageList = mutableListOf<PlannedOutage>()
    var impactingActiveOutageList = listOf<ActiveOutage>()
    var sitesImpactedByPlannedOutage = listOf<SavedLocation>()

    var userSavedLocations = mutableListOf<SavedLocation>()
    private var userSites = mutableListOf<SavedLocation>()
    private var userPOI = mutableListOf<SavedLocation>()

    fun removeQueryListeners() {
        queryUserDataListener = null
        queryOutageDataListener = null
    }

    /// QUERIES
    fun queryOutageData() {
        ESRIDBManager.instance.checkAndRefreshToken(this)
    }

    override fun onTokenRefreshSuccess() {
        ESRIDBManager.instance.queryActiveOutages(activeOutageQueryResultCallback)
        ESRIDBManager.instance.queryPlannedOutageWithBoundaries(outageBoundariesQueryResultCallback)
        ESRIDBManager.instance.queryTerritoryBoundaries(callback = territoryBoundariesQueryResultCallback)

        FirebaseManager.instance?.getSitesForCurrentUser(userSitesResultCallback)
        FirebaseManager.instance?.getPOIForCurrentUser(userPOIResultCallback)
    }

    override fun onTokenRefreshFailed() {
        queryOutageDataListener?.onOutageDataFailure(LoggedException("Token Refresh Failed"), null)
    }

    // Helper Functions
    private fun getBoundariesWhereClause(): String {
        var string = ""

        activeOutageList.forEachIndexed { index, outage ->
            string += "NUM_1 = \'${outage.outageID}\'"
            if (index < activeOutageList.lastIndex) string += " OR "
        }

        return string
    }

    private fun buildSiteIDListString(): String {
        var string  = ""

        userSites.forEachIndexed { index, savedLocation ->
            string += "SITE_ID = \'${savedLocation.siteID ?: return@forEachIndexed}\'"
            if (index < userSites.lastIndex) string += " OR "
        }

        return string
    }

    fun isLocationInServiceTerritory(location: LatLng): Boolean {
        return territoryPolygons.any {
            PolyUtil.containsLocation(location.latitude, location.longitude, it?.points, true)
        }
    }

    fun checkLocationActiveImpact(location: SavedLocation) {
        val siteIDOutage = impactingActiveOutageList.firstOrNull { it.siteID == location.siteID }
        val coordsOutage = boundariesContainLocationOrNull(activeOutageList, location)

        if (siteIDOutage != null) {
            location.outageType = LocationOutageType().active
            location.impactingOutageID = siteIDOutage.outageID
            activeOutageList.firstOrNull { it.outageID == siteIDOutage.outageID }?.isImpactingUserLocation = true
        }

        if (coordsOutage != null) {
            location.outageType = LocationOutageType().active
            location.impactingOutageID = coordsOutage.outageID
            coordsOutage.isImpactingUserLocation = true
        }
    }

    fun checkLocationPlannedImpact(location: SavedLocation) {
        val siteImpacted = sitesImpactedByPlannedOutage.firstOrNull { it.siteID == location.siteID }
        val coordsOutage = boundariesContainLocationOrNull(plannedOutageList, location)

        if (siteImpacted != null) {
            location.outageType = LocationOutageType().planned
            boundariesContainLocationOrNull(plannedOutageList, siteImpacted)?.let {
                location.impactingOutageID = it.outageID
            }
        }

        if (coordsOutage != null) {
            location.outageType = LocationOutageType().planned
            location.impactingOutageID = coordsOutage.outageID
        }
    }

    private fun boundariesContainLocationOrNull(outages: List<BaseOutage>, location: SavedLocation): BaseOutage? {
        return outages.firstOrNull {
            PolyUtil.containsLocation(
                    location.latitude ?: return@firstOrNull false,
                    location.longitude ?: return@firstOrNull false,
                    it.gpsPointsForBoundary ?: return@firstOrNull false,
                    true
            )
        }
    }

    /// CALLBACKS

    // Used for both Active Outages and Planned Outages
    @Suppress("UNCHECKED_CAST")
    private var outageBoundariesQueryResultCallback = object: Callback<OutageBoundariesQueryResult> {
        override fun onResponse(call: Call<OutageBoundariesQueryResult>, response: Response<OutageBoundariesQueryResult>){
            val data = response.body() as OutageBoundariesQueryResult
            val layer = if (call.request().url.toString().contains(PlannedOutagesURL)) OutageLayer.Planned else OutageLayer.Active

            if (data.features.isNullOrEmpty()) {
                queryOutageDataListener?.onOutageDataFailure(LoggedException("No Boundary Features Found for call to ${call.request().url}"), layer)
                return
            }

            data.features.forEach { item ->
                val outageID = item.attributes.NUM_1 ?: item.attributes.OBJECTID
                val geometryRing = item.geometry.rings as ArrayList<ArrayList<ArrayList<Double>>>
                val gpsPoints = geometryRing[0]

                val outage = activeOutageList.find { it.outageID == outageID } ?: PlannedOutage(item)
                outage.saveGPSBoundaryPoints(gpsPoints)
                if (outage is PlannedOutage) plannedOutageList.add(outage)
            }

            queryOutageDataListener?.onOutageDataSuccess(layer)
        }

        override fun onFailure(call: Call<OutageBoundariesQueryResult>, t: Throwable) {
            val isPlannedLayer = call.request().url.toString().contains(PlannedOutagesURL)
            queryOutageDataListener?.onOutageDataFailure(LoggedException(cause = t), if (isPlannedLayer) OutageLayer.Planned else OutageLayer.Active)
        }
    }

    private var impactingOutagesQueryResultCallback = object : Callback<OutageResult> {
        override fun onResponse(call: Call<OutageResult>, response: Response<OutageResult>) {
            val data = response.body() as OutageResult
            val isPlannedLayer = call.request().url.toString().contains(SitesWithPlannedImpactURL)
            val layer = if (isPlannedLayer) OutageLayer.PlannedImpact else OutageLayer.Impact

            if (data.features.isNullOrEmpty()) {
                queryOutageDataListener?.onOutageDataFailure(LoggedException("No Features Found for call to ${call.request().url}"), layer)
                return
            }

            if (isPlannedLayer) {
                sitesImpactedByPlannedOutage = data.features.map { SavedLocation(
                        siteID = it.attributes.SITE_ID,
                        latitude = it.attributes.LATITUDE.toDoubleOrNull(),
                        longitude = it.attributes.LONGITUDE.toDoubleOrNull()
                ) }
            } else {
                impactingActiveOutageList = data.features.map { ActiveOutage(it) }
            }

            queryOutageDataListener?.onOutageDataSuccess(layer)
        }

        override fun onFailure(call: Call<OutageResult>, t: Throwable) {
            val isPlannedLayer = call.request().url.toString().contains(SitesWithPlannedImpactURL)
            queryOutageDataListener?.onOutageDataFailure(LoggedException(cause = t), if (isPlannedLayer) OutageLayer.PlannedImpact else OutageLayer.Impact)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private var territoryBoundariesQueryResultCallback = object: Callback<TerritoryBoundariesQueryResult> {
        override fun onResponse(call: Call<TerritoryBoundariesQueryResult>, response: Response<TerritoryBoundariesQueryResult>) {
            val data = response.body() as TerritoryBoundariesQueryResult
            if (data.features.isNullOrEmpty()) return

            territoryBoundariesSet = arrayListOf()

            data.features.forEach { item ->
                val geometryRing = item.geometry.rings as? ArrayList<ArrayList<ArrayList<Double>>> ?: return
                for (eachSet in geometryRing) {
                    val eachTerritory = arrayListOf<LatLng>()
                    for (eachItem in eachSet) {
                        val latlon = LatLng(eachItem[1], eachItem[0])
                        eachTerritory.add(latlon)
                    }
                    territoryBoundariesSet?.add(eachTerritory)
                }
            }

            queryOutageDataListener?.onOutageDataSuccess(OutageLayer.Territory)
        }

        override fun onFailure(call: Call<TerritoryBoundariesQueryResult>, t:Throwable) {
            queryOutageDataListener?.onOutageDataFailure(LoggedException(cause = t), OutageLayer.Territory)
        }
    }

    private var activeOutageQueryResultCallback = object: Callback<OutageResult> {
        override fun onResponse(call: Call<OutageResult>, response: Response<OutageResult>){
            val data = response.body() as OutageResult

            if (data.features.isNullOrEmpty()) {
                queryOutageDataListener?.onOutageDataFailure(LoggedException("No Features Found for call to ${call.request().url}"), OutageLayer.Active)
                return
            }

            activeOutageList = data.features.map { ActiveOutage(it) }
            ESRIDBManager.instance.queryActiveOutageBoundaries(getBoundariesWhereClause(), outageBoundariesQueryResultCallback)
        }

        override fun onFailure(call: Call<OutageResult>, t: Throwable) {
            queryOutageDataListener?.onOutageDataFailure(LoggedException(cause = t), OutageLayer.Active)
        }
    }

    private var userSitesResultCallback: (List<SavedLocation>?, LoggedException?) -> Unit = { list, exception ->
        if (exception != null || list?.isEmpty() != false) {
            userSites.clear()
            userSavedLocations.clear()
            userSavedLocations.addAll(userSites)
            userSavedLocations.addAll(userPOI)
            queryOutageDataListener?.onOutageDataSuccess(OutageLayer.Impact)
            queryOutageDataListener?.onOutageDataSuccess(OutageLayer.PlannedImpact)
            queryUserDataListener?.onUserDataFailure(exception ?: LoggedException("No Saved Sites Found"))
        } else {
            userSites.clear()
            userSites.addAll(list)

            userSavedLocations.clear()
            userSavedLocations.addAll(userSites)
            userSavedLocations.addAll(userPOI)

            if (userSites.isNotEmpty()) {
                val siteIDs = buildSiteIDListString()
                if (siteIDs.isNotEmpty()) {
                    ESRIDBManager.instance.queryImpactingActiveOutages(siteIDs, impactingOutagesQueryResultCallback)
                    ESRIDBManager.instance.queryImpactingPlannedOutages(siteIDs, impactingOutagesQueryResultCallback)
                } else {
                    Log.d("Outages", "No Site IDs to Query for Impact")
                    queryOutageDataListener?.onOutageDataSuccess(OutageLayer.Impact)
                    queryOutageDataListener?.onOutageDataSuccess(OutageLayer.PlannedImpact)
                }
            }

            queryUserDataListener?.onUserDataSuccess()
        }
    }

    private var userPOIResultCallback: (List<SavedLocation>?, LoggedException?) -> Unit = { list, exception ->
        if (exception != null || list?.isEmpty() != false) {
            userPOI.clear()
            userSavedLocations.clear()
            userSavedLocations.addAll(userSites)
            userSavedLocations.addAll(userPOI)
            queryOutageDataListener?.onOutageDataSuccess(OutageLayer.Impact)
            queryUserDataListener?.onUserDataFailure(exception ?: LoggedException("No Saved POI Found"))
        } else {
            userPOI.clear()
            userPOI.addAll(list)

            userSavedLocations.clear()
            userSavedLocations.addAll(userSites)
            userSavedLocations.addAll(userPOI)

            queryUserDataListener?.onUserDataSuccess()
        }
    }

}


/*
Example Data


1. query consolidate outages, service 0

"features": [
    {
      "attributes": {
        "OBJECTID": 9,
        "NUM_1": "D865448",
        "SUB_TYCOD": "CONFIRM",
        "DEV_NAME": "S12941",
        "NUM_CUST": 519,
        "EST_REP_TIME": 631152000000,
        "EST_REP_TIME_FMT": "JAN-01 12:00 AM",
        "OFF_DTS": 1543854046000,
        "OFF_DTS_FMT": "DEC-03 4:20 PM",
        "UPDATE_DTS": 1543854047000,
        "UPDATE_DTS_FMT": "DEC-03 4:20 PM",
        "OUTAGE_TYPE_DESC": "Unplanned Trouble Call",
        "EVENT_STATUS_DESCRIPTION": "Work in Progress",
        "CAUSE_CODE": "Unknown",
        "SUPPLEMENTAL_1": "TBD",
        "SUPPLEMENTAL_2": "TBD",
        "NUM_CUST_FMT": "519"
      },
      "geometry": {
        "x": -119.59399356548371,
        "y": 56.00334618082371
      }
    }

    2. processConsolidateOutage

    3.  queryBoundariesOutage, service 6


    "features": [
    {
      "attributes": {
        "OBJECTID": 9,
        "CUST_CNT": 519,
        "NUM_1": "D865448",
        "NUM_CUST": 519,
        "SCALE": null,
        "EVENT_STATUS_DESCRIPTION": "Active",
        "CAUSE_CODE": "Unknown",
        "SUPPLEMENTAL_1": "TBD",
        "LOCATION": null,
        "CENTROID_LONG": null,
        "CENTROID_LAT": null,
        "Shape__Area": 0.227880576293501,
        "Shape__Length": 1.92965651919082
      },
      "geometry": {
        "rings": [
          [
            [
              -119.524323368994,
              55.7975341660512
            ],
            [
              -119.524446408866,
              55.7975314198392
            ],
            [
              -119.524569536554,
              55.7975322451546
            ],
            [
              -119.524692425579,
              55.7975366398085
            ],
            ....
          ]
        ]
      }

      4. outageBoundariesQueryResultCallback,

      checkCustomerSiteIDImpactedByOutage --> queryActiveOutage() service 1

      .....
      {
      "attributes": {
        "OBJECTID": 6106,
        "EST_REP_TIME": 631152000000,
        "OFF_DTS": 1543854046000,
        "UPDATE_DTS": 1543854047000,
        "OUTAGE_TYPE_DESC": "Unplanned Trouble Call",
        "EVENT_STATUS_DESCRIPTION": "Active",
        "CAUSE_CODE": "Unknown",
        "SUPPLEMENTAL_1": "TBD",
        "SITE_ID": "0010051391312",
        "SITE_ID_EXCLUDE_REASON": "Open",
        "NUM_1": "D865448",
        "NUM_CUST": 519,
        "LATITUDE": 56.04196341,
        "LONGITUDE": -119.56832554
      },
      "geometry": {
        "x": -119.56832542909112,
        "y": 56.04196341543611
      }
    }
    .....



 */