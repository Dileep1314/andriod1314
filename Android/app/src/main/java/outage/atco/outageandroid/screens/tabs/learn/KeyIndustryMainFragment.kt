package outage.atco.outageandroid.screens.tabs.learn

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.yarolegovich.discretescrollview.transform.Pivot
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.app_bar_main.mainTitle
import kotlinx.android.synthetic.main.fragment_key_industry_main.contentLoadingProgress
import kotlinx.android.synthetic.main.fragment_key_industry_main.keyIndustryContentLoading
import kotlinx.android.synthetic.main.fragment_key_industry_main.keyIndustryRecycler
import kotlinx.android.synthetic.main.item_card_key_industry_item.view.keyIndustryItemImage
import kotlinx.android.synthetic.main.item_card_key_industry_item.view.keyIndustryItemSubtitle
import kotlinx.android.synthetic.main.item_card_key_industry_item.view.keyIndustryItemTitle
import outage.atco.outageandroid.R
import outage.atco.outageandroid.utility.inflate

class KeyIndustryMainFragment: Fragment(R.layout.fragment_key_industry_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainTitle.text = getString(R.string.learn_key_industry_players)

        keyIndustryRecycler.setItemTransformer(ScaleTransformer.Builder()
                .setMaxScale(1.05f)
                .setMinScale(0.8f)
                .setPivotX(Pivot.X.CENTER) // CENTER is a default one
                .setPivotY(Pivot.Y.CENTER) // CENTER is a default one
                .build())
    }

    override fun onResume() {
        super.onResume()

        val content: Pair<KeyIndustryActivity.KeyIndustryContent?, Int>? = (activity as? KeyIndustryActivity)?.requestContent()
        content?.let {
            val (keyIndustryContent, position) = content
            if (keyIndustryContent == null) displayIsLoading() else displayContent(keyIndustryContent, position)
        }
    }

    private fun displayIsLoading() {
        keyIndustryRecycler.visibility = View.GONE
        keyIndustryContentLoading.visibility = View.VISIBLE
        contentLoadingProgress.animate()
    }

    fun displayContent(content: KeyIndustryActivity.KeyIndustryContent, position: Int) {
        keyIndustryRecycler?.adapter = KeyIndustryItemsAdapter(content)
        keyIndustryRecycler?.adapter?.notifyDataSetChanged()
        keyIndustryContentLoading?.visibility = View.GONE
        keyIndustryRecycler?.visibility = View.VISIBLE

        if (position > 0) {
            keyIndustryRecycler.scrollToPosition(position)
        }
    }

    inner class KeyIndustryItemsAdapter(val content: KeyIndustryActivity.KeyIndustryContent) : RecyclerView.Adapter<KeyIndustryItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyIndustryItemViewHolder {
            val view = parent.inflate(R.layout.item_card_key_industry_item)

            view.layoutParams.width = (parent.width * 0.75).toInt()
            return KeyIndustryItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return content.items?.size ?: 0
        }

        override fun onBindViewHolder(holder: KeyIndustryItemViewHolder, position: Int) {
            holder.bind(content.items?.get(position) ?: return, position)
        }
    }

    inner class KeyIndustryItemViewHolder(private val cell: View): RecyclerView.ViewHolder(cell) {
        fun bind(item: KeyIndustryActivity.KeyIndustryItem, position: Int) {
            cell.tag = position
            cell.keyIndustryItemTitle?.text = item.title
            cell.keyIndustryItemSubtitle?.text = item.subtitle
            cell.keyIndustryItemImage?.setImageBitmap(item.bitmap)

            cell.setOnClickListener {
                val pos = cell.tag as Int
                (activity as? KeyIndustryActivity)?.openFragment(KeyIndustryItemFragment(item), pos)
            }
        }
    }
}