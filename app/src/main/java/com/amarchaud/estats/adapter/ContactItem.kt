package com.amarchaud.estats.adapter

import android.view.View
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemContactBinding
import com.xwray.groupie.viewbinding.BindableItem

class ContactItem(var name: String, var addr: String) : BindableItem<ItemContactBinding>() {

    override fun bind(viewBinding: ItemContactBinding, position: Int) {
        viewBinding.apply {
            contactName.text = name
            contactAddr.text = addr
        }
    }

    override fun getLayout(): Int = R.layout.item_contact

    override fun initializeViewBinding(view: View): ItemContactBinding = ItemContactBinding.bind(view)
}