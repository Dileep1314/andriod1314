package outage.atco.outageandroid.screens.tabs.learn

import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_key_industry_item.*
import outage.atco.outageandroid.R

class KeyIndustryItemFragment(val item: KeyIndustryActivity.KeyIndustryItem = KeyIndustryActivity.KeyIndustryItem()): Fragment(R.layout.fragment_key_industry_item) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postponeEnterTransition()
        activity?.window?.setFlags(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // bind content
        keyIndustryItemImage.setImageBitmap(item.bitmap)
        keyIndustryItemTitle.text = item.title
        keyIndustryItemSubtitle.text = item.subtitle
        keyIndustryItemContent.text = item.content


        view.post {
            startPostponedEnterTransition()
        }
    }

    override fun onStop() {
        activity?.window?.clearFlags(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        super.onStop()
    }
}