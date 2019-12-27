package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_email_expired.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmailExpiredFragment(val email: String? = null, val flow: AuthFlowType = AuthFlowType.None):
        Fragment(R.layout.fragment_email_expired) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_email_expired)

        bindProgressButton(emailExpiredResendButton)
        emailExpiredResendButton.attachTextChangeAnimator()

        emailExpiredResendButton.setOnClickListener {

            emailExpiredResendButton.showSubmitting()

            val useCase = if (flow == AuthFlowType.Reset) "resetPassword" else "newUser"
            val email = email ?: return@setOnClickListener showErrorMessage(getString(R.string.settings_error_email_query), activity)

            FirebaseManager.instance?.sendVerificationEmail(email, useCase, object: Callback<Any> {
                override fun onFailure(call: Call<Any>, t: Throwable) {
                    @Suppress("ThrowableNotThrown")
                    LoggedException(cause = t)
                    showErrorMessage(getString(R.string.settings_error_email_query), activity)
                    emailExpiredResendButton.hideLoading(R.string.settings_resend_email)
                }

                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.code() in 200..299) {
                        AppDataManager.instance?.pendingVerificationEmail = email
                        return if (flow == AuthFlowType.Reset) {
                            (activity as? AuthActivity)?.openFragment(EmailResetFragment(email)) ?: Unit
                        } else {
                            (activity as? AuthActivity)?.openFragment(EmailVerificationFragment(email)) ?: Unit
                        }
                    }

                    showErrorMessage(getString(R.string.settings_error_email_query), activity)
                    emailExpiredResendButton.hideLoading(R.string.settings_resend_email)
                }

            })

        }
    }
}