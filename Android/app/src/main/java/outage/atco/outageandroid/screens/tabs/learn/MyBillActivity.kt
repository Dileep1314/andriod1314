package outage.atco.outageandroid.screens.tabs.learn

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.android.synthetic.main.activity_my_bill.*
import kotlinx.android.synthetic.main.app_bar_general.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.screens.ConnectivityActivity
import outage.atco.outageandroid.utility.AnalyticsEvents
import outage.atco.outageandroid.utility.FirebaseManager
import outage.atco.outageandroid.utility.inflate
import java.io.BufferedReader

class MyBillActivity: ConnectivityActivity(R.layout.activity_my_bill) {

    class BillContent(
            @SerializedName("Tips")
            val sections: Map<String, Map<String, String>>? = null
    )

    private val resource: BufferedReader by lazy { resources.openRawResource(R.raw.mock_bill_content).bufferedReader() }
    private val content: BillContent? by lazy { Gson().fromJson(resource, BillContent::class.java) }
    private val items: Map<String, String> by lazy {
        content.run {
            val temp = mutableMapOf<String, String>()
            this?.sections?.forEach { temp.putAll(it.value) }
            temp
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generalTitle.text = getString(R.string.learn_my_bill)
        FirebaseManager.instance?.logEvent(AnalyticsEvents.LEARN_MY_BILL_VISITED)
    }

    fun onClick(view: View) {
        val toolTipContent = items[view.tag] ?: return
        val tooltipView = billContentsParent.inflate(R.layout.item_bill_tooltip)

        // TODO: Simple Tooltip works for now but is not the best. Find or build replacement
        SimpleTooltip.Builder(this)
                .anchorView(view)
                .text(toolTipContent)
                .contentView(tooltipView, R.id.tooltipTextView)
                .animated(false)
                .dismissOnInsideTouch(false)
                .arrowColor(getColor(R.color.white))
                .backgroundColor(getColor(R.color.white))
                .textColor(getColor(R.color.primaryBlack))
                .gravity(Gravity.TOP)
                .setWidth(billContentsParent.width)
                .build()
                .show()
    }

}


