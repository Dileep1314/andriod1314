package outage.atco.outageandroid.screens.tabs.learn

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_learn.*
import kotlinx.android.synthetic.main.item_card_learn_tab.view.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.*
import java.util.*

class LearnFragment: Fragment(R.layout.fragment_learn) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainTitle.text = getString(R.string.learn_tab_title)

        learnHomeRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        learnHomeRecycler.adapter = LearnItemsAdapter()
    }

    override fun onResume() {
        super.onResume()

        FirebaseManager.instance?.setCurrentScreen(activity ?: return, AnalyticsScreenNames.LEARN_TAB)
    }

    inner class LearnItemsAdapter: RecyclerView.Adapter<LearnItemViewHolder>() {
        val content: LearnHomeContent by lazy {
            val temp = Gson().fromJson(resources.openRawResource(R.raw.learn_home_content).bufferedReader(), LearnHomeContent::class.java)
            temp.items?.forEach {
                val drawableID = context?.resources?.getIdentifier(it.icon?.toLowerCase(Locale.ROOT), "drawable", context?.packageName) ?: R.drawable.atco_logo
                it.bitmap = decodeSampledBitmapFromResource(resources, drawableID, 300, 300)
            }

            temp
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LearnItemViewHolder {
            return LearnItemViewHolder(parent.inflate(R.layout.item_card_learn_tab))
        }

        override fun getItemCount(): Int {
            return content.items?.size ?: 0
        }

        override fun onBindViewHolder(holder: LearnItemViewHolder, position: Int) {
            holder.bind(content.items?.get(position) ?: return)
        }

    }

    inner class LearnItemViewHolder(private val cell: View): RecyclerView.ViewHolder(cell) {
        fun bind(item: LearnHomeItem) {
            cell.learnItemTitle?.text = item.title
            cell.learnItemDescription?.text = item.description
            cell.learnItemImage.setImageBitmap(item.bitmap)
            cell.setOnClickListener {
                when (adapterPosition) {
                    0 -> startActivity(Intent(context, MyBillActivity::class.java))
                    1 -> startActivity(Intent(context, KeyIndustryActivity::class.java))
                }
            }
        }
    }

    class LearnHomeContent(
            @SerializedName("HomeListItems")
            val items: List<LearnHomeItem>? = null
    )

    class LearnHomeItem(
            @SerializedName("Title")
            val title: String? = "",
            @SerializedName("Description")
            val description: String? = "",
            @SerializedName("Icon")
            val icon: String? = "",
            var bitmap: Bitmap? = null
    )
}