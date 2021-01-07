package com.amarchaud.estats.bindingadapter

import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.util.concurrent.TimeUnit

object TimeStransformation {
    @JvmStatic
    @BindingAdapter("onLongMsToTime")
    fun setOnImageLoadFromUrl(view: TextView, timems: Long?) {
        timems?.let {
            view.text = when (it) {
                in (0L..60000L) -> { // < 1 min
                    String.format(
                        "%02ds",
                        TimeUnit.MILLISECONDS.toMinutes(it),
                    )
                }
                in (60000L..3600000L) -> { // >= 1 min && < 1 hour
                    String.format(
                        "%02dm:%02ds",
                        TimeUnit.MILLISECONDS.toMinutes(it),
                        TimeUnit.MILLISECONDS.toSeconds(it) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(it)
                        )
                    )
                }
                in (3600000L..86400000L) -> { // >= 1 hour && < 1 day
                    String.format(
                        "%02dh%02dm:%02ds",
                        TimeUnit.MILLISECONDS.toHours(it),
                        TimeUnit.MILLISECONDS.toMinutes(it) - TimeUnit.HOURS.toMinutes(
                            TimeUnit.MILLISECONDS.toHours(it)
                        ),
                        TimeUnit.MILLISECONDS.toSeconds(it) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(it)
                        )
                    )
                }
                else -> { // >= 1day
                    String.format(
                        "%02dd%02dh%02dm:%02ds",
                        TimeUnit.MILLISECONDS.toDays(it),
                        TimeUnit.MILLISECONDS.toHours(it) - TimeUnit.DAYS.toHours(
                            TimeUnit.MILLISECONDS.toDays(it)
                        ),
                        TimeUnit.MILLISECONDS.toMinutes(it) - TimeUnit.HOURS.toMinutes(
                            TimeUnit.MILLISECONDS.toHours(it)
                        ),
                        TimeUnit.MILLISECONDS.toSeconds(it) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(it)
                        )
                    )
                }
            }
        }
    }
}