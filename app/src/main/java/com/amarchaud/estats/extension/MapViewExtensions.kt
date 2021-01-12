package com.amarchaud.estats.extension

import com.amarchaud.estats.BuildConfig
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

fun MapView.initMapView(center: GeoPoint) {
    Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // VERY IMPORTANT !
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(false)
    controller.setZoom(15.0)
    setExpectedCenter(center)
}


fun MapView.addMarker(lat: Double, lon: Double, title: String?, id: Int) {
    val oneMarker = Marker(this)
    oneMarker.position = GeoPoint(lat, lon)
    if (title != null) {
        oneMarker.title = title // display when click on the marker
        oneMarker.setTextIcon(title) // displayed on screen
    }
    if (id >= 0)
        oneMarker.id = id.toString()
    oneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    this.overlayManager.add(oneMarker)
}

fun MapView.removeMarker(id: Int) {
    if (id >= 0) {
        this.overlays.firstOrNull {
            if (it is Marker) (it.id == id.toString()) else false
        }?.let {
            this.overlayManager.remove(it)
            this.requestLayout()
        }
    }
}


fun MapView.createCircle(center: GeoPoint, radiusInMeters: Double, color: Int, id: Int): Polygon {
    val circle: List<GeoPoint> = Polygon.pointsAsCircle(center, radiusInMeters)
    val p = Polygon(this).apply {
        points = circle
        //title = "A circle" // display when click on the circle
        outlinePaint.apply {
            this.color = color
        }
    }
    if (id >= 0)
        p.id = id.toString()
    return p
}


fun MapView.addCircle(center: GeoPoint, radiusInMeters: Double, color: Int, id: Int) {
    val p = createCircle(center, radiusInMeters, color, id)
    this.overlayManager.add(p)
    invalidate()
}

fun MapView.removeCirle(id: Int) {
    if (id >= 0) {
        this.overlays.firstOrNull {
            if (it is Polygon) (it.id == id.toString()) else false
        }?.let {
            this.overlayManager.remove(it)
            this.requestLayout()
        }
    }
}
