package outage.atco.outageandroid.screens.tabs.settings.account

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_login.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment(val email: String? = null): Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_login_title)
        loginEmailInput.setText(email)

        loginForgotPassword.setOnClickListener {
            AlertDialog.Builder(context ?: return@setOnClickListener, R.style.AppTheme_Dialog)
                    .setTitle(R.string.settings_forgot_password)
                    .setMessage(R.string.settings_forgot_pass_message)
                    .setPositiveButton(R.string.settings_reset_password) { _, _ ->
                        sendPasswordResetEmail()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        bindProgressButton(submitButton)
        submitButton.attachTextChangeAnimator()
        submitButton.setOnClickListener {
            submit()
        }

        loginPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE or EditorInfo.IME_ACTION_GO or EditorInfo.IME_ACTION_SEND) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        loginPasswordInput.requestFocus()
    }

    private fun submit() {
        hideKeyboard()
        if (loginPasswordInput.text?.isBlank() != false)
            return showErrorMessage(getString(R.string.settings_error_password), activity)

        val email = email ?: return showErrorMessage(getString(R.string.settings_error_email_query), activity)

        submitButton.showSubmitting()

        FirebaseManager.instance?.loginUser(email, loginPasswordInput.text.toString(), object: Callback<Any> {
            override fun onFailure(call: Call<Any>, t: Throwable) {
                @Suppress("ThrowableNotThrown")
                LoggedException(cause = t)
                showErrorMessage(getString(R.string.settings_error_login_query), activity)
                submitButton.hideLoading(R.string.settings_login)
            }

            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.code() in 200..299) return handleLoginResponse(response.body().serializeToMap())
                showErrorMessage(getString(R.string.settings_error_login_query), activity)
                submitButton.hideLoading(R.string.settings_login)
            }
        })
    }

    private fun handleLoginResponse(response: Map<String, Any>) {
        val token = response["message"] as? String ?: return

        FirebaseManager.instance?.customTokenLogin(token) {
            if (activity?.intent?.extras?.getString("destination") == AuthFlowType.Unlink.type) {
                activity?.setResult(Activity.RESULT_OK)
            }

            FirebaseManager.instance?.logEvent(AnalyticsEvents.LOGIN)
            activity?.finish()
        }
    }

    private fun sendPasswordResetEmail() {
        FirebaseManager.instance?.sendVerificationEmail(email ?: return, "resetPassword", object: Callback<Any> {
            override fun onFailure(call: Call<Any>, t: Throwable) {
                @Suppress("ThrowableNotThrown")
                LoggedException(cause = t)
                showErrorMessage(getString(R.string.settings_error_email_query), activity)
            }

            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.code() in 200..299) {
                    AppDataManager.instance?.pendingVerificationEmail = email
                    return (activity as? AuthActivity)?.openFragment(EmailResetFragment(email)) ?: Unit
                }
                showErrorMessage(getString(R.string.settings_error_email_query), activity)
            }
        })
    }
}