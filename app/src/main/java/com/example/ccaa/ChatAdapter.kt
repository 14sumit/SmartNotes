package com.example.ccaa

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatList: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val textView: TextView) :
        RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val textView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView

        return ChatViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        holder.textView.text = chatList[position].message
    }

    override fun getItemCount(): Int = chatList.size
}