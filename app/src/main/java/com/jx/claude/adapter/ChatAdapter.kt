package com.jx.claude.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jx.claude.R
import com.jx.claude.models.ChatMessage
import com.jx.claude.utils.MarkwonProvider
import com.jx.claude.utils.ModelCapabilityHelper
import io.noties.markwon.recycler.MarkwonAdapter
import org.commonmark.node.FencedCodeBlock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1
        private const val TYPE_ERROR = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.content == newItem.content &&
                        oldItem.thinkingContent == newItem.thinkingContent &&
                        oldItem.inputTokens == newItem.inputTokens &&
                        oldItem.outputTokens == newItem.outputTokens &&
                        oldItem.tokensPerSecond == newItem.tokensPerSecond &&
                        oldItem.estimatedCost == newItem.estimatedCost
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        return when {
            msg.isUser -> TYPE_USER
            msg.isError -> TYPE_ERROR
            else -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(
                inflater.inflate(R.layout.item_message_user, parent, false)
            )
            TYPE_ERROR -> ErrorViewHolder(
                inflater.inflate(R.layout.item_message_error, parent, false)
            )
            else -> BotViewHolder(
                inflater.inflate(R.layout.item_message_bot, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder) {
            is UserViewHolder -> bindUser(holder, message)
            is BotViewHolder -> bindBot(holder, message)
            is ErrorViewHolder -> bindError(holder, message)
        }
    }

    // ── Bind: User ──────────────────────────────────────────────

    private fun bindUser(holder: UserViewHolder, message: ChatMessage) {
        val prefix = message.attachmentNames
            ?.joinToString("\n") { "📎 $it" }

        holder.tvMessage.text = if (prefix != null)
            "$prefix\n\n${message.content}" else message.content

        holder.tvTimestamp.text = formatTimestamp(message.timestamp)

        holder.btnCopy.setOnClickListener {
            copyToClipboard(it.context, message.content)
        }
    }

    // ── Bind: Bot ───────────────────────────────────────────────

    private fun bindBot(holder: BotViewHolder, message: ChatMessage) {
        val markwon = MarkwonProvider.get(holder.itemView.context)
        if (message.content.isNotBlank()) {
            holder.markwonAdapter.setMarkdown(markwon, message.content)
            holder.markwonAdapter.notifyDataSetChanged()
        }

        holder.tvTimestamp.text = formatTimestamp(message.timestamp)

        // Copy entire raw message
        holder.btnCopy.setOnClickListener {
            copyToClipboard(it.context, message.content)
        }

        // ── Token info ──────────────────────────────────────────
        val tokenParts = mutableListOf<String>()

        message.tokensPerSecond?.let {
            tokenParts.add("⚡ %.1f tok/s".format(it))
        }
        if (message.inputTokens != null && message.outputTokens != null) {
            tokenParts.add(
                "📊 %,d → %,d tokens".format(message.inputTokens, message.outputTokens)
            )
        }
        message.estimatedCost?.let {
            tokenParts.add(ModelCapabilityHelper.formatCost(it))
        }

        if (tokenParts.isNotEmpty()) {
            holder.tvTokenInfo.visibility = View.VISIBLE
            holder.tvTokenInfo.text = tokenParts.joinToString("  •  ")
        } else {
            holder.tvTokenInfo.visibility = View.GONE
        }

        // ── Thinking section ────────────────────────────────────
        if (!message.thinkingContent.isNullOrBlank()) {
            holder.thinkingSection.visibility = View.VISIBLE
            holder.tvThinkingContent.text = message.thinkingContent

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

    // ── Bind: Error ─────────────────────────────────────────────

    private fun bindError(holder: ErrorViewHolder, message: ChatMessage) {
        holder.tvErrorMessage.text = "⚠️  ${message.content}"
        holder.tvTimestamp.text = formatTimestamp(message.timestamp)
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ── ViewHolders ─────────────────────────────────────────────

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rvMarkdown: RecyclerView = view.findViewById(R.id.rvMarkdown)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvTokenInfo: TextView = view.findViewById(R.id.tvTokenInfo)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        val thinkingSection: LinearLayout = view.findViewById(R.id.thinkingSection)
        val tvThinkingToggle: TextView = view.findViewById(R.id.tvThinkingToggle)
        val tvThinkingContent: TextView = view.findViewById(R.id.tvThinkingContent)

        val markwonAdapter: MarkwonAdapter =
            MarkwonAdapter.builderTextViewIsRoot(R.layout.item_default_text)
                .include(FencedCodeBlock::class.java, CodeBlockEntry())
                .build()

        init {
            rvMarkdown.layoutManager = LinearLayoutManager(view.context)
            rvMarkdown.adapter = markwonAdapter
        }
    }

    class ErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvErrorMessage: TextView = view.findViewById(R.id.tvErrorMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }
}