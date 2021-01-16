package com.amarchaud.estats.adapter

import android.view.View
import com.amarchaud.estats.R
import com.amarchaud.estats.databinding.ItemContactBinding
import com.xwray.groupie.viewbinding.BindableItem

class ContactItem(var name: String, var addr: String) : BindableItem<ItemContactBinding>() {

    var isChecked = true
        private set

    override fun bind(viewBinding: ItemContactBinding, position: Int) {
        viewBinding.apply {
            contactName.text = name
            contactAddr.text = addr

            contactToAdd.setOnClickListener {
                isChecked = contactToAdd.isChecked
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_contact

    override fun initializeViewBinding(view: View): ItemContactBinding = ItemContactBinding.bind(view)
}