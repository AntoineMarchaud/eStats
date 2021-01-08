package com.amarchaud.estats.adapter.decoration

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.TouchCallback

abstract class SwipeTouchCallback() : TouchCallback() {
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (ItemTouchHelper.ACTION_STATE_SWIPE == actionState) {
            val child = viewHolder.itemView
            // Fade out the item
            child.alpha = 1 - Math.abs(dX) / child.width.toFloat()
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

}
