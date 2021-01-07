package com.amarchaud.estats.adapter

import android.view.View
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemSubLocationBinding
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.xwray.groupie.viewbinding.BindableItem

class LocationInfoSubItem(private val locationInfoSub: LocationInfoSub) :
    BindableItem<ItemSubLocationBinding>() {

    override fun bind(viewBinding: ItemSubLocationBinding, position: Int) {
        viewBinding.locationInfoSub = locationInfoSub
    }

    override fun getLayout(): Int = R.layout.item_sub_location

    override fun initializeViewBinding(view: View): ItemSubLocationBinding {
        return ItemSubLocationBinding.bind(view)
    }
}