package com.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    if (text.isBlank()) return

    // Memoize parsing so streaming re-parses only on text change
    val segments = remember(text) { parseSegments(text) }
    val defaultStyle = if (textAlign != null) {
        MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    if (segments.isEmpty()) {
        Text(text = text, style = defaultStyle, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
        return
    }

    // If the only segment is plain text with no inline formatting, render fast path
    if (segments.size == 1 && segments[0] is MarkdownSegment.Text) {
        val seg = segments[0] as MarkdownSegment.Text
        if (!hasInlineMarkdown(seg.content)) {
            Text(text = seg.content, style = defaultStyle, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
            return
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Text -> {
                    if (segment.content.isNotBlank()) {
                        ClickableMarkdownText(
                            text = segment.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        )
                    }
                }
                is MarkdownSegment.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = segment.code,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownSegment.Table -> {
                    MarkdownTable(
                        headers = segment.headers,
                        rows = segment.rows,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
                is MarkdownSegment.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                is MarkdownSegment.Heading -> {
                    val headingStyle = when (segment.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        4 -> MaterialTheme.typography.titleMedium
                        5 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyLarge
                    }
                    ClickableMarkdownText(
                        text = segment.content,
                        style = headingStyle.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = if (segment.level <= 2) 12.dp else 8.dp, bottom = 4.dp),
                        textAlign = textAlign
                    )
                }
                is MarkdownSegment.UnorderedListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ClickableMarkdownText(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        )
                    }
                }
                is MarkdownSegment.OrderedListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "${segment.number}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ClickableMarkdownText(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        )
                    }
                }
                is MarkdownSegment.TaskListItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (segment.checked) "☑" else "☐",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (segment.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ClickableMarkdownText(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (segment.checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        )
                    }
                }
                is MarkdownSegment.Image -> {
                    val imgUriHandler = LocalUriHandler.current
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = segment.alt.ifEmpty { segment.url },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { imgUriHandler.openUri(segment.url) }
                        )
                    }
                }
                is MarkdownSegment.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        ClickableMarkdownText(
                            text = segment.content,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f),
                            textAlign = textAlign
                        )
                    }
                }
            }
        }
    }
}

// ── Inline formatting ────────────────────────────────────────────

// Patterns without color-dependent styles (colors applied at composable call site)
private val BOLD_PATTERN = Regex("""\*\*(.+?)\*\*""")
private val ITALIC_PATTERN = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
private val STRIKE_PATTERN = Regex("""~~(.+?)~~""")
private val CODE_PATTERN = Regex("""`(.+?)`""")

private val LINK_PATTERN = Regex("""\[(.+?)]\((.+?)\)""")

private fun hasInlineMarkdown(text: String): Boolean {
    return text.contains("**") || text.contains('*') || text.contains("~~") ||
           text.contains('`') || text.contains("[")
}

/**
 * Composable that renders an AnnotatedString with clickable URL support.
 * Detects StringAnnotation("URL") on tap and opens via the system browser.
 */
@Composable
private fun ClickableMarkdownText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val annotated = remember(text, linkColor, codeBackground) {
        buildFormattedAnnotatedString(text, linkColor, codeBackground)
    }
    val uriHandler = LocalUriHandler.current
    val effectiveStyle = if (textAlign != null) style.copy(color = color, textAlign = textAlign) else style.copy(color = color)
    ClickableText(
        text = annotated,
        style = effectiveStyle,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
        }
    )
}

/**
 * Build an AnnotatedString from markdown text, applying bold (**), italic (*),
 * strikethrough (~~), inline code (`), and links ([text](url)).
 * Unclosed delimiters are rendered as literal text.
 */
