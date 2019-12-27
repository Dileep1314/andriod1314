package outage.atco.outageandroid.utility

import com.google.gson.GsonBuilder
import outage.atco.outageandroid.BuildConfig.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.*
import kotlin.collections.ArrayList

const val TokenURL                  = "token?"
const val ActiveOutagesURL          = "MobileOutageService_V1/FeatureServer/0/query?"
const val ImpactingActiveOutagesURL = "MobileOutageService_V1/FeatureServer/1/query?"
const val SitesWithPlannedImpactURL = "MobileOutageService_V1/FeatureServer/2/query?"
const val PlannedOutagesURL         = "MobileOutageService_V1/FeatureServer/3/query?"
const val OutageBoundariesURL       = "MobileOutageService_V1/FeatureServer/6/query?"
const val TerritoriesURL            = "MobileOutageService_V1/FeatureServer/7/query?"

interface LoginAPI {
    @POST(TokenURL)
    @FormUrlEncoded
    fun getAccessToken(
            @Field("client_id") client_id: String,
            @Field("client_secret") client_secret: String,
            @Field("grant_type") grant_type: String
    ): Call<AccessToken>
}

interface OutageESRIAPI {
    @POST(ActiveOutagesURL)
    @FormUrlEncoded
    fun activeOutageRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<OutageResult>

    @POST(ImpactingActiveOutagesURL)
    @FormUrlEncoded
    fun impactingActiveOutageRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<OutageResult>

    @POST(SitesWithPlannedImpactURL)
    @FormUrlEncoded
    fun impactingPlannedOutageRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<OutageResult>

    @POST(PlannedOutagesURL)
    @FormUrlEncoded
    fun plannedOutageBoundariesRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<OutageBoundariesQueryResult>

    @POST(OutageBoundariesURL)
    @FormUrlEncoded
    fun outageBoundariesRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<OutageBoundariesQueryResult>

    @POST(TerritoriesURL)
    @FormUrlEncoded
    fun territoryBoundariesRequest(
            @Field("where") where: String,
            @Field("outFields") outFields: String,
            @Field("sqlFormat") sqlFormat: String,
            @Field("f") f: String,
            @Field("token") token: String
    ): Call<TerritoryBoundariesQueryResult>
}

class AccessToken(
    var access_token: String?       = null,
    var expires_in: Int?            = null,
    var expires_millisecond: Long?  = 0,
    var error: Any?                 = null
)

// OUTAGES
data class OutageAttributes(
        val CAUSE_CODE: String,
        val EVENT_STATUS_DESCRIPTION: String,
        val NUM_1: String,
        val NUM_CUST: Int,
        val EST_REP_TIME: Double,
        val OFF_DTS: Double,
        val UPDATE_DTS: Double,
        val SUPPLEMENTAL_1: String,
        val SITE_ID_EXCLUDE_REASON: String,
        val SITE_ID: String,
        val LATITUDE: String,
        val LONGITUDE: String)

data class OutageGeometryData(val x: Double, val y: Double)

data class OutageFeature(val attributes: OutageAttributes, val geometry: OutageGeometryData)

data class OutageResult(val features: ArrayList<OutageFeature>)

// OUTAGE BOUNDARIES
data class OutageBoundariesQueryFeatureAttributes(
        val OBJECTID: Int?,
        val CUST_CNT: Int,
        val NUM_1: String?,
        val NUM_CUST: Int,
        val SCALE: String?,
        val EVENT_STATUS_DESCRIPTION: String,
        val CAUSE_CODE: String?,
        val SUPPLEMENTAL_1: String?,
        val STARTDATE: Double,
        val ENDDATE: Double,
        val DESCOFWORK: String,
        val NUMOFCUSTOMEREFFECTED: Int,
        val OUTAGECUSTOMERSKEY: String,
        val STATUS: String,
        val Shape__Area: Double,
        val Share__Length: Double)

data class OutageBoundariesGeometryData(val rings: Any) //ArrayList<ArrayList<Double>>

data class OutageBoundariesQueryFeature(val attributes: OutageBoundariesQueryFeatureAttributes, val geometry: OutageBoundariesGeometryData)

data class OutageBoundariesQueryResult(val features:ArrayList<OutageBoundariesQueryFeature>)

// TERRITORIES
data class TerritoryBoundariesQueryFeatureAttributes(
        val OBJECTID: Int,
        val OBJECTID_1: Int,
        val ID_1: Int,
        val COMPANY: String,
        val Shape__Area: Double,
        val Share__Length: Double)

data class TerritoryBoundariesGeometryData(val rings: Any)

