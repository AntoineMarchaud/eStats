package com.amarchaud.estats.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.IOException

object GeoCoder {


    suspend fun getLocationFromAddressSuspend(strAddress: String?, ctx: Context): GeoPoint? {

        val coder = Geocoder(ctx);
        val address: MutableList<Address> = withContext(Dispatchers.IO) {
            try {
                coder.getFromLocationName(strAddress, 1)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } ?: return null

        val location = address[0]
        location.latitude;
        location.longitude;

        return GeoPoint(location.latitude, location.longitude)

    }

    fun getLocationFromAddress(strAddress: String?, ctx: Context): GeoPoint? {


        val coder = Geocoder(ctx);
        val address: MutableList<Address>?
        try {
            address = coder.getFromLocationName(strAddress, 1)
            if (address == null)
                return null

            val location = address[0]
            location.latitude;
            location.longitude;

            return GeoPoint(location.latitude, location.longitude)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}

