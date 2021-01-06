package com.amarchaud.estats.fake

import com.amarchaud.estats.model.entity.LocationInfo
import com.amarchaud.estats.model.entity.LocationInfoSub
import kotlin.random.Random

object DataProvider {

    fun getRandomLocationInfo(
        countItems: Int
    ): Pair<LocationInfo, List<LocationInfoSub>> {

        return Pair(
            LocationInfo(
                id = 0,
                name = "randomData",
                lat = Random.nextDouble(48.0, 49.0),
                lon = Random.nextDouble(1.0, 2.0)
            ),
            (1..countItems).map {
                LocationInfoSub(
                    name = "Random sub $it",
                    lat = Random.nextDouble(48.0, 49.0),
                    lon = Random.nextDouble(1.0, 2.0)
                )
            })

    }
}