data class TerritoryBoundariesQueryFeature(val attributes: TerritoryBoundariesQueryFeatureAttributes, val geometry: TerritoryBoundariesGeometryData)

data class TerritoryBoundariesQueryResult(val features: ArrayList<TerritoryBoundariesQueryFeature>)


class ESRIDBManager private constructor() {

    private val loginService: LoginAPI
    private var outageQueryService: OutageESRIAPI

    var token: AccessToken
    var tokenRefreshListener: TokenRefreshResult? = null

    interface TokenRefreshResult {
        fun onTokenRefreshSuccess()
        fun onTokenRefreshFailed()
    }

    private object Holder { val INSTANCE = ESRIDBManager() }

    companion object {
        val instance: ESRIDBManager by lazy { Holder.INSTANCE }
    }

    init {
        token = AppDataManager.instance?.loadToken() ?: AccessToken()
        val gson = GsonBuilder().setLenient().create()

        // Uncomment to enable Logging on these calls
//        val logger = HttpLoggingInterceptor()
//        logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
//        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        val retrofitToken = Retrofit.Builder()
                .baseUrl(ESRI_TOKEN_BASE_URL)
//                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val retrofitData = Retrofit.Builder()
                .baseUrl(ESRI_BASE_URL)
//                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        loginService = retrofitToken.create(LoginAPI::class.java)
        outageQueryService = retrofitData.create(OutageESRIAPI::class.java)
    }

    // Login & Tokens
    private fun isTokenExpired(): Boolean {
        return token.expires_millisecond ?: System.currentTimeMillis() <= System.currentTimeMillis()
    }

    fun checkAndRefreshToken(listener: TokenRefreshResult?) {
        tokenRefreshListener = listener
        if (isTokenExpired()) {
            getAccessToken(object: Callback<AccessToken> {
                override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                    try {
                        token = response.body() ?: return tokenRefreshListener?.onTokenRefreshFailed() ?: Unit
                        token.expires_millisecond = System.currentTimeMillis() + (token.expires_in?.times(1000) ?: 0)

                        val accessToken = token.access_token ?: return tokenRefreshListener?.onTokenRefreshFailed() ?: Unit
                        AppDataManager.instance?.saveToken(accessToken, token.expires_millisecond ?: 0L)
                        tokenRefreshListener?.onTokenRefreshSuccess()

                    } catch(e: LoggedException) {
                        tokenRefreshListener?.onTokenRefreshFailed()
                    }
                }

                override fun onFailure(call: Call<AccessToken>, t: Throwable ) {
                    @Suppress("ThrowableNotThrown")
                    LoggedException(cause = t)
                    tokenRefreshListener?.onTokenRefreshFailed()
                }
            })
        } else {
            tokenRefreshListener?.onTokenRefreshSuccess()
        }
    }

    private fun getAccessToken(callback: Callback<AccessToken>) {
        val call = loginService.getAccessToken(ESRI_CLIENT_ID, ESRI_CLIENT_SECRET, ESRI_GRANT_TYPE)
        call.enqueue(callback)
    }

    // Queries
    fun queryActiveOutages(callback: Callback<OutageResult>) {
        val whereClause = """
            EVENT_STATUS_DESCRIPTION <> 'WAITING' AND
            EVENT_STATUS_DESCRIPTION <> 'CANCELLED' AND
            EVENT_STATUS_DESCRIPTION <> 'NON EXISTENT' AND
            EVENT_STATUS_DESCRIPTION <> 'HELD' AND
            SUPPLEMENTAL_1 <> 'CUSTOMER REQUESTED' AND
            CAUSE_CODE <> 'NON OUTAGE'
            """.trimIndent()

        val accessToken = token.access_token ?: return
        val call = outageQueryService.activeOutageRequest(whereClause, "*",
                "standard", "json", accessToken)
        call.enqueue(callback)
    }

    fun queryImpactingActiveOutages(sitesString: String, callback: Callback<OutageResult>) {
        val whereClause = """
            EVENT_STATUS_DESCRIPTION <> 'WAITING' AND
            EVENT_STATUS_DESCRIPTION <> 'CANCELLED' AND
            EVENT_STATUS_DESCRIPTION <> 'NON EXISTENT' AND
            EVENT_STATUS_DESCRIPTION <> 'HELD' AND
            SUPPLEMENTAL_1 <> 'CUSTOMER REQUESTED' AND
            CAUSE_CODE <> 'NON OUTAGE' AND
            SITE_ID_EXCLUDE_REASON <> 'Closed' AND
            ($sitesString)
            """.trimIndent()

        val accessToken = token.access_token ?: return
        val call = outageQueryService.impactingActiveOutageRequest(whereClause, "*",
                "standard", "json", accessToken)
        call.enqueue(callback)
    }

    fun queryImpactingPlannedOutages(sitesString: String, callback: Callback<OutageResult>) {
        val whereClause = """
            OBJECTID > 0 AND
            STARTDATE > date '${Date().formatServer()}' AND
            ($sitesString)
            """.trimIndent()

        val accessToken = token.access_token ?: return
        val call = outageQueryService.impactingPlannedOutageRequest(whereClause, "*",
                 "standard", "json", accessToken)
        call.enqueue(callback)
    }

    fun queryActiveOutageBoundaries(whereClause: String, callback: Callback<OutageBoundariesQueryResult>) {
        val accessToken = token.access_token ?: return

        val call = outageQueryService.outageBoundariesRequest(whereClause,
                "*","standard", "json", accessToken)
        call.enqueue(callback)
    }

    fun queryPlannedOutageWithBoundaries(callback: Callback<OutageBoundariesQueryResult>) {
        val accessToken = token.access_token ?: return
        val whereClause = "OBJECTID > 0 AND STARTDATE > date '${Date().formatServer()}'"
        val call = outageQueryService.plannedOutageBoundariesRequest(whereClause,
                "*","standard", "json", accessToken)
        call.enqueue(callback)
    }

    fun queryTerritoryBoundaries(callback: Callback<TerritoryBoundariesQueryResult>) {
        val accessToken = token.access_token ?: return

        val call = outageQueryService.territoryBoundariesRequest("OBJECTID > 0",
                "*","standard", "json", accessToken)
        call.enqueue(callback)
    }
}


