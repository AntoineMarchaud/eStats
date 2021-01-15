package com.amarchaud.estats.dialog

import android.database.Cursor
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.amarchaud.estats.adapter.ContactItem
import com.amarchaud.estats.databinding.DialogListContactBinding
import com.amarchaud.estats.model.other.Contact
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.parcelize.Parcelize



class ListContactDialog : DialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        const val TAG = "ListContactDialog"

        //out
        const val KEY_RESULT_CONTACT = "KEY_RESULT_CONTACT"
        const val KEY_RESULT_LIST_CONTACT = "KEY_LIST_CONTACTS"

        fun newInstance(): ListContactDialog {

            val fragment = ListContactDialog()

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private val PROJECTION: Array<out String> = arrayOf(
        StructuredPostal._ID,
        StructuredPostal.DISPLAY_NAME,
        StructuredPostal.CITY,
        StructuredPostal.STREET,
        StructuredPostal.COUNTRY
    )

    private var _binding: DialogListContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = DialogListContactBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializes the loader
        LoaderManager.getInstance(this).initLoader(0, null, this)

        // Gets the ListView from the View list of the parent activity
        activity?.also {

            with(binding) {
                with(listContacts) {
                    layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                    adapter = groupAdapter
                }

                okButton.setOnClickListener {

                    val listContacts = arrayListOf<Contact>()
                    for (i in 0 until groupAdapter.itemCount) {
                        with(groupAdapter.getItem(i) as ContactItem) {
                            listContacts.add(Contact(this.name, this.addr))
                        }
                    }

                    val result: Bundle = Bundle().apply {
                        putParcelableArrayList(KEY_RESULT_LIST_CONTACT, listContacts)
                    }

                    // send result to Listener(s)
                    requireActivity().supportFragmentManager.setFragmentResult(KEY_RESULT_CONTACT, result)

                    dismiss()
                }
                cancelButton.setOnClickListener {
                    dismiss()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        activity?.let {
            return CursorLoader(it, StructuredPostal.CONTENT_URI, PROJECTION, null, null, null)
        } ?: throw IllegalStateException()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        while (cursor.moveToNext()) {
            with(cursor) {
                val name = getString(getColumnIndex(StructuredPostal.DISPLAY_NAME))
                val addr = getString(getColumnIndex(StructuredPostal.STREET))
                groupAdapter.add(ContactItem(name, addr))
                Log.d(TAG, "add : $name $addr")
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        groupAdapter.clear()
    }
}


