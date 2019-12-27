package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_signup.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class SignupFragment(val email: String? = null, val code: String? = null):
        Fragment(R.layout.fragment_signup) {

    // Map of input to whether it has been touched
    private val validatedInputs by lazy {
        mutableMapOf(
                signupPassCodeInput to false, signupNewPasswordInput to false,
                signupConfirmPasswordInput to false, signupFirstNameInput to false,
                signupLastNameInput to false, signupSecurityQuestion to false,
                signupSecurityAnswerInput to false, signupSecurityConfirmInput to false
        )
    }

    private val requiredInputs by lazy {
        listOf(
                signupPassCodeInput, signupFirstNameInput, signupLastNameInput
        )
    }

    private val matchingInputs by lazy {
        mapOf(
                signupNewPasswordInput to signupConfirmPasswordInput,
                signupSecurityAnswerInput to signupSecurityConfirmInput
        )
    }

    private val requiredLengthInputs by lazy {
        mapOf(
                signupSecurityAnswerInput to 4,
                signupSecurityConfirmInput to 4
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_register_button)

        signupEmailInput.setText(email ?: return showErrorMessage(getString(R.string.settings_error_email_query), activity))

        code?.let {
            signupPassCodeInput.setText(it)
            signupPassCodeLayout.setBoxBackgroundColorResource(R.color.backgroundGrey)
            signupPassCodeLayout.endIconMode = END_ICON_NONE
        }

        signupSecurityConfirmInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        validatedInputs.forEach { (input, _) ->
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

        bindProgressButton(submitButton)
        submitButton.attachTextChangeAnimator()
        submitButton.setOnClickListener {
            submit()
        }

        // TODO: Find a way to validate PassCode before submitting

        val adapterList = resources.getStringArray(R.array.settings_signup_security_questions)
        val adapter = ArrayAdapter<String>(context ?: return, R.layout.dropdown_menu_item_2line, R.id.item_text, adapterList)

        signupSecurityQuestion.setAdapter(adapter)
    }

    private fun submit() {
        hideKeyboard()

        validatedInputs.forEach { validatedInputs[it.key] = true }
        if (!validateFields()) return

        submitButton.showSubmitting()

        FirebaseManager.instance?.createUserVerifiedAccount(
                securityKey = signupPassCodeInput.text.toString().toUpperCase(Locale.ROOT),
                email = signupEmailInput.text.toString(),
                firstName = signupFirstNameInput.text.toString(),
                lastName = signupLastNameInput.text.toString(),
                phoneNum = signupPhoneInput.text.toString(),
                password = signupNewPasswordInput.text.toString(),
                recoveryQuestion = signupSecurityQuestion.text.toString(),
                recoveryAnswer = signupSecurityAnswerInput.text.toString(),
                callback = signupCallback
        )
    }

    private fun validateFields(showDialogs: Boolean = true): Boolean {
        // Kotlin lazy evaluates on 'any', so must iterate using for each first to ensure all fields checked
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

        requiredLengthInputs.forEach { (field, length) ->
            if (validatedInputs[field] != true) return@forEach
            field.validateLengthRequired(length)
        }

        if (requiredLengthInputs.any { (field, _) -> (field.parent.parent as? TextInputLayout)?.error != null }) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_required_fields), activity)
                return passesValidation
            }
        }

        // Validate Password
        val isPasswordValid = validatedInputs[signupNewPasswordInput] == true && signupNewPasswordInput.validatePassword()
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
            val fieldTypeString = if (field.id == R.id.signupNewPasswordInput) R.string.settings_error_match_password else R.string.settings_error_match_security
            confirm.validateMatching(field, getString(fieldTypeString))
        }

        if (matchingInputs.any { (_, field) -> (field.parent.parent as? TextInputLayout)?.error != null }) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_fields_mismatch), activity)
                return passesValidation
            }
        }

        return passesValidation
    }

    private var signupCallback = object: Callback<Any> {
        override fun onFailure(call: Call<Any>, t: Throwable) {
            @Suppress("ThrowableNotThrown")
            LoggedException(cause = t)
            showErrorMessage(getString(R.string.settings_error_signup_fields), activity)
            submitButton.hideLoading(R.string.settings_register_button)
        }

        override fun onResponse(call: Call<Any>, response: Response<Any>) {
            if (response.code() in 200..299) return handleSignupResponse(response.body().serializeToMap())
            if (response.code() == 404) {
                return (activity as? AuthActivity)?.openFragment(EmailExpiredFragment(email ?: return, AuthFlowType.Register)) ?: Unit
            }
            showErrorMessage(getString(R.string.network_error), activity)
            submitButton.hideLoading(R.string.settings_register_button)
        }
    }

    private fun handleSignupResponse(response: Map<String, Any>) {
        FirebaseManager.instance?.logEvent(AnalyticsEvents.NEW_REGISTRATION)

        AppDataManager.instance?.pendingVerificationEmail = null
        val token = response["message"] as? String
                ?: return showErrorMessage(getString(R.string.settings_error_invalid_token), activity)
        FirebaseManager.instance?.customTokenLogin(token) {
            activity?.finish()
        }
    }
}