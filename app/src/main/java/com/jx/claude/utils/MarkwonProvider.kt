package com.jx.claude.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j

object MarkwonProvider {

    private var markwon: Markwon? = null
    fun get(context: Context): Markwon {
        if (markwon == null) {
            // Prism4j handles language grammars
            val prism4j = Prism4j(ClaudeGrammarLocator())
            val prism4jTheme = Prism4jThemeDarkula.create()

            markwon = Markwon.builder(context.applicationContext)
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .codeBlockBackgroundColor(Color.parseColor("#2B2B2B"))
                            .codeBlockTypeface(Typeface.MONOSPACE)
                            .codeBlockTextSize(
                                (context.resources.displayMetrics.scaledDensity * 13).toInt()
                            )
                            .codeBackgroundColor(Color.parseColor("#2B2B2B"))
                            .codeTextColor(Color.parseColor("#A9B7C6"))
                            .codeTypeface(Typeface.MONOSPACE)
                    }

                    // Makes links tappable in all TextViews rendered by Markwon
                    override fun afterSetText(textView: TextView) {
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    }
                })
                .build()
        }
        return markwon!!
    }
}