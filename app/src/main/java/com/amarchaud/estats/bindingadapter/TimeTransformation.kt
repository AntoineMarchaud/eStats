package com.amarchaud.estats.bindingadapter

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.amarchaud.estats.utils.TimeTransformation
import java.util.concurrent.TimeUnit

object TimeTransformationBindingAdapter {
    @JvmStatic
    @BindingAdapter("onLongMsToTime")
    fun setOnImageLoadFromUrl(view: TextView, timeMs: Long?) {
        timeMs?.let {
            view.text = TimeTransformation.MillisecondToTimeStr(timeMs)
        }
    }
}