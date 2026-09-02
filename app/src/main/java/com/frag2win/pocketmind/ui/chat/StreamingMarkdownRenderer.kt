package com.frag2win.pocketmind.ui.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.TextUnit

/**
 * A lightweight, regex-based stream-safe markdown parser.
 * It handles partial markdown syntax (unclosed **, *, `) by treating them as 
 * plain text until the closing delimiter arrives, preventing "syntax flashing".
 */
fun String.toLiveAnnotatedString(
    primaryColor: Color = Color.White,
    accentColor: Color = Color(0xFF22D3EE)
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < this@toLiveAnnotatedString.length) {
            when {
                // Bold handling: **text**
                this@toLiveAnnotatedString.startsWith("**", i) -> {
                    val end = this@toLiveAnnotatedString.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) {
                            append(this@toLiveAnnotatedString.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        // Unclosed bold - treat delimiter as text to avoid raw symbol look
                        append(this@toLiveAnnotatedString.substring(i, i + 1))
                        i++
                    }
                }
                // Italic handling: *text*
                this@toLiveAnnotatedString.startsWith("*", i) -> {
                    val end = this@toLiveAnnotatedString.indexOf("*", i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = primaryColor)) {
                            append(this@toLiveAnnotatedString.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(this@toLiveAnnotatedString.substring(i, i + 1))
                        i++
                    }
                }
                // Inline Code handling: `code`
                this@toLiveAnnotatedString.startsWith("`", i) -> {
                    val end = this@toLiveAnnotatedString.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF1E1F20),
                            color = accentColor
                        )) {
                            append(this@toLiveAnnotatedString.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(this@toLiveAnnotatedString.substring(i, i + 1))
                        i++
                    }
                }
                else -> {
                    append(this@toLiveAnnotatedString[i])
                    i++
                }
            }
        }
    }
}

/**
 * Performance-optimized component for streaming text.
 * It uses a keyed remember block to avoid parsing on every single character 
 * if only whitespace or structural noise is added.
 */
@Composable
fun StreamingMarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.tertiary

    val annotatedString = remember(content, textColor, accentColor) {
        content.toLiveAnnotatedString(primaryColor = textColor, accentColor = accentColor)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = TextUnit.Unspecified),
        color = textColor
    )
}
