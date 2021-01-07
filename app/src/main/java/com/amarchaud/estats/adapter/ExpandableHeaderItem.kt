package com.amarchaud.estats.adapter

import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemLocationBinding
import com.amarchaud.estats.model.entity.LocationInfo
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem

class ExpandableHeaderItem(override var locationInfo: LocationInfo) : LocationInfoItem(locationInfo),
    ExpandableItem {

    private lateinit var expandableGroup: ExpandableGroup

    override fun bind(viewBinding: ItemLocationBinding, position: Int) {
        super.bind(viewBinding, position)
        viewBinding.icExpand.setImageResource(if (expandableGroup.isExpanded) R.drawable.ic_expanded_indicator else R.drawable.ic_collapsed_indicator)
        viewBinding.icExpand.setOnClickListener {
            expandableGroup.onToggleExpanded()
            viewBinding.icExpand.setImageResource(if (expandableGroup.isExpanded) R.drawable.ic_expanded_indicator else R.drawable.ic_collapsed_indicator)
        }
    }

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        this.expandableGroup = onToggleListener
    }
}