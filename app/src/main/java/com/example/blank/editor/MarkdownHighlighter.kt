package com.example.blank.editor

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.text.getSpans

class MarkdownHighlighter {
    private val dark = 0xFF141414.toInt()
    private val headingBlue = 0xFF0969DA.toInt()
    private val syntaxGray = 0xFF6E7781.toInt()
    private val codeGray = 0xFF57606A.toInt()

    private val titleRegex = Regex("^(#{1,6})\\s+.+$", RegexOption.MULTILINE)
    private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    private val italicRegex = Regex("(?<!\\*)\\*(?!\\s)(.+?)(?<!\\s)\\*(?!\\*)")
    private val listRegex = Regex("^(\\s*[-*+]\\s+.+)$", RegexOption.MULTILINE)
    private val codeFenceRegex = Regex("```([\\s\\S]*?)```")

    fun apply(editable: Editable) {
        editable.getSpans<ForegroundColorSpan>(0, editable.length).forEach(editable::removeSpan)
        editable.getSpans<StyleSpan>(0, editable.length).forEach(editable::removeSpan)
        editable.getSpans<TypefaceSpan>(0, editable.length).forEach(editable::removeSpan)

        editable.setSpan(
            ForegroundColorSpan(dark),
            0,
            editable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        titleRegex.findAll(editable).forEach { match ->
            editable.setSpan(
                ForegroundColorSpan(headingBlue),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editable.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        boldRegex.findAll(editable).forEach { match ->
            editable.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        italicRegex.findAll(editable).forEach { match ->
            editable.setSpan(
                StyleSpan(Typeface.ITALIC),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        listRegex.findAll(editable).forEach { match ->
            editable.setSpan(
                ForegroundColorSpan(syntaxGray),
                match.range.first,
                match.range.first + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        codeFenceRegex.findAll(editable).forEach { match ->
            editable.setSpan(
                ForegroundColorSpan(codeGray),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editable.setSpan(
                TypefaceSpan("monospace"),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
