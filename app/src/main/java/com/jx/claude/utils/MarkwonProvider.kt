package com.jx.claude.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin

object MarkwonProvider {

    private var markwon: Markwon? = null

    fun get(context: Context): Markwon {
        if (markwon == null) {
            markwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .codeBackgroundColor(Color.parseColor("#252045"))
                            .codeTextColor(Color.parseColor("#E0D0FF"))
                            .codeTextSize(dpToPx(context, 13))
                            .codeTypeface(Typeface.MONOSPACE)
                            .codeBlockMargin(dpToPx(context, 8))
                            .blockQuoteColor(Color.parseColor("#7C4DFF"))
                            .blockQuoteWidth(dpToPx(context, 3))
                            .blockMargin(dpToPx(context, 12))
                            .headingBreakHeight(0)
                            .headingTextSizeMultipliers(
                                floatArrayOf(1.5f, 1.3f, 1.17f, 1.1f, 1.0f, 0.9f)
                            )
                            .bulletListItemStrokeWidth(dpToPx(context, 1))
                            .listItemColor(Color.parseColor("#B388FF"))
                            .thematicBreakColor(Color.parseColor("#44B388FF"))
                            .thematicBreakHeight(dpToPx(context, 1))
                            .linkColor(Color.parseColor("#B388FF"))
                    }
                })
                .build()
        }
        return markwon!!
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}