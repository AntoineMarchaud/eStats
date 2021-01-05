package com.amarchaud.estats.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Distance {
    private const val R = 6378.137; // Radius of earth in KM
    fun measure(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        val dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                sin(dLon / 2) * sin(dLon / 2);
        val c = 2 * atan2(Math.sqrt(a), sqrt(1 - a));
        val d = R * c;
        return d * 1000; // meters
    }
}