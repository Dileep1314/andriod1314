package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_start_auth.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StartAuthFragment: Fragment(R.layout.fragment_start_auth) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.register_login)

        bindProgressButton(nextButton)
        nextButton.attachTextChangeAnimator()
        nextButton.setOnClickListener {
            submit()
        }

        startAuthEmailInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE or EditorInfo.IME_ACTION_GO or EditorInfo.IME_ACTION_SEND) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        startAuthEmailInput.requestFocus()
    }

    private fun submit() {
        hideKeyboard()
        if (!validateEmail()) return showErrorMessage(getString(R.string.settings_error_email), activity)

        nextButton.isEnabled = false
        nextButton.showSubmitting()

        FirebaseManager.instance?.checkIfUserExist(startAuthEmailInput.text.toString(), object: Callback<Any> {
            override fun onFailure(call: Call<Any>, t: Throwable) {
                @Suppress("ThrowableNotThrown")
                LoggedException(cause = t)
                showErrorMessage(getString(R.string.settings_error_email_query), activity)
                nextButton.hideLoading(R.string.next)
            }

            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.code() == 200) {
                    if (startAuthEmailInput?.text.toString() == AppDataManager.instance?.pendingVerificationEmail) {
                        return (activity as? AuthActivity)?.openFragment(EmailResetFragment(startAuthEmailInput?.text.toString())) ?: Unit
                    }
                    checkTermsAgreement(AuthFlowType.Login)
                } else {
                    if (startAuthEmailInput?.text.toString() == AppDataManager.instance?.pendingVerificationEmail) {
                        return (activity as? AuthActivity)?.openFragment(EmailVerificationFragment(startAuthEmailInput?.text.toString())) ?: Unit
                    }

                    AlertDialog.Builder(context ?: return, R.style.AppTheme_Dialog)
                            .setTitle(R.string.settings_email_verification_title)
                            .setMessage(R.string.settings_new_email_prompt)
                            .setPositiveButton(R.string.settings_register_button) { _, _ ->
                                AppDataManager.instance?.hasUserAgreedToTerms = false
                                checkTermsAgreement(AuthFlowType.Register)
                            }
                            .setNegativeButton(R.string.settings_change_email) { _, _ ->
                                nextButton.hideLoading(R.string.next)
                            }
                            .show()
                }
            }
        })
    }

    private fun validateEmail(): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(startAuthEmailInput.text.toString()).matches()
    }

    private fun checkTermsAgreement(destination: AuthFlowType) {
        if (AppDataManager.instance?.hasUserAgreedToTerms == true && destination == AuthFlowType.Login) {
            (activity as? AuthActivity)?.openFragment(LoginFragment(startAuthEmailInput?.text.toString()))
        } else {
            (activity as? AuthActivity)?.openFragment(TermsOfServiceFragment(destination, startAuthEmailInput?.text.toString()))
        }
    }
}