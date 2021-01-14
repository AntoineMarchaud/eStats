package com.amarchaud.estats.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import org.osmdroid.util.GeoPoint

object GeoCoder {

    fun getLocationFromAddress(strAddress: String?, ctx: Context): GeoPoint? {

        if(strAddress == null)
            return null

        val coder = Geocoder(ctx);
        val address: MutableList<Address>

        address = coder.getFromLocationName(strAddress, 5);
        if (address == null) {
            return null;
        }
        val location = address[0];
        location.latitude;
        location.longitude;

        return GeoPoint(location.latitude * 1E6, location.getLongitude() * 1E6)
    }
}