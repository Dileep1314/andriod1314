package outage.atco.outageandroid.utility

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.GsonBuilder
import outage.atco.outageandroid.BuildConfig.REGISTRATION_BASE_URL
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.reports.GeneralIssueReport
import outage.atco.outageandroid.models.reports.OutageReport
import outage.atco.outageandroid.models.reports.StreetlightReport
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

//Tables in Database
data class DatabaseTables(
        val pushToken: String = "PushToken",
        val siteID: String = "SiteID",
        val notification: String = "notification",
        val watchList: String = "WatchList",
        val reportedOutage: String = "ReportedOutage",
        val reportedStreetLight: String = "ReportedStreetLight",
        val reportedGeneralIssue: String = "ReportedGeneralProblem",
        val feedback: String = "Feedback"
)

data class StandardResponse(
        var status: Int,
        var message: String,
        var detail: String
)

data class UserProfile(
        var firstName: String,
        var lastName: String,
        var email: String,
        var phoneNumber: String
)

data class UserExistParam(
        var email: String
)

data class VerificationEmailParam(
        var email: String,
        var useCase: String
)

data class LoginUserParam(
        var email: String,
        var password: String
)

data class HandlePasswordParam(
        var email: String,
        var password: String,
        var firebaseKey: String
)

data class CreateUserVerifiedAccountParam(
        var securityKey: String,
        var email: String,
        var firstName: String,
        var lastName: String,
        var phoneNum: String,
        var password: String,
        var recoveryQuestion: String,
        var recoveryAnswer: String
)

const val CheckUserURL              = "checkForUserAccountInOKTA"
const val EmailVerificationURL      = "sendUserEmailVerification"
const val CreateAccountURL          = "createUserVerifiedAccount"
const val LoginURL                  = "loginUser"
const val PasswordResetURL          = "handlePasswordReset"

interface RegistrationAPI {
    @POST(CheckUserURL)
    @Headers("Content-Type: application/json")
    fun checkIfUserExist(
            @Body param: UserExistParam
    ): Call<Any>


    @POST(EmailVerificationURL)
    fun sendVerificationEmail(
            @Body param: VerificationEmailParam
    ): Call<Any>


    @POST(CreateAccountURL)
    @Headers("Content-Type: application/json")
    fun createUserVerifiedAccount(
            @Body param: CreateUserVerifiedAccountParam
    ): Call<Any>


    @POST(LoginURL)
    @Headers("Content-Type: application/json")
    fun loginUser(
            @Body param: LoginUserParam
    ): Call<Any>


    @POST(PasswordResetURL)
    @Headers("Content-Type: application/json")
    fun sendHandlePassword(
            @Body param: HandlePasswordParam
    ): Call<Any>
}

class FirebaseManager private constructor() {
    var auth: FirebaseAuth? = null
    var analytics: FirebaseAnalytics? = null
    var database: DatabaseReference? = null
    val user: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser
    var functions: FirebaseFunctions? = null

    private val registrationAPI: RegistrationAPI

    init {
        val gson = GsonBuilder().setLenient().create()

        // Uncomment to enable Logging on these calls
//        val logger = HttpLoggingInterceptor()
//        logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
//        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        val retrofit = Retrofit.Builder()
                .baseUrl(REGISTRATION_BASE_URL)
//                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        registrationAPI = retrofit.create(RegistrationAPI::class.java)
    }

    companion object {
        var instance: FirebaseManager? = null

        fun setupInstance(appContext: Context) {
            FirebaseApp.initializeApp(appContext)

            instance = FirebaseManager()

            instance?.auth = FirebaseAuth.getInstance()
            instance?.database = FirebaseDatabase.getInstance().reference
            instance?.functions = FirebaseFunctions.getInstance()
            instance?.analytics = FirebaseAnalytics.getInstance(appContext)

        }

    }


    /// User Authentication
    interface OktaAuthCallback {
        //        fun onAuthenticationStart()
        fun onAuthenticationSuccess()

        fun onAuthenticationFailed(message: String)
    }

    private var authenticationListeners: MutableList<OktaAuthCallback?> = mutableListOf()

