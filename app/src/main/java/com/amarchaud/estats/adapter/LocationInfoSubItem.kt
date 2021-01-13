package com.amarchaud.estats.adapter

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemSubLocationBinding
import com.amarchaud.estats.model.entity.LocationInfoSub
import com.amarchaud.estats.utils.TimeTransformation
import com.amarchaud.estats.view.MainFragment
import com.xwray.groupie.viewbinding.BindableItem

/**
 * Example with DataBinding only
 */
class LocationInfoSubItem(var locationInfoSubParam: LocationInfoSub,
                          var typeIndexDisplayed: Int) :
    BindableItem<ItemSubLocationBinding>() {

    override fun getLayout(): Int = R.layout.item_sub_location

    override fun initializeViewBinding(view: View): ItemSubLocationBinding = ItemSubLocationBinding.bind(view)

    override fun bind(viewBinding: ItemSubLocationBinding, position: Int) {
        viewBinding.locationInfoSub = locationInfoSubParam

        viewBinding.apply {
            when (typeIndexDisplayed) {
                MainFragment.Companion.DurationType.DAY.value -> subDuration.text = TimeTransformation.millisecondToTimeStr(locationInfoSubParam.duration_day)
                MainFragment.Companion.DurationType.WEEK.value -> subDuration.text = TimeTransformation.millisecondToTimeStr(locationInfoSubParam.duration_week)
                MainFragment.Companion.DurationType.MONTH.value -> subDuration.text = TimeTransformation.millisecondToTimeStr(locationInfoSubParam.duration_month)
                MainFragment.Companion.DurationType.YEAR.value -> subDuration.text = TimeTransformation.millisecondToTimeStr(locationInfoSubParam.duration_year)
                MainFragment.Companion.DurationType.ALL_TIME.value -> subDuration.text = TimeTransformation.millisecondToTimeStr(locationInfoSubParam.duration_all_time)
            }
            viewBinding.subDuration
        }

    }

    // delete item if swipe to left!
    override fun getSwipeDirs(): Int  = ItemTouchHelper.LEFT
}