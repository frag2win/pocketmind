package com.frag2win.pocketmind.ui.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun pocketMindMarkdownColors(): MarkdownColors = markdownColor(
    text = MaterialTheme.colorScheme.onBackground,
    inlineCodeBackground = if (isSystemInDarkTheme()) Color(0xFF1E1F20) else Color(0xFFE2E8F0),
    dividerColor = MaterialTheme.colorScheme.outlineVariant
)

@Composable
fun pocketMindMarkdownTypography(): MarkdownTypography {
    val textColor = MaterialTheme.colorScheme.onBackground
    return markdownTypography(
        h1 = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = textColor),
        h2 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = textColor),
        h3 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = textColor),
        paragraph = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        code = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    )
}