    fun signOut() {
        auth?.signOut()

        logEvent(AnalyticsEvents.LOGOUT)
        OutageManager.instance.userSavedLocations = mutableListOf()
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth?.removeAuthStateListener(listener)
    }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth?.addAuthStateListener(listener)
    }

    fun deleteUser(callback: (Boolean, LoggedException?) -> Unit) {
        user?.delete()?.addOnCompleteListener { task ->
            logEvent(AnalyticsEvents.UNLINK)
            callback(task.isSuccessful, task.exception?.log())
        }
    }

    fun checkIfUserExist(email: String, callback: Callback<Any>) {
        val call = registrationAPI.checkIfUserExist(UserExistParam(email))
        call.enqueue(callback)
    }

    fun sendVerificationEmail(email: String, useCase: String, callback: Callback<Any>) {
        val call = registrationAPI.sendVerificationEmail(VerificationEmailParam(email, useCase))
        call.enqueue(callback)
    }

    fun loginUser(email: String, password: String, callback: Callback<Any>) {
        val call = registrationAPI.loginUser(LoginUserParam(email, password))
        call.enqueue(callback)
    }

    fun createUserVerifiedAccount(securityKey: String, email: String, firstName: String, lastName: String, phoneNum: String,
                                  password: String, recoveryQuestion: String, recoveryAnswer: String, callback: Callback<Any>) {
        val param = CreateUserVerifiedAccountParam(
                securityKey, email, firstName, lastName, phoneNum, password, recoveryQuestion, recoveryAnswer
        )

        val call = registrationAPI.createUserVerifiedAccount(param)
        call.enqueue(callback)
    }

    fun sendHandlePasswordReset(email: String, password: String, firebaseKey: String, callback: Callback<Any>) {
        val call = registrationAPI.sendHandlePassword(HandlePasswordParam(email, password, firebaseKey))
        call.enqueue(callback)
    }

    fun getUserDetails(callback: (UserProfile?, LoggedException?) -> Unit) {
        functions?.getHttpsCallable("showUserProfile")?.call(user?.uid)?.continueWith {
            val result = it.result?.data as? String
                    ?: return@continueWith callback(null, LoggedException("Cannot Read User Profile"))

            val profile = gson.fromJson<UserProfile>(result, UserProfile::class.java)

            callback(profile, null)
        }
    }

    fun saveUserDetails(profile: UserProfile, callback: (Boolean, LoggedException?) -> Unit) {
        functions?.getHttpsCallable("saveUserProfile")?.call(profile.serializeToMap())?.continueWith {
            val result = it.result?.data as? String
                    ?: return@continueWith callback(false, LoggedException("Cannot Save User Profile. Please try again"))

            val response = gson.fromJson<StandardResponse>(result, StandardResponse::class.java)

            if (response.status == 200) callback(true, null) else callback(false, LoggedException(response.message))
        }
    }

    fun customTokenLogin(token: String, callback: (Boolean) -> Unit) {
        instance?.auth?.signInWithCustomToken(token)?.addOnCompleteListener {
            if (!it.isSuccessful) {
                @Suppress("ThrowableNotThrown")
                LoggedException(cause = it.exception)

                authenticationListeners.forEach { listener ->
                    listener?.onAuthenticationFailed("Sorry. We're experiencing difficulty while signing you in, please try again later!")
                }
                return@addOnCompleteListener callback(false)
            }

            authenticationListeners.onEach { listener ->
                listener?.onAuthenticationSuccess()
            }

            // Setup user info and push messaging
            getUserDetails { userProfile, loggedException ->
                if (loggedException != null) AppDataManager.instance?.userProfile = userProfile
            }
            setupPushMessaging()
            Crashlytics.setUserIdentifier(instance?.user?.uid)
            analytics?.setUserId(instance?.user?.uid)
            callback(true)
        }

    }


    /// Site Requests

    fun saveOrUpdateSite(site: SavedLocation, callback: (Boolean, LoggedException?) -> Unit) {
        val userID = user?.uid ?: return callback(false, LoggedException("User is not logged in"))

        val userPath = database?.ref?.child(DatabaseTables().siteID)?.child(userID)

        var uniqueID = site.uniqueID
        if (uniqueID == null) {
            uniqueID = userPath?.push()?.key
        }

        // Remove some keys from site before saving to db
        site.uniqueID = null
        site.outageType = null
        site.impactingOutageID = null
        site.poiName = null

        userPath?.child(uniqueID ?: return)?.updateChildren(site.serializeToMap())?.addOnSuccessListener {
            callback(true, null)
        }?.addOnFailureListener {
            callback(false, it.log())
        }
    }

