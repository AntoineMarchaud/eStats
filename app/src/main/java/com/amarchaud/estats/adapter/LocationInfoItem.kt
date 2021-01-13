package com.amarchaud.estats.adapter

import android.graphics.drawable.Animatable
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemLocationBinding
import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.dialog.AddSubLocationDialog
import com.amarchaud.estats.utils.TimeTransformation
import com.amarchaud.estats.view.MainFragment
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem

/**
 * Example with ViewBinding only
 */
class LocationInfoItem(
    private var fragment: MainFragment,
    var locationInfo: LocationInfo,
    var displayExpanded: Boolean
) : // main location info
    BindableItem<ItemLocationBinding>(), ExpandableItem {

    // members
    private lateinit var expandableGroup: ExpandableGroup

    override fun getLayout(): Int = R.layout.item_location

    override fun initializeViewBinding(view: View): ItemLocationBinding =
        ItemLocationBinding.bind(view)

    override fun bind(viewBinding: ItemLocationBinding, position: Int) {

        viewBinding.apply {

            name.text = locationInfo.name
            lat.text = locationInfo.lat.toString()
            lon.text = locationInfo.lon.toString()
            duration.text = TimeTransformation.millisecondToTimeStr(locationInfo.duration_day)

            itemLayout.setOnLongClickListener {

                val fragmentManager = fragment.requireActivity().supportFragmentManager
                val customPopup = AddSubLocationDialog.newInstance(locationInfo.name!!, locationInfo.lat, locationInfo.lon, locationInfo.delta, locationInfo.id)
                customPopup.show(fragmentManager, "add new position")

                true
            }

            icExpand.visibility = if (displayExpanded) View.VISIBLE else View.INVISIBLE
            icExpand.setImageResource(if (expandableGroup.isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand)
            icExpand.setOnClickListener {
                expandableGroup.onToggleExpanded()
                viewBinding.icExpand.setImageResource(if (expandableGroup.isExpanded) R.drawable.ic_collapse_animated else R.drawable.ic_expand_animated)
                val drawable = icExpand.drawable as Animatable
                drawable.start()
            }
        }
    }

    // delete item if swipe to left!
    override fun getSwipeDirs(): Int = ItemTouchHelper.LEFT

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        this.expandableGroup = onToggleListener
    }
}