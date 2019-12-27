package outage.atco.outageandroid.utility

import android.content.Context
import android.content.SharedPreferences
import com.crashlytics.android.Crashlytics
import outage.atco.outageandroid.R

class AppDataManager private constructor() {
    var sharedPref: SharedPreferences? = null

    var hasUserCompletedOnboarding: Boolean
        get() {
            val agreeString = sharedPref?.getString("UserViewedOnboardingHelp", "") // Backwards Compat to old version of app
            val agreeBool = sharedPref?.getBoolean("userCompletedOnboarding", false)
            return (agreeBool == true || agreeString == "Yes")
        }
        set(value) { sharedPref?.edit()?.putBoolean("userCompletedOnboarding", value)?.apply() }

    var hasUserAgreedToTerms: Boolean
        get() {
            val agreeString = sharedPref?.getString("UserAgreesLegal", "") // Backwards Compat to old version of app
            val agreeBool = sharedPref?.getBoolean("userAgreesTerms", false)
            return (agreeBool == true || agreeString == "Yes")
        }
        set(value) { sharedPref?.edit()?.putBoolean("userAgreesTerms", value)?.apply() }

    var pendingVerificationEmail: String?
        get() = sharedPref?.getString("RegistrationUserEmail", null)
        set(value) { sharedPref?.edit()?.putString("RegistrationUserEmail", value)?.apply() }

    var userProfile: UserProfile? = null

    companion object {
        var instance: AppDataManager? = null

        fun setupInstance(appContext: Context) {

            instance = AppDataManager()
            instance?.sharedPref = appContext.getSharedPreferences(appContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

            PlacesQueryManager.setupInstance(appContext)
            FirebaseManager.setupInstance(appContext)

            if (FirebaseManager.instance?.user != null) {
                FirebaseManager.instance?.setupPushMessaging()
                Crashlytics.setUserIdentifier(FirebaseManager.instance?.user?.uid)
                FirebaseManager.instance?.getUserDetails { userProfile, loggedException ->
                    if (loggedException == null) instance?.userProfile = userProfile
                }
            }
        }
    }

    fun saveToken(token: String, expiresMill: Long){
        sharedPref?.edit()
            ?.putString("Token", token)
            ?.putLong("Expires", expiresMill)
            ?.apply()
    }

    fun loadToken(): AccessToken {
        return AccessToken(
            access_token = sharedPref?.getString("Token", ""),
            expires_millisecond = sharedPref?.getLong("Expires", 0)
        )
    }



}
