package com.frag2win.pocketmind.ui.chat

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
    text = MaterialTheme.colorScheme.onSurface,
    codeBackground = Color(0xFF0F172A), // Slate 900
    dividerColor = MaterialTheme.colorScheme.outlineVariant
)

@Composable
fun pocketMindMarkdownTypography(): MarkdownTypography = markdownTypography(
    h1 = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    h2 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    h3 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
)