//    fun getSiteForSiteID(id: String, callback: (SavedLocation?, LoggedException?) -> Unit) {
//        val path = database?.ref?.child(DatabaseTables().siteID)?.child(user?.uid ?: return)?.child(id)
//
//        path?.addListenerForSingleValueEvent(object: ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {
//                callback(null, error.toException().log())
//            }
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val location = snapshot.value.serializeToMap().toDataClass<SavedLocation>()
//                location.uniqueID = snapshot.key
//
//                callback(location, null)
//            }
//
//        })
//    }

    fun getSitesForCurrentUser(callback: (List<SavedLocation>?, LoggedException?) -> Unit) {
        if (user == null) return callback(emptyList(), null)
        val userPath = database?.ref?.child(DatabaseTables().siteID)?.child(user?.uid ?: return)

        val listener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                userPath?.removeEventListener(this)
                return callback(emptyList(), error.toException().log())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                return callback(snapshot.children.mapNotNull {
                    // If there is a problem with the child node, return null for site and log exception. Saved Site is likely malformed and unusable
                    try {
                        val location = it.value.serializeToMap().toDataClass<SavedLocation>()
                        location.uniqueID = it.key
                        location
                    } catch (e: Exception) {
                        e.log()
                        null
                    }
                }, null)

            }
        }

        userPath?.addValueEventListener(listener)
    }

    fun deleteSite(location: SavedLocation?, callback: (Boolean, LoggedException?) -> Unit) {
        val path = database?.ref?.child(DatabaseTables().siteID)?.child(user?.uid
                ?: return)?.child(location?.uniqueID ?: return)

        path?.removeValue { databaseError, _ ->
            if (databaseError == null) {
                callback(true, null)
            } else {
                callback(false, databaseError.toException().log())
            }
        }
    }


    /// POI Requests

    fun getPOIForCurrentUser(callback: (List<SavedLocation>?, LoggedException?) -> Unit) {
        // Don't throw exception as likely just not logged in
        if (user == null) return callback(emptyList(), null)

        val userPath = database?.ref?.child(DatabaseTables().watchList)?.child(user?.uid ?: return)

        val listener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                userPath?.removeEventListener(this)
                return callback(emptyList(), error.toException().log())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                return callback(snapshot.children.mapNotNull {
                    // If there is a problem with the child node, return null for POI and log exception. Saved POI is likely malformed and unusable
                    try {
                        val poi = it?.value?.serializeToMap()?.toDataClass<SavedLocation>()
                        poi?.poiName = it.key
                        poi
                    } catch (e: Exception) {
                        e.log()
                        null
                    }
                }, null)

            }
        }

        userPath?.addValueEventListener(listener)
    }

    fun saveOrUpdatePOI(poi: SavedLocation, newPOI: Boolean = true, callback: (Boolean, LoggedException?) -> Unit) {
        val userID = user?.uid ?: return callback(false, LoggedException("User is not logged in"))
        val name = poi.poiName ?: poi.siteName
        if (name.isNullOrBlank()) return callback(false, LoggedException("Location Name is required"))

        val userPath = database?.ref?.child(DatabaseTables().watchList)?.child(userID)

        if (newPOI) {
            try {
                if (userPath?.orderByChild(name) != null) return callback(false, LoggedException("POI Name already exists"))
            } catch (e: NullPointerException) {
                // Node is null, so we are safe to save to it
            }
        }

        // Remove unneeded keys from poi before saving to db
        poi.uniqueID = null
        poi.outageType = null
        poi.impactingOutageID = null
        poi.siteName = null
        poi.poiName = null
        poi.isEmailEnabled = null
        poi.isPushEnabled = null
        poi.isTextEnabled = null
        poi.isVerboseMsg = null
        poi.siteID = null
        poi.verified = null

        userPath?.child(name)?.updateChildren(poi.serializeToMap())?.addOnSuccessListener {
            callback(true, null)
        }?.addOnFailureListener {
            callback(false, it.log())
        }
    }

    fun deletePOI(location: SavedLocation?, callback: (Boolean, LoggedException?) -> Unit) {
        val userID = user?.uid ?: return callback(false, LoggedException("User is not logged in"))
        val name = location?.poiName ?: return callback(false, LoggedException("Location Name is required"))

        val path = database?.ref?.child(DatabaseTables().watchList)?.child(userID)?.child(name)

        path?.removeValue { databaseError, _ ->
            if (databaseError == null) {
                callback(true, null)
            } else {
                callback(false, databaseError.toException().log())
            }
        }
    }

    // Reports

    fun writeOutageReport(report: OutageReport, callback: (Boolean, LoggedException?) -> Unit) {
        val userID = user?.uid ?: return callback(false, LoggedException("No User ID found"))
        logEvent(AnalyticsEvents.OUTAGE_REPORTED)

        report.userID = userID
        database?.child(DatabaseTables().reportedOutage)?.child(userID)?.push()?.setValue(report.serializeToMap())?.addOnSuccessListener {
            callback(true, null)
        }?.addOnFailureListener { callback(false, it.log()) }
    }

    fun writeStreetLightReport(report: StreetlightReport, callback: (Boolean, Exception?) -> Unit) {
        val userID = user?.uid ?: return callback(false, Exception("No User ID found"))
        logEvent(AnalyticsEvents.STREETLIGHT_REPORTED)

        database?.child(DatabaseTables().reportedStreetLight)?.child(userID)?.push()?.setValue(report.serializeToMap())?.addOnSuccessListener {
            callback(true, null)
        }?.addOnFailureListener {
            callback(false, it)
        }
    }

    fun writeGeneralIssueReport(report: GeneralIssueReport, callback: (Boolean, Exception?) -> Unit) {
        val userID = user?.uid ?: return callback(false, Exception("No User ID found"))
        logEvent(AnalyticsEvents.GENERAL_ISSUE_REPORTED)

        database?.child(DatabaseTables().reportedGeneralIssue)?.child(userID)?.push()?.setValue(report.serializeToMap())?.addOnSuccessListener {
            callback(true, null)
        }?.addOnFailureListener {
            callback(false, it)
        }
    }


    /// Push Messaging

    fun setupPushMessaging() {
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        @Suppress("ThrowableNotThrown")
                        LoggedException(cause = task.exception
                                ?: Throwable("Cannot retrieve Push Token"))
                        return@addOnCompleteListener
                    }

                    val pushToken = task.result?.token ?: return@addOnCompleteListener
                    if (pushToken.isNotEmpty()) {
                        savePushToken(pushToken)
                    }
                }
    }

    fun savePushToken(token: String) {
        val userID = user?.uid ?: return
        val ref = database?.child(DatabaseTables().pushToken)?.child(userID) ?: return

        ref.child(token).setValue(true).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                @Suppress("ThrowableNotThrown")
                LoggedException(cause = task.exception ?: Throwable("Cannot save Push Token"))
                return@addOnCompleteListener
            }
            Log.d("Saved Push Token", token)
        }
    }


    // Analytics

    fun logEvent(eventName: String, attributes: Bundle? = null) {
        instance?.analytics?.logEvent(eventName, attributes)
    }

    fun setCurrentScreen(activity: Activity, name: String) {
        instance?.analytics?.setCurrentScreen(activity, name, name)
    }

