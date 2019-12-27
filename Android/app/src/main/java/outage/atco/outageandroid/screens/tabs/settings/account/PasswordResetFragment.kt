package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_password_reset.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class PasswordResetFragment(val email: String? = null, val code: String? = null):
        Fragment(R.layout.fragment_password_reset) {

    private val validatedInputs by lazy {
        mutableMapOf(
                resetPassCodeInput to false,
                resetNewPasswordInput to false,
                resetConfirmPasswordInput to false
        )
    }

    private val requiredInputs by lazy {
        listOf(resetPassCodeInput)
    }

    private val matchingInputs by lazy {
        mapOf(resetNewPasswordInput to resetConfirmPasswordInput)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_new_password_button)

        AppDataManager.instance?.pendingVerificationEmail = null

        code?.let {
            resetPassCodeInput.setText(it)
            resetPassCodeInput.isEnabled = false
            resetPassCodeLayout.setBoxBackgroundColorResource(R.color.backgroundGrey)
            resetPassCodeLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        resetConfirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        validatedInputs.onEach { (input, _) ->
            input.afterTextChanged {
                validateFields(false)
            }
        }

        validatedInputs.forEach { (input, _) ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    validatedInputs[input] = true
                } else {
                    validateFields(false)
                }
            }
        }

        resetPasswordButton.setOnClickListener {
            submit()
        }

        bindProgressButton(resetPasswordButton)
        resetPasswordButton.attachTextChangeAnimator()
    }

    private fun submit() {
        hideKeyboard()
        if (!validateFields()) return

        val email = email ?: return showErrorMessage(getString(R.string.settings_error_email_query), activity)

        resetPasswordButton.showSubmitting()

        FirebaseManager.instance?.sendHandlePasswordReset(
                email = email,
                password = resetNewPasswordInput.text.toString(),
                firebaseKey = resetPassCodeInput.text.toString().toUpperCase(Locale.ROOT),
                callback = resetCallback
        )
    }

    private fun validateFields(showDialogs: Boolean = true): Boolean {
        var passesValidation = true
        // Validate Required Fields
        requiredInputs.forEach {
            if (validatedInputs[it] != true) return@forEach
            it.validateRequired()
        }
        if (requiredInputs.any { (it.parent.parent as? TextInputLayout)?.error != null }) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_required_fields), activity)
                return passesValidation
            }
        }

        // Validate Password
        val isPasswordValid = validatedInputs[resetNewPasswordInput] == true && resetNewPasswordInput.validatePassword()
        if (!isPasswordValid) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_password_prompt), activity)
                return passesValidation
            }
        }

        // Validate Matching Password and Security Answer Fields
        matchingInputs.forEach { (field, confirm) ->
            if (validatedInputs[field] != true) return@forEach
            confirm.validateMatching(field, getString(R.string.settings_error_match_password))
        }

        if (matchingInputs.any { (_, field) -> (field.parent.parent as? TextInputLayout)?.error != null }) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_match, getString(R.string.settings_error_match_password)), activity)
                return passesValidation
            }
        }

        return passesValidation
    }

    private var resetCallback = object: Callback<Any> {
        override fun onFailure(call: Call<Any>, t: Throwable) {
            @Suppress("ThrowableNotThrown")
            LoggedException(cause = t)
            showErrorMessage(getString(R.string.settings_error_signup_fields), activity)
            resetPasswordButton.hideLoading(R.string.settings_new_password_button)
        }

        override fun onResponse(call: Call<Any>, response: Response<Any>) {
            if (response.code() in 200..299) return handleResetResponse(response.body().serializeToMap())
            if (response.code() == 404) {
                return (activity as? AuthActivity)?.openFragment(EmailExpiredFragment(email ?: return, AuthFlowType.Reset)) ?: Unit
            }
            showErrorMessage(getString(R.string.network_error), activity)
            resetPasswordButton.hideLoading(R.string.settings_new_password_button)
        }
    }

    private fun handleResetResponse(response: Map<String, Any>) {
        val token = response["message"] as? String ?: return showErrorMessage(getString(R.string.settings_error_invalid_token), activity)
        FirebaseManager.instance?.customTokenLogin(token) {
            activity?.finish()
        }
    }
}