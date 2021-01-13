package com.amarchaud.estats.utils

import java.util.concurrent.TimeUnit

object TimeTransformation {

    fun millisecondToTimeStr(timeMs: Long): String {
        return when (timeMs) {
            in (0L..60000L) -> { // < 1 min
                String.format(
                    "%02ds",
                    TimeUnit.MILLISECONDS.toSeconds(timeMs),
                )
            }
            in (60000L..3600000L) -> { // >= 1 min && < 1 hour
                String.format(
                    "%02dm:%02ds",
                    TimeUnit.MILLISECONDS.toMinutes(timeMs),
                    TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(timeMs)
                    )
                )
            }
            in (3600000L..86400000L) -> { // >= 1 hour && < 1 day
                String.format(
                    "%02dh%02dm:%02ds",
                    TimeUnit.MILLISECONDS.toHours(timeMs),
                    TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(timeMs)
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(timeMs)
                    )
                )
            }
            else -> { // >= 1day
                String.format(
                    "%02dd%02dh%02dm:%02ds",
                    TimeUnit.MILLISECONDS.toDays(timeMs),
                    TimeUnit.MILLISECONDS.toHours(timeMs) - TimeUnit.DAYS.toHours(
                        TimeUnit.MILLISECONDS.toDays(timeMs)
                    ),
                    TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(timeMs)
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(timeMs)
                    )
                )
            }
        }
    }
}