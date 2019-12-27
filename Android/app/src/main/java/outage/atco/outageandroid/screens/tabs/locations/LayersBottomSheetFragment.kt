package outage.atco.outageandroid.screens.tabs.locations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_layers_bottom_sheet.*
import outage.atco.outageandroid.R


class LayersBottomSheetFragment(private val layerStates: Map<String, Boolean>, val fragment: MapFragment): BottomSheetDialogFragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_layers_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeButton.setOnClickListener(this)
        activeOutagesVisibility.setOnClickListener(this)
        impactOutagesVisibility.setOnClickListener(this)
        plannedOutagesVisibility.setOnClickListener(this)
        scheduledOutagesVisibility.setOnClickListener(this)
        myLocationVisibility.setOnClickListener(this)

        layerStates.forEach { (layer, isSelected) ->
            view.findViewWithTag<ImageView>(layer)?.isSelected = isSelected
        }
    }

    override fun onClick(view: View?) {
        view?.apply { isSelected = !isSelected }

        when (view?.tag) {
            "close" -> dismiss()
            else -> fragment.setLayerVisibility(view?.tag.toString())
        }
    }

}