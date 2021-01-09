package com.amarchaud.estats.adapter

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemSubLocationBinding
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.xwray.groupie.viewbinding.BindableItem

/**
 * Example with DataBinding only
 */
class LocationInfoSubItem(var locationInfoSub: LocationInfoSub) :
    BindableItem<ItemSubLocationBinding>() {

    override fun getLayout(): Int = R.layout.item_sub_location

    override fun initializeViewBinding(view: View): ItemSubLocationBinding = ItemSubLocationBinding.bind(view)

    override fun bind(viewBinding: ItemSubLocationBinding, position: Int) {
        viewBinding.locationInfoSub = locationInfoSub
    }

    // delete item if swipe to left!
    override fun getSwipeDirs(): Int  = ItemTouchHelper.LEFT
}