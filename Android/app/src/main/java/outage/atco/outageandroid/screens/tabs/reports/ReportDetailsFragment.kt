package outage.atco.outageandroid.screens.tabs.reports

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.github.razir.progressbutton.bindProgressButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_report_details.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.reports.BaseReport
import outage.atco.outageandroid.models.reports.GeneralIssueReport
import outage.atco.outageandroid.models.reports.OutageReport
import outage.atco.outageandroid.models.reports.StreetlightReport
import outage.atco.outageandroid.utility.*

class ReportDetailsFragment(val type: String = ReportType.General.string,
                            private val report: BaseReport = BaseReport()):
        Fragment(R.layout.fragment_report_details) {

    private val requiredInputs by lazy {
        mutableMapOf<EditText, Boolean>(
                reportOutageComments to false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title: String

        outageQuestionLayout.visibility = View.GONE
        streetlightQuestionLayout.visibility = View.GONE
        generalQuestionLayout.visibility = View.GONE

        when (type) {
            ReportType.Outage.string -> {
                title = getString(R.string.reports_power_outage)
                outageQuestionLayout.visibility = View.VISIBLE
            }
            ReportType.Streetlight.string -> {
                title = getString(R.string.reports_streetlight_title)
                streetlightQuestionLayout.visibility = View.VISIBLE
                generalQuestionLayout.visibility = View.VISIBLE
                val adapter = ArrayAdapter<String>(context ?: return, R.layout.dropdown_menu_item, resources.getStringArray(R.array.reports_problem_types_streetlight))
                reportProblemTypeSelect.setAdapter(adapter)
                requiredInputs.put(reportProblemTypeSelect, false)
            }
            else -> {
                title = getString(R.string.reports_general_issue)
                generalQuestionLayout.visibility = View.VISIBLE
                val adapter = ArrayAdapter<String>(context ?: return, R.layout.dropdown_menu_item, resources.getStringArray(R.array.reports_problem_types_general))
                reportProblemTypeSelect.setAdapter(adapter)
                requiredInputs.put(reportProblemTypeSelect, false)
            }
        }

        generalTitle.text = title

        bindProgressButton(reportOutageSubmitButton)
        reportOutageSubmitButton.setOnClickListener {
            submit()
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

        reportOutageComments.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE or EditorInfo.IME_ACTION_GO or EditorInfo.IME_ACTION_SEND) {
                submit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }

    private fun validateFields(showDialogs: Boolean = true): Boolean {
        // Kotlin lazy evaluates on 'any', so must iterate using forEach first to ensure all fields checked
        var passesValidation = true
        // Validate Required Fields
        requiredInputs.forEach { it.key.validateRequired() }
        if (requiredInputs.any { (it.key.parent.parent as? TextInputLayout)?.error != null }) {
            passesValidation = false
            if (showDialogs) {
                showErrorMessage(getString(R.string.settings_error_required_fields), activity)
                return passesValidation
            }
        }

        return passesValidation
    }

    private fun submit() {
        hideKeyboard()
        if (!validateFields()) return

        reportOutageSubmitButton.showSubmitting()
        when (type) {
            ReportType.Outage.string -> submitOutageReport()
            ReportType.General.string -> submitGeneralReport()
            ReportType.Streetlight.string -> submitStreetlightReport()
        }
    }

    private fun submitOutageReport() {
        val outageReport = report.serializeToMap().toDataClass<OutageReport>()

        outageReport.isPowerOut = reportOutagePowerOffYes.isChecked
        outageReport.comments = reportOutageComments.text.toString()

        FirebaseManager.instance?.writeOutageReport(outageReport) { success, _ ->
            if (!success) return@writeOutageReport handleFailure()

            goToSuccess()
        }
    }

    private fun submitGeneralReport() {
        val generalReport = report.serializeToMap().toDataClass<GeneralIssueReport>()

        generalReport.comments = reportOutageComments.text.toString()
        generalReport.problemType = reportProblemTypeSelect.text.toString()

        FirebaseManager.instance?.writeGeneralIssueReport(generalReport) { success, _ ->
            if (!success) return@writeGeneralIssueReport handleFailure()
            goToSuccess()
        }
    }

    private fun submitStreetlightReport() {
        val streetlightReport = report.serializeToMap().toDataClass<StreetlightReport>()

        streetlightReport.comments = reportOutageComments.text.toString()
        streetlightReport.assetID = streetlightAssetID.text.toStringOrNotProvided()
        streetlightReport.problemType = reportProblemTypeSelect.text.toString()

        FirebaseManager.instance?.writeStreetLightReport(streetlightReport) { success, _ ->
            if (!success) return@writeStreetLightReport handleFailure()
            goToSuccess()
        }
    }

    private fun goToSuccess() {
        reportOutageSubmitButton.hideLoading(R.string.reports_submit)

        val fragment = ReportSuccessFragment(type)
        fragmentManager?.beginTransaction()
            ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(fragment.tag)
            ?.commit()
    }

    private fun handleFailure() {
        showErrorMessage(getString(R.string.reports_error_submit), activity)
        reportOutageSubmitButton.hideLoading(R.string.reports_submit)
    }
}