package outage.atco.outageandroid.screens.tabs.locations

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.map_card_cell_site.view.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.LocationOutageType
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.VerificationStatusStrings
import outage.atco.outageandroid.screens.tabs.locations.sites.AddNewSiteActivity
import outage.atco.outageandroid.screens.tabs.locations.sites.EditSavedLocationActivity
import outage.atco.outageandroid.utility.OutageManager
import outage.atco.outageandroid.utility.inflate

class MapCardAdapter(private val siteList: MutableList<SavedLocation>, val fragment: MapFragment): RecyclerView.Adapter<MapCardAdapter.SiteCardHolder>() {
    override fun onBindViewHolder(holder: SiteCardHolder, position: Int) {
        holder.bind(siteList[position])
    }

    override fun getItemCount(): Int {
        return siteList.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteCardHolder {
        val resID = if (viewType == 0) R.layout.map_card_cell_site else R.layout.map_card_no_saved
        return SiteCardHolder(parent.inflate(resID), viewType)
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= siteList.lastIndex) return 1
        return super.getItemViewType(position)
    }

    inner class SiteCardHolder(private val cell: View, private val viewType: Int): RecyclerView.ViewHolder(cell) {

        fun bind(site: SavedLocation) {
            when (viewType) {
                0 -> {
                    cell.siteIDText?.text = if (site.siteID.isNullOrEmpty()) "--" else site.siteID
                    cell.verifiedText?.visibility = if (site.siteID.isNullOrEmpty()) View.GONE else View.VISIBLE
                    cell.verifiedText?.text = VerificationStatusStrings[site.verified ?: 0]
                    cell.verifiedText?.setTextColor(ContextCompat.getColor(cell.context, if (site.verified == 1) R.color.compGreen else R.color.compRed))

                    cell.siteNameText?.text = site.siteName ?: site.poiName ?: ""
                    cell.addressText?.text = if (site.locality.isNullOrEmpty()) "--" else site.locality
                    cell.siteCardHeader?.setBackgroundColor(ContextCompat.getColor(cell.context, site.outageType?.second ?: R.color.primaryBlue))
                    cell.siteMapPinButton?.setImageResource(site.outageType?.first ?: R.drawable.ic_map_saved_location)

                    val isImpacted = site.outageType != LocationOutageType().noOutage
                    val latLng = LatLng(site.latitude ?: -1.0, site.longitude ?: -1.0)
                    val hasCoords = latLng != LatLng(-1.0,-1.0)

                    cell.siteMapPinButton?.visibility = if (isImpacted || hasCoords) View.VISIBLE else View.GONE

                    cell.editSiteButton?.setOnClickListener {
                        val intent = Intent(fragment.context, EditSavedLocationActivity::class.java)
                        intent.putExtra("location", site)
                        fragment.startActivity(intent, null)
                    }
                    cell.siteMapPinButton?.setOnClickListener {
                        val outage = OutageManager.instance.activeOutageList.find { it.outageID == site.impactingOutageID }
                                ?: OutageManager.instance.plannedOutageList.find { it.outageID == site.impactingOutageID }

                        outage?.let { return@setOnClickListener fragment.showOutageInfo(it) }

                        if (latLng == LatLng(-1.0,-1.0)) return@setOnClickListener
                        fragment.viewModel.zoomToLocation(latLng, callback = null)
                    }
                }
                else -> {
                    cell.setOnClickListener {
                        fragment.startActivity(Intent(fragment.context, AddNewSiteActivity::class.java), null)
                    }
                }
            }
        }
    }
}



