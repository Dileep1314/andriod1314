package outage.atco.outageandroid.screens.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import outage.atco.outageandroid.screens.tabs.learn.LearnFragment
import outage.atco.outageandroid.screens.tabs.locations.MapFragment
import outage.atco.outageandroid.screens.tabs.reports.ReportsFragment
import outage.atco.outageandroid.screens.tabs.settings.SettingsFragment

class MainTabsAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val tabFragments = listOf(
            MapFragment(), ReportsFragment(), LearnFragment(), SettingsFragment()
    )

    override fun getItem(position: Int): Fragment {
        return tabFragments[position]
    }

    override fun getCount(): Int {
        return tabFragments.count()
    }
}