//    fun logRequest(eventName: String, success: Boolean, message: String? = null, exception: Exception? = null) {
//        val eventBundle = Bundle()
//        eventBundle.putLong(Param.SUCCESS, if (success) 1 else 0)
//        message?.let { eventBundle.putString(AnalyticsAttributes.MESSAGE, it) }
//        exception?.let { eventBundle.putString(AnalyticsAttributes.EXCEPTION, it.localizedMessage ?: return@let) }
//
//        instance?.analytics?.logEvent(eventName, eventBundle)
//
//    }

}

object AnalyticsScreenNames {
    const val LOCATIONS_TAB = "LocationsTabFragment"
    const val REPORTS_TAB = "ReportsTabFragment"
    const val LEARN_TAB = "LearnTabFragment"
    const val SETTINGS_TAB = "SettingsTabFragment"
}

object AnalyticsEvents {
    // User Events
    const val LOGIN = "app_login"
    const val LOGOUT = "app_logout"
    const val UNLINK = "app_unlink_account"
    const val NEW_REGISTRATION = "app_newUserRegistration_completed"

    // Location Events
    const val SITE_ADDED = "app_locationWithSiteID_added"
    const val POI_ADDED = "app_location_added"
    const val SITE_DELETED = "app_locationWithSiteID_deleted"
    const val POI_DELETED = "app_location_deleted"

    // App Tour Events
    const val TOUR_FULLY_VIEWED_FIRST_TIME = "app_firstTime_appTour_fullyViewed"
    const val TOUR_EARLY_EXIT_FIRST_TIME = "app_firstTime_appTour_earlyExit"
    const val TOUR_FULLY_VIEWED_SETTINGS = "app_settings_appTour_fullyViewed"
    const val TOUR_EARLY_EXIT_SETTINGS = "app_settings_appTour_earlyExit"

    // Report Events
    const val STREETLIGHT_REPORTED = "app_new_streetlight_reported"
    const val OUTAGE_REPORTED = "app_new_outage_reported"
    const val GENERAL_ISSUE_REPORTED = "app_new_generalIssue_reported"

    // Learn Events
    const val LEARN_HOME_VISITED = "app_help_home"
    const val LEARN_MY_BILL_VISITED = "app_help_myBill_viewed"
    const val LEARN_KIP_VISITED = "app_help_kip_viewed"
}

object AnalyticsAttributes {
    const val MESSAGE = "message"
    const val EXCEPTION = "exception"
}