package com.jx.claude.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jx.claude.R
import com.jx.claude.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.id == b.id
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position).isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            BotVH(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserVH -> holder.bind(msg)
            is BotVH -> holder.bind(msg)
        }
    }

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.content
            tvTimestamp.text =
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
        }
    }

    class BotVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val thinkingSection: LinearLayout = view.findViewById(R.id.thinkingSection)
        private val tvThinkingToggle: TextView = view.findViewById(R.id.tvThinkingToggle)
        private val tvThinkingContent: TextView = view.findViewById(R.id.tvThinkingContent)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.content
            tvTimestamp.text =
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

            if (!msg.thinkingContent.isNullOrBlank()) {
                thinkingSection.visibility = View.VISIBLE
                tvThinkingContent.text = msg.thinkingContent

                var expanded = false
                tvThinkingToggle.setOnClickListener {
                    expanded = !expanded
                    tvThinkingContent.visibility = if (expanded) View.VISIBLE else View.GONE
                    tvThinkingToggle.text =
                        if (expanded) "🧠 Thinking  ▼" else "🧠 Thinking  ▶"
                }
            } else {
                thinkingSection.visibility = View.GONE
            }
        }
    }
}