private fun buildFormattedAnnotatedString(
    input: String,
    linkColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color
) = buildAnnotatedString {

    // Build theme-aware inline patterns
    val inlinePatterns = listOf(
        BOLD_PATTERN to SpanStyle(fontWeight = FontWeight.Bold),
        ITALIC_PATTERN to SpanStyle(fontStyle = FontStyle.Italic),
        STRIKE_PATTERN to SpanStyle(textDecoration = TextDecoration.LineThrough),
        CODE_PATTERN to SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
    )

    // Collect all matches with their start/end positions
    data class Match(val start: Int, val end: Int, val style: SpanStyle, val url: String? = null)

    // Parse links first — extract text+url pairs and treat them as literal text
    val linkMatches = mutableListOf<Match>()
    val linkStripped = StringBuilder()
    var idx = 0
    while (idx < input.length) {
        val remaining = input.substring(idx)
        val linkMatch = LINK_PATTERN.find(remaining)
        if (linkMatch != null && linkMatch.range.first == 0) {
            val linkText = linkMatch.groupValues[1]
            val linkUrl = linkMatch.groupValues[2]
            val contentStart = linkStripped.length
            linkStripped.append(linkText)
            linkMatches.add(
                Match(contentStart, linkStripped.length, SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), linkUrl)
            )
            idx += linkMatch.value.length
        } else {
            linkStripped.append(remaining[0])
            idx++
        }
    }

    // Now parse inline formatting on the link-stripped text
    val inlineMatches = mutableListOf<Match>()
    for ((pattern, style) in inlinePatterns) {
        val matches = pattern.findAll(linkStripped.toString())
        for (m in matches) {
            val content = m.groupValues[1]
            // Skip if the match content is empty or overlaps an existing match
            if (content.isEmpty()) continue
            val s = m.range.first + 1  // position after opening delimiter
            val e = s + content.length // position before closing delimiter
            // Check for overlap with existing matches
            if (inlineMatches.none { it.start < e && it.end > s }) {
                inlineMatches.add(Match(s, e, style))
            }
        }
    }

    // Build the string, merging link and inline styles
    val allMatches = (linkMatches + inlineMatches).sortedBy { it.start }

    var pos = 0
    val stripped = linkStripped.toString()
    while (pos < stripped.length) {
        // Find the next match starting at or after pos
        val next = allMatches.firstOrNull { it.start >= pos }
        if (next == null) {
            // No more styles — append remaining text
            append(stripped.substring(pos))
            break
        }
        // Append plain text before this match
        if (next.start > pos) {
            append(stripped.substring(pos, next.start))
        }
        // Append styled text
        val styledText = stripped.substring(next.start, next.end)
        // Merge link style with inline style
        val linkStyle = linkMatches.firstOrNull { it.start == next.start && it.end == next.end }
        val mergedStyle = if (linkStyle != null) {
            next.style.merge(linkStyle.style)
        } else {
            next.style
        }
        // Add URL annotation if it's a link
        if (linkStyle?.url != null) {
            pushStringAnnotation(tag = "URL", annotation = linkStyle.url)
        }
        withStyle(mergedStyle) {
            append(styledText)
        }
        if (linkStyle?.url != null) {
            pop()
        }
        pos = next.end
    }
}

// ── Table rendering ──────────────────────────────────────────────

