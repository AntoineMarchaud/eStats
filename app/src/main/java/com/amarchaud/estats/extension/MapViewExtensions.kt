package com.amarchaud.estats.extension

import com.amarchaud.estats.BuildConfig
import com.amarchaud.estats.dialog.AddCurrentLocationDialog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


fun MapView.addMarker(lat: Double, lon: Double, name: String?) {
    val oneMarker = Marker(this)
    oneMarker.position = GeoPoint(lat, lon)
    oneMarker.title = name
    oneMarker.setTextIcon(name) // displayed on screen
    oneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    this.overlays.add(oneMarker)
}

fun MapView.removeMarker(lat: Double, lon: Double, name: String?) {
    this.overlays.firstOrNull {
        if (it is Marker) (it.position.latitude == lat && it.position.longitude == lon && it.title == name) else false
    }?.let {
        this.overlays.remove(it)
        this.requestLayout()
    }
}


fun MapView.initMapView(center : GeoPoint) {
    Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(false)
    controller.setZoom(15.0)
    setExpectedCenter(center)
}