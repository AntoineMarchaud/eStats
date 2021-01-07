package com.amarchaud.estats.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemLocationBinding
import com.amarchaud.estats.databinding.ItemSubLocationBinding
import com.amarchaud.estats.model.entity.LocationWithSubs
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ItemsExpandableAdapter(var item: LocationWithSubs) :
    RecyclerView.Adapter<ItemsExpandableAdapter.ViewHolder>() {


    companion object {
        private const val VIEW_TYPE_MAIN_LOCATION = 0
        private const val VIEW_TYPE_SUB_LOCATION = 1
    }

    var isExpanded: Boolean by Delegates.observable(true) { _: KProperty<*>, _: Boolean, newExpandedValue: Boolean ->
        if (newExpandedValue) {
            notifyItemRangeInserted(1, item.subLocation.size)
            notifyItemChanged(0)
        } else {
            notifyItemRangeRemoved(1, item.subLocation.size)
            notifyItemChanged(0)
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_MAIN_LOCATION else VIEW_TYPE_SUB_LOCATION
    }

    override fun getItemCount(): Int = if (isExpanded) item.subLocation.size + 1 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_MAIN_LOCATION -> ViewHolder.LocationInfoViewHolder(
                DataBindingUtil.inflate(
                    inflater,
                    R.layout.item_location,
                    parent,
                    false
                )
            )
            else -> ViewHolder.LocationInfoSubViewHolder(
                DataBindingUtil.inflate(
                    inflater,
                    R.layout.item_sub_location,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.LocationInfoViewHolder -> {
                holder.binding.locationInfo = item.locationInfo

                holder.binding.icExpand.rotation =
                    if (isExpanded) 0F else 180F

                holder.binding.root.setOnClickListener {
                    isExpanded = !isExpanded
                }
            }
            is ViewHolder.LocationInfoSubViewHolder -> {
                holder.binding.locationInfoSub = item.subLocation[position - 1]
            }
        }
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class LocationInfoViewHolder(var binding: ItemLocationBinding) : ViewHolder(binding.root)
        class LocationInfoSubViewHolder(var binding: ItemSubLocationBinding) :
            ViewHolder(binding.root)
    }
}