@Composable
fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val colCount = headers.size

    Column(
        modifier = modifier
            .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
    ) {
        // Header row
        Row {
            headers.forEachIndexed { index, header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = header.trim(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Data rows
        rows.forEachIndexed { rowIdx, row ->
            if (rowIdx > 0 || colCount > 0) {
                HorizontalDivider(color = borderColor, thickness = 0.5.dp)
            }
            Row {
                row.forEachIndexed { cellIdx, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cell.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ── Segment types ────────────────────────────────────────────────

private sealed class MarkdownSegment {
    data class Text(val content: String) : MarkdownSegment()
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownSegment()
    data object Divider : MarkdownSegment()
    data class Heading(val level: Int, val content: String) : MarkdownSegment()
    data class UnorderedListItem(val text: String) : MarkdownSegment()
    data class OrderedListItem(val number: Int, val text: String) : MarkdownSegment()
    data class TaskListItem(val text: String, val checked: Boolean) : MarkdownSegment()
    data class Image(val alt: String, val url: String) : MarkdownSegment()
    data class Blockquote(val content: String) : MarkdownSegment()
}

// ── Parser ───────────────────────────────────────────────────────

private fun parseSegments(text: String): List<MarkdownSegment> {
    val lines = text.split("\n")
    val segments = mutableListOf<MarkdownSegment>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim().ifEmpty { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            segments.add(MarkdownSegment.CodeBlock(codeLines.joinToString("\n"), language))
            continue
        }

        // Divider: --- *** ___
        if (line.trim().matches(DIVIDER_REGEX)) {
            segments.add(MarkdownSegment.Divider)
            i++
            continue
        }

        // Image: ![alt](url)
        val imageMatch = IMAGE_REGEX.matchEntire(line.trim())
        if (imageMatch != null) {
            segments.add(MarkdownSegment.Image(imageMatch.groupValues[1], imageMatch.groupValues[2]))
            i++
            continue
        }

        // Heading: # through ######
        val headingMatch = HEADING_REGEX.matchEntire(line.trim())
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val content = headingMatch.groupValues[2]
            segments.add(MarkdownSegment.Heading(level, content))
            i++
            continue
        }

        // Task list: - [ ] text or - [x] text
        val taskMatch = TASK_REGEX.matchEntire(line)
        if (taskMatch != null) {
            segments.add(MarkdownSegment.TaskListItem(taskMatch.groupValues[2], taskMatch.groupValues[1] == "x"))
            i++
            continue
        }

        // Unordered list item: - or * followed by space (but not task list)
        val ulMatch = UL_REGEX.matchEntire(line)
        if (ulMatch != null) {
            segments.add(MarkdownSegment.UnorderedListItem(ulMatch.groupValues[1]))
            i++
            continue
        }

        // Ordered list item: number. followed by space
        val olMatch = OL_REGEX.matchEntire(line)
        if (olMatch != null) {
            segments.add(MarkdownSegment.OrderedListItem(olMatch.groupValues[1].toInt(), olMatch.groupValues[2]))
            i++
            continue
        }

        // Blockquote: > ... (support multi-line accumulation)
        if (line.trimStart().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                val content = lines[i].trimStart().removePrefix(">").trimStart()
                quoteLines.add(content)
                i++
            }
            segments.add(MarkdownSegment.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // Table detection: header line with | followed by separator line
        if (i + 1 < lines.size && line.contains("|") && isTableSeparator(lines[i + 1])) {
            val headers = parseTableRow(line)
            i += 2
            val dataRows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].contains("|") && !lines[i].trimStart().startsWith("```")) {
                val cells = parseTableRow(lines[i])
                if (cells.size == headers.size) {
                    dataRows.add(cells)
                }
                i++
            }
            segments.add(MarkdownSegment.Table(headers, dataRows))
            continue
        }

        // Plain text: accumulate until next special pattern
        val textLines = mutableListOf<String>()
        while (i < lines.size) {
            val l = lines[i]
            if (l.trimStart().startsWith("```") ||
                l.trim().matches(DIVIDER_REGEX) ||
                HEADING_REGEX.matches(l.trim()) ||
                UL_REGEX.matches(l) ||
                OL_REGEX.matches(l) ||
                l.trimStart().startsWith(">")
            ) {
                break
            }
            if (i + 1 < lines.size && l.contains("|") && isTableSeparator(lines[i + 1])) {
                break
            }
            textLines.add(l)
            i++
        }
        val combined = textLines.joinToString("\n").trim()
        if (combined.isNotEmpty()) {
            segments.add(MarkdownSegment.Text(combined))
        }
    }

    return if (segments.isEmpty()) listOf(MarkdownSegment.Text(text.trim())) else segments
}

private val DIVIDER_REGEX = Regex("^-{3,}$")
private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.+)""")
private val TASK_REGEX = Regex("""^[-*]\s+\[([ xX])\]\s+(.+)""")
private val UL_REGEX = Regex("""^[-*]\s+(.+)""")
private val OL_REGEX = Regex("""^(\d+)\.\s+(.+)""")
private val IMAGE_REGEX = Regex("""!\[(.+?)]\((\S+)\)""")

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains("|") &&
           trimmed.contains("-") &&
           trimmed.matches(Regex("""\|[\s\-:|]+\|"""))
}

private fun parseTableRow(line: String): List<String> {
    return line.trim('|').split("|").map { it.trim() }
}
