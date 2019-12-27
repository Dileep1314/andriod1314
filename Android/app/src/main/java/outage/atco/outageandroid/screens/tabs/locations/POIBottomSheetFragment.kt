package outage.atco.outageandroid.screens.tabs.locations

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_poi_bottom_sheet.*
import outage.atco.outageandroid.R
import outage.atco.outageandroid.models.SavedLocation
import outage.atco.outageandroid.models.reports.GeneralIssueReport
import outage.atco.outageandroid.screens.tabs.locations.sites.AddNewSiteActivity
import outage.atco.outageandroid.screens.tabs.reports.ReportIssueActivity
import outage.atco.outageandroid.screens.tabs.reports.ReportType
import outage.atco.outageandroid.screens.tabs.settings.account.AuthActivity
import outage.atco.outageandroid.utility.FirebaseManager
import outage.atco.outageandroid.utility.OutageManager
import outage.atco.outageandroid.utility.pxToDp
import outage.atco.outageandroid.utility.showErrorMessage

class POIBottomSheetFragment(private val poi: Place, val fragment: MapFragment): BottomSheetDialogFragment() {

    private var savedLocation: SavedLocation? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_poi_bottom_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            window?.setDimAmount(0f)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        fragment.onDismissBottomSheet(savedLocation)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        poiNameText.text = poi.name
        poiAddressText.text = poi.address

        closeButton.setOnClickListener {
            dismiss()
        }

        poiSaveButton.setOnClickListener {
            val intent = Intent(context, AddNewSiteActivity::class.java)
            intent.putExtra("place", poi)
            startActivity(intent)
            dismiss()
        }

        poiDeleteButton.setOnClickListener {
            AlertDialog.Builder(context ?: return@setOnClickListener, R.style.AppTheme_Dialog)
                    .setMessage(R.string.locations_delete_site_message)
                    .setTitle(R.string.locations_delete_site)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        deleteLocation(savedLocation)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        poiReportGeneral.setOnClickListener {
            val report = GeneralIssueReport(address = poi.address, latitude = poi.latLng?.latitude, longitude = poi.latLng?.longitude)
            val intent = Intent(context, ReportIssueActivity::class.java)
            intent.putExtra("type", ReportType.General.string)
            intent.putExtra("report", report)
            startActivity(intent)
        }

        poiReportStreetlight.setOnClickListener {
            val report = GeneralIssueReport(address = poi.address, latitude = poi.latLng?.latitude, longitude = poi.latLng?.longitude)
            val intent = Intent(context, ReportIssueActivity::class.java)
            intent.putExtra("type", ReportType.Streetlight.string)
            intent.putExtra("report", report)
            startActivity(intent)
        }

        poiLoginButton.setOnClickListener {
            startActivity(Intent(fragment.context ?: return@setOnClickListener, AuthActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        savedLocation = OutageManager.instance.userSavedLocations.firstOrNull { poi.latLng?.latitude == it.latitude && poi.latLng?.longitude == it.longitude }

        if (fragment.viewModel.isUserLoggedIn) {
            poiLoginButton.visibility = View.GONE
            poiLoginText.visibility = View.GONE
            poiLoginDivider.visibility = View.GONE
            reportIssuesView.visibility = View.VISIBLE
            poiDeleteButton.visibility = if (savedLocation != null) View.VISIBLE else View.GONE
            poiSaveButton.visibility = if (savedLocation != null) View.GONE else View.VISIBLE

        } else {
            poiLoginText.visibility = View.VISIBLE
            poiLoginButton.visibility = View.VISIBLE
            poiLoginDivider.visibility = View.VISIBLE
            reportIssuesView.visibility = View.GONE
            poiDeleteButton.visibility = View.GONE
            poiSaveButton.visibility = View.GONE
        }

        fragment.viewModel.refreshMapOutageLayers()
        fragment.recyclerContainer?.visibility = View.GONE

        poiBottomSheetParent?.post {
            val height = poiBottomSheetParent?.height?.pxToDp
            // Adjust for bottom navigation bar height of 56dp as bottom sheet is shifted down to cover it
            fragment.viewModel.setMapPadding(((height ?: 400) - 56).toFloat())
            if (poi.latLng == LatLng(-1.0,-1.0)) {
                fragment.viewModel.expandToProvince(callback = null)
            } else {
                poi.latLng?.let { fragment.viewModel.zoomToLocation(it, fragment.viewModel.standardZoomInLevel, callback = null) }
            }
            if (savedLocation == null) poi.latLng?.let { fragment.viewModel.didDropPinOnMap(it) }
        }


    }

    private fun deleteLocation(savedLocation: SavedLocation?) {
        FirebaseManager.instance?.deletePOI(savedLocation ?: return) { success, _ ->
            if (success) {
                Toast.makeText(context, getString(R.string.locations_delete_site_success), Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                showErrorMessage(getString(R.string.locations_error_delete_location), activity)
            }
        }
    }
}
