package com.amarchaud.estats.extension

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


fun MapView.addMarker(lat: Double, lon: Double, name: String?) {
    val oneMarker = Marker(this)
    oneMarker.position = GeoPoint(lat, lon)
    oneMarker.title = name
    oneMarker.setTextIcon(name) // displayed on screen
    oneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    val added = this.overlays.add(oneMarker)
}

fun MapView.removeMarker(lat: Double, lon: Double, name: String?) {
    this.overlays.firstOrNull {
        if (it is Marker) (it.position.latitude == lat && it.position.longitude == lon && it.title == name) else false
    }?.let {
        val removed = this.overlays.remove(it)
        this.requestLayout()
    }
}