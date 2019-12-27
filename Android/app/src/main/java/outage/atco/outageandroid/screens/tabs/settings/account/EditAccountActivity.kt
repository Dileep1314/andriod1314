package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_edit_account.*
import kotlinx.android.synthetic.main.app_bar_general.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.utility.*

class EditAccountActivity : ConnectivityActivity(R.layout.activity_edit_account) {

    private val requiredInputs by lazy {
        mutableMapOf(
                editFirstNameInput to false, editLastNameInput to false
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generalTitle.text = getString(R.string.settings_edit_account_title)

        if (AppDataManager.instance?.userProfile == null) fetchUserDetails() else setFields()

        editPhoneInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        requiredInputs.onEach { (input, _) ->
            input.afterTextChanged {
                validateFields(false)
            }
        }

        requiredInputs.forEach { (input, _) ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    requiredInputs[input] = true
                } else {
                    validateFields(false)
                }
            }
        }

        editAccountSaveButton.setOnClickListener {
            submit()
        }
    }

    private fun fetchUserDetails() {
        editAccountSaveButton.showLoading()
        FirebaseManager.instance?.getUserDetails { userProfile, loggedException ->
            editAccountSaveButton.hideLoading(R.string.settings_edit_account_save_button)
            loggedException?.let {
                return@getUserDetails showErrorMessage(it.message ?: return@getUserDetails, this)
            }
            AppDataManager.instance?.userProfile = userProfile
            setFields()
        }
    }

    private fun setFields() {
        AppDataManager.instance?.userProfile?.let {
            editEmailInput.setText(it.email)
            editFirstNameInput.setText(it.firstName)
            editLastNameInput.setText(it.lastName)
            editPhoneInput.setText(it.phoneNumber)
        }
    }

    private fun submit() {
        hideKeyboard()
        if (!validateFields()) return

        editAccountSaveButton.showSubmitting()
        val profile = UserProfile(
                email = editEmailInput.text.toString(),
                firstName = editFirstNameInput.text.toString(),
                lastName = editLastNameInput.text.toString(),
                phoneNumber = editPhoneInput.text.toString()
        )
        FirebaseManager.instance?.saveUserDetails(profile) { success, exception ->
            editAccountSaveButton.hideLoading(R.string.settings_edit_account_save_button)
            exception?.let { return@saveUserDetails showErrorMessage(it.message ?: return@saveUserDetails, this) }

            if (success) {
                finish()
                Toast.makeText(this, getString(R.string.settings_edit_account_success), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateFields(showDialogs: Boolean = true): Boolean {
        var validationPasses = true
        // Validate Required Fields
        requiredInputs.forEach { (field, _) ->
            if (requiredInputs[field] != true) return@forEach
            field.validateRequired()
        }
        if (requiredInputs.any { (it.key.parent.parent as? TextInputLayout)?.error != null }) {
            validationPasses = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_required_fields), this)
            }
        }

        return validationPasses
    }
}

