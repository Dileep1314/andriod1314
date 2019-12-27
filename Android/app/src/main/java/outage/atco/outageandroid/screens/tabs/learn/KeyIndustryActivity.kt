package outage.atco.outageandroid.screens.tabs.learn

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.utility.AnalyticsEvents
import outage.atco.outageandroid.utility.FirebaseManager
import outage.atco.outageandroid.utility.decodeSampledBitmapFromResource
import java.util.*
import kotlin.coroutines.CoroutineContext

class KeyIndustryActivity: ConnectivityActivity(R.layout.activity_empty_container) {

    private lateinit var job: Job
    private lateinit var keyIndustryContent: KeyIndustryContent
    private var positionOfTheSelectedContent: Int = 0
    private val dispatcher = Dispatchers.IO
    private val keyIndustryMainFragment = KeyIndustryMainFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        job = asyncLoadKeyIndustryContent(dispatcher, this)

        // Don't add fragment to back stack, as its the base view
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, keyIndustryMainFragment)
                .commit()

        FirebaseManager.instance?.logEvent(AnalyticsEvents.LEARN_KIP_VISITED)
    }

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }

    fun openFragment(newFragment: KeyIndustryItemFragment, position: Int) {
        positionOfTheSelectedContent = position

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .setTransition(TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(newFragment.tag)
                .commit()
    }

    fun requestContent(): Pair<KeyIndustryContent?, Int> {
        return if (::keyIndustryContent.isInitialized) Pair(keyIndustryContent, positionOfTheSelectedContent) else Pair(null, 0)
    }

    // Key Industry Content has several images that must be loaded from resources, so prepare here in background to ensure its ready to display
    private fun asyncLoadKeyIndustryContent(dispatcher: CoroutineContext, context: Context) = CoroutineScope(dispatcher).launch {
        val resource = context.resources.openRawResource(R.raw.key_industry_content).bufferedReader()
        val content: KeyIndustryContent = {
            val temp = Gson().fromJson(resource, KeyIndustryContent::class.java)
            temp.items?.forEach {
                val drawableID = context.resources.getIdentifier(it.image?.toLowerCase(Locale.ROOT), "drawable", context.packageName)
                it.bitmap = decodeSampledBitmapFromResource(context.resources, drawableID, 300, 300)
            }

            temp
        }()

        keyIndustryContent = content
        CoroutineScope(Dispatchers.Main).launch {
            keyIndustryMainFragment.displayContent(content, 0)
        }
    }

    class KeyIndustryContent(
            @SerializedName("KeyIndustryPlayerCollections")
            val items: List<KeyIndustryItem>? = null
    )

    class KeyIndustryItem(
            @SerializedName("BodyTitle")
            val title: String? = "",
            @SerializedName("BodySubTitle")
            val subtitle: String? = "",
            @SerializedName("Header")
            val header: String? = "",
            @SerializedName("Image")
            val image: String? = "",
            @SerializedName("Content")
            val content: String? = "",
            var bitmap: Bitmap? = null
    )
}