/* EXAMPLE DATA

layer 6
{
      "attributes": {
        "OBJECTID": 451226,
        "CUST_CNT": 1,
        "NUM_1": "D865377",
        "NUM_CUST": 1,
        "SCALE": null,
        "EVENT_STATUS_DESCRIPTION": "Pending",
        "CAUSE_CODE": "Tree Contacts",
        "SUPPLEMENTAL_1": "TBD",
        "LOCATION": null,
        "CENTROID_LONG": null,
        "CENTROID_LAT": null,
        "Shape__Area": 0.00000271841508947546,
        "Shape__Length": 0.0063061814612538
      },
      "geometry": {
        "rings": [
          [
            [
              -119.395247197336,
              58.4963469000482
            ],
            [
              -119.395338970715,
              58.4963454707986
            ],
            ....
            [
              -119.395247197336,
              58.4963469000482
            ]
          ]
        ]
      }
    }
 */


/*
layer 0

"features": [
    {
      "attributes": {
        "OBJECTID": 2297563,
        "NUM_1": "D865377",
        "SUB_TYCOD": "CONFIRM",
        "DEV_NAME": "S108034",
        "NUM_CUST": 1,
        "EST_REP_TIME": 1540373904000,
        "EST_REP_TIME_FMT": "OCT-24 9:38 AM",
        "OFF_DTS": 1540310617000,
        "OFF_DTS_FMT": "OCT-23 4:03 PM",
        "UPDATE_DTS": 1540374189000,
        "UPDATE_DTS_FMT": "OCT-24 9:43 AM",
        "OUTAGE_TYPE_DESC": "Unplanned Trouble Call",
        "EVENT_STATUS_DESCRIPTION": "Under Investigation",
        "CAUSE_CODE": "Unknown",
        "SUPPLEMENTAL_1": "TBD",
        "SUPPLEMENTAL_2": "TBD",
        "NUM_CUST_FMT": "< 10"
      },
      "geometry": {
        "x": -119.395331306234,
        "y": 58.49701878830474
      }
    }
  ]
 */


/*
layer 1
"features": [
    {
      "attributes": {
        "OBJECTID": 9444506,
        "EST_REP_TIME": 1540373904000,
        "OFF_DTS": 1540310617000,
        "UPDATE_DTS": 1540374189000,
        "OUTAGE_TYPE_DESC": "Unplanned Trouble Call",
        "EVENT_STATUS_DESCRIPTION": "Pending",
        "CAUSE_CODE": "Tree Contacts",
        "SUPPLEMENTAL_1": "TBD",
        "SITE_ID": "0010021220223",
        "SITE_ID_EXCLUDE_REASON": "Open",
        "NUM_1": "D865377",
        "NUM_CUST": 1,
        "LATITUDE": 58.49701237,
        "LONGITUDE": -119.39531316
      },
      "geometry": {
        "x": -119.395331306234,
        "y": 58.49701878830474
      }
    }
  ]
 */
