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
import com.jx.claude.utils.MarkwonProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.content == newItem.content &&
                        oldItem.thinkingContent == newItem.thinkingContent
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder) {
            is UserViewHolder -> {
                holder.tvMessage.text = message.content
                holder.tvTimestamp.text = formatTimestamp(message.timestamp)
            }
            is BotViewHolder -> {
                // Render markdown for bot messages
                if (message.content.isNotBlank()) {
                    MarkwonProvider.get(holder.itemView.context)
                        .setMarkdown(holder.tvMessage, message.content)
                } else {
                    holder.tvMessage.text = ""
                }

                holder.tvTimestamp.text = formatTimestamp(message.timestamp)

                // Thinking section
                if (!message.thinkingContent.isNullOrBlank()) {
                    holder.thinkingSection.visibility = View.VISIBLE
                    holder.tvThinkingContent.text = message.thinkingContent

                    // Reset state on rebind
                    holder.tvThinkingContent.visibility = View.GONE
                    holder.tvThinkingToggle.text = "🧠 Thinking  ▶"

                    holder.tvThinkingToggle.setOnClickListener {
                        val visible = holder.tvThinkingContent.visibility == View.VISIBLE
                        holder.tvThinkingContent.visibility =
                            if (visible) View.GONE else View.VISIBLE
                        holder.tvThinkingToggle.text =
                            if (visible) "🧠 Thinking  ▶" else "🧠 Thinking  ▼"
                    }
                } else {
                    holder.thinkingSection.visibility = View.GONE
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val thinkingSection: LinearLayout = view.findViewById(R.id.thinkingSection)
        val tvThinkingToggle: TextView = view.findViewById(R.id.tvThinkingToggle)
        val tvThinkingContent: TextView = view.findViewById(R.id.tvThinkingContent)
    }
}