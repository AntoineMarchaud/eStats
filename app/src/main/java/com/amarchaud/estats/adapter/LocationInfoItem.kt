package com.amarchaud.estats.adapter

import android.view.View
import com.amarchaud.estats.R
import com.amarchaud.estats.bindingadapter.TimeTransformation
import com.amarchaud.estats.databinding.ItemLocationBinding
import com.amarchaud.estats.model.entity.LocationInfo
import com.xwray.groupie.viewbinding.BindableItem


open class LocationInfoItem(open var locationInfo: LocationInfo) :
    BindableItem<ItemLocationBinding>() {

    override fun getLayout(): Int = R.layout.item_location

    override fun initializeViewBinding(view: View): ItemLocationBinding =
        ItemLocationBinding.bind(view)

    override fun bind(viewBinding: ItemLocationBinding, position: Int) {
        viewBinding.apply {
            name.text = locationInfo.name
            subLat.text = java.lang.String.valueOf(locationInfo.lat)
            subLon.text = java.lang.String.valueOf(locationInfo.lon)
            TimeTransformation.setOnImageLoadFromUrl(subDuration, locationInfo.duration_day)
        }
    }
}