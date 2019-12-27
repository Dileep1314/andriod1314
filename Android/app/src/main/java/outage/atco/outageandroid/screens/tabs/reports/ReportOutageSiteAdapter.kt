package outage.atco.outageandroid.screens.tabs.reports

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_report_outage_location.view.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.VerificationStatusStrings
import outage.atco.outageandroid.utility.inflate

class ReportOutageSiteAdapter(private val siteList: MutableList<SavedLocation>, val fragment: ReportOutageSiteFragment): RecyclerView.Adapter<ReportOutageSiteAdapter.SiteCardHolder>() {
    override fun onBindViewHolder(holder: SiteCardHolder, position: Int) {
        holder.bind(siteList[position])
    }

    override fun getItemCount(): Int {
        return siteList.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteCardHolder {
        return SiteCardHolder(parent.inflate(R.layout.item_report_outage_location))
    }

    inner class SiteCardHolder(private val cell: View): RecyclerView.ViewHolder(cell) {

        fun bind(site: SavedLocation) {
            cell.reportOutageItemSiteName?.text = site.siteName ?: site.poiName ?: fragment.getString(R.string.locations_unnamed)
            cell.reportOutageItemSiteID?.text = site.siteID ?: "--"
            cell.reportOutageItemStatus?.text = if (site.siteID != null) VerificationStatusStrings[site.verified ?: 3] else fragment.getString(R.string.locations_no_site_id)

            cell.setOnClickListener {
                fragment.goToOutageDetails(site)
            }

            val color = ContextCompat.getColor(fragment.context ?: return, if (site.verified == 1) R.color.compGreen else R.color.errorRed)
            cell.reportOutageItemStatus.setTextColor(color)

        }
    }
}