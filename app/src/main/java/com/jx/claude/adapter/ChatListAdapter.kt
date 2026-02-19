package com.jx.claude.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jx.claude.R
import com.jx.claude.models.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onClick: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit
) : ListAdapter<ChatSession, ChatListAdapter.VH>(DIFF) {

    var selectedId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatSession>() {
            override fun areItemsTheSame(a: ChatSession, b: ChatSession) = a.id == b.id
            override fun areContentsTheSame(a: ChatSession, b: ChatSession) =
                a.title == b.title && a.messages.size == b.messages.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_session, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvChatTitle)
        private val tvDate: TextView = view.findViewById(R.id.tvChatDate)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(session: ChatSession) {
            tvTitle.text = session.title
            tvDate.text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                .format(Date(session.createdAt))

            val isSelected = session.id == selectedId
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.bg_drawer_item_selected
                else R.drawable.bg_drawer_item
            )

            itemView.setOnClickListener { onClick(session) }
            btnDelete.setOnClickListener { onDelete(session) }
        }
    }
}