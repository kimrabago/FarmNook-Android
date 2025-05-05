package com.ucb.capstone.farmnook.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(private var contacts: List<String>, private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    fun updateList(newList: List<String>) {
        contacts = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(contact: String) {
            itemView.findViewById<TextView>(android.R.id.text1).text = contact
            itemView.setOnClickListener {
                onClick(contact)
            }
        }
    }
}

