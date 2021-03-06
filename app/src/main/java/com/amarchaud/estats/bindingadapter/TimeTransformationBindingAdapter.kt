package com.amarchaud.estats.bindingadapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.amarchaud.estats.utils.TimeTransformation
import kotlinx.coroutines.MainScope

object TimeTransformationBindingAdapter {
    @JvmStatic
    @BindingAdapter("onLongMsToTime")
    fun setOnImageLoadFromUrl(view: TextView, timeMs: Long?) {
        timeMs?.let {
            view.text = TimeTransformation.millisecondToTimeStr(timeMs)
        }
    }
}