package outage.atco.outageandroid.screens.tabs.settings.account

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import kotlinx.android.synthetic.main.app_bar_general.*
import kotlinx.android.synthetic.main.fragment_terms_of_service.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TermsOfServiceFragment(val flowType: AuthFlowType = AuthFlowType.None, val email: String? = null):
        Fragment(R.layout.fragment_terms_of_service), OnLoadCompleteListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generalTitle.text = getString(R.string.settings_terms_of_service)

        nextButton.visibility = if (flowType == AuthFlowType.None) View.GONE else View.VISIBLE
        agreeCheckbox.visibility = if (flowType == AuthFlowType.None) View.GONE else View.VISIBLE

        bindProgressButton(nextButton)
        nextButton.attachTextChangeAnimator()

        nextButton.setOnClickListener {
            if (!agreeCheckbox.isChecked) return@setOnClickListener showErrorMessage(getString(R.string.settings_terms_agree_required), activity)

            val email = email ?: return@setOnClickListener showErrorMessage(getString(R.string.settings_error_email), activity)

            AppDataManager.instance?.hasUserAgreedToTerms = true

            when (flowType) {
                AuthFlowType.Login -> {
                    (activity as? AuthActivity)?.openFragment(LoginFragment(email))
                }
                AuthFlowType.Register -> {
                    nextButton.showSubmitting()
                    FirebaseManager.instance?.sendVerificationEmail(email, "newUser", verificationEmailCallback)
                }
                else -> { /* Shouldn't ever get here */ }
            }
        }

        pdfView.fromAsset("ATCO-Terms.pdf")
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle( DefaultScrollHandle(context))
                .spacing(0)
                .pageSnap(false)
                .autoSpacing(false)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load()
    }

    override  fun loadComplete(nbPages: Int) {
        pdfView.zoomTo(1.0f)
    }

    private var verificationEmailCallback = object : Callback<Any> {
        override fun onFailure(call: Call<Any>, t: Throwable) {
            @Suppress("ThrowableNotThrown")
            LoggedException(cause = t)
            showErrorMessage(getString(R.string.settings_error_email_query), activity)
            nextButton.hideLoading(R.string.next)
        }

        override fun onResponse(call: Call<Any>, response: Response<Any>) {
            val email = email ?: return showErrorMessage(getString(R.string.settings_error_email_query), activity)
            if (response.code() in 200..299) {
                AppDataManager.instance?.pendingVerificationEmail = email
                return (activity as? AuthActivity)?.openFragment(EmailVerificationFragment(email)) ?: Unit
            }

            showErrorMessage(getString(R.string.settings_error_email_query), activity)
            nextButton.hideLoading(R.string.next)
        }
    }

}
