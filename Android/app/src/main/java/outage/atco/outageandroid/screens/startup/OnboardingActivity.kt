package outage.atco.outageandroid.screens.startup

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.item_onboarding.view.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.MainActivity
import outage.atco.outageandroid.utility.*

class OnboardingActivity: AppCompatActivity(R.layout.activity_onboarding) {

    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    private val requiredImageWidth = 400
    private val requiredImageHeight = 800
    private var isFromSettings = false
    private var hasUserFullyViewedTour = false

    private val onboardingImages by lazy {
        listOf(
                decodeSampledBitmapFromResource(resources, R.drawable.onboarding0, requiredImageWidth, requiredImageHeight),
                decodeSampledBitmapFromResource(resources, R.drawable.onboarding1, requiredImageWidth, requiredImageHeight),
                decodeSampledBitmapFromResource(resources, R.drawable.onboarding2, requiredImageWidth, requiredImageHeight),
                decodeSampledBitmapFromResource(resources, R.drawable.onboarding3, requiredImageWidth, requiredImageHeight)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isFromSettings = intent.getBooleanExtra("fromSettings", false)

        onboardingRecycler?.layoutManager = layoutManager
        LinearSnapHelper().attachToRecyclerView(onboardingRecycler)

        onboardingRecycler.adapter = OnboardingAdapter()

        exitTourButton.setOnClickListener {
            handleExit()
        }

        AppDataManager.instance?.hasUserCompletedOnboarding = true
    }

    override fun onBackPressed() {
        handleExit()
    }

    private fun handleExit() {
        if (isFromSettings) {
            val event = if (hasUserFullyViewedTour) AnalyticsEvents.TOUR_FULLY_VIEWED_SETTINGS else AnalyticsEvents.TOUR_EARLY_EXIT_SETTINGS
            FirebaseManager.instance?.logEvent(event)
            finish()
        } else {
            val event = if (hasUserFullyViewedTour) AnalyticsEvents.TOUR_FULLY_VIEWED_FIRST_TIME else AnalyticsEvents.TOUR_EARLY_EXIT_FIRST_TIME
            FirebaseManager.instance?.logEvent(event)
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    inner class OnboardingAdapter : RecyclerView.Adapter<OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            return OnboardingViewHolder(parent.inflate(R.layout.item_onboarding))
        }

        override fun getItemCount(): Int {
            return onboardingImages.size
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            holder.bind(onboardingImages[position], position)
        }
    }

    inner class OnboardingViewHolder(private val item: View): RecyclerView.ViewHolder(item) {
        fun bind(image: Bitmap, index: Int) {
            val titleID = resources.getIdentifier("onboarding_item_title$index", "string", packageName)
            val contentID = resources.getIdentifier("onboarding_item_content$index", "string", packageName)

            item.onboardingImage?.setImageBitmap(image)
            item.onboardingImage?.contentDescription = getString(titleID)
            item.onboardingTitle?.text = getString(titleID)
            item.onboardingText?.text = getString(contentID)

            if (index == onboardingImages.size - 1) {
                Log.d("Onboarding", "Last Position reached")
                hasUserFullyViewedTour = true
            }
        }
    }
}


