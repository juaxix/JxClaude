package com.jx.claude.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.jx.claude.R
import com.jx.claude.utils.ClaudeGrammarLocator
import io.noties.markwon.Markwon
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.syntax.Prism4jSyntaxHighlight
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.prism4j.Prism4j
import org.commonmark.node.FencedCodeBlock

class CodeBlockEntry : MarkwonAdapter.Entry<FencedCodeBlock, CodeBlockEntry.Holder>() {

    companion object {
        // Shared across all code blocks — created once
        private val prism4j = Prism4j(ClaudeGrammarLocator())
        private val prismTheme = Prism4jThemeDarkula.create()
        private val highlighter = Prism4jSyntaxHighlight.create(prism4j, prismTheme)
    }

    class Holder(itemView: View) : MarkwonAdapter.Holder(itemView) {
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvLanguage: TextView = itemView.findViewById(R.id.tvLanguage)
        val btnCopyCode: ImageButton = itemView.findViewById(R.id.btnCopyCode)
    }

    override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): Holder {
        return Holder(inflater.inflate(R.layout.item_code_block, parent, false))
    }

    override fun bindHolder(markwon: Markwon, holder: Holder, node: FencedCodeBlock) {
        val code = node.literal?.trimEnd() ?: ""
        val lang = node.info?.takeIf { it.isNotBlank() }

        // Language label
        holder.tvLanguage.text = lang ?: "code"

        // Syntax-highlight directly with Prism4j (no Markwon code-block wrapper)
        val highlighted = highlighter.highlight(lang, code)
        holder.tvCode.setText(highlighted, TextView.BufferType.SPANNABLE)

        // Copy code button
        holder.btnCopyCode.setOnClickListener {
            val clipboard =
                it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(it.context, "Code copied!", Toast.LENGTH_SHORT).show()
        }
    }
}