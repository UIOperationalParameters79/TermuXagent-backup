package com.termuxagent.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termuxagent.ui.theme.MonoTextStyle

/**
 * Minimal, dependency-free Markdown renderer. Handles the subset an LLM agent
 * actually emits in its final answers: paragraphs, headings, fenced code
 * blocks, inline code, bold, italics, strikethrough, bullet / numbered lists,
 * nested quotes, horizontal rules, and GitHub-flavored tables.
 *
 * Intentionally not a full CommonMark parser — keeping it tight means zero
 * dependency risk and predictable rendering.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseBlocks(markdown)
    Column(modifier = modifier.fillMaxWidth()) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = renderInline(block.text),
                        style = style.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                is MdBlock.Paragraph -> {
                    Text(
                        text = renderInline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MdBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = block.code,
                                style = MonoTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
                is MdBlock.ListItem -> {
                    val prefix = if (block.ordered) "${block.n}." else "•"
                    val indent = "  ".repeat(block.depth.coerceAtMost(4))
                    Text(
                        text = renderInline("$indent$prefix ${block.text}"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                is MdBlock.Quote -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = renderInline(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                is MdBlock.HRule -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) { /* hairline */ }
                }
                is MdBlock.Table -> {
                    MdTable(block)
                }
            }
        }
    }
}

@Composable
private fun MdTable(table: MdBlock.Table) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val headerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth().background(headerBg)) {
            table.headers.forEachIndexed { idx, h ->
                val align = table.alignments.getOrNull(idx) ?: TableAlign.LEFT
                Text(
                    text = renderInline(h),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    textAlign = when (align) {
                        TableAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
                        TableAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                        TableAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                    }
                )
            }
        }
        // Data rows
        table.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { idx, cell ->
                    val align = table.alignments.getOrNull(idx) ?: TableAlign.LEFT
                    Text(
                        text = renderInline(cell),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        textAlign = when (align) {
                            TableAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
                            TableAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                            TableAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                        }
                    )
                }
            }
        }
        // Bottom border
        Box(modifier = Modifier.fillMaxWidth().width(0.dp).background(borderColor)) { }
    }
}

// ── Parser ───────────────────────────────────────────────────────────────────

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Code(val language: String, val code: String) : MdBlock()
    data class ListItem(val text: String, val ordered: Boolean, val n: Int, val depth: Int) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    object HRule : MdBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableAlign>
    ) : MdBlock()
}

private enum class TableAlign { LEFT, CENTER, RIGHT }

private fun parseBlocks(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val lines = src.lines()
    var i = 0
    val paragraphBuf = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuf.isNotBlank()) {
            out.add(MdBlock.Paragraph(paragraphBuf.toString().trim()))
            paragraphBuf.setLength(0)
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        // Fenced code block
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            val lang = line.trimStart().removePrefix("```").trim()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.appendLine(lines[i])
                i++
            }
            i++ // skip closing ```
            out.add(MdBlock.Code(language = lang, code = code.toString().trimEnd()))
            continue
        }
        // Horizontal rule
        if (line.matches(Regex("^\\s*([-*_])\\1{2,}\\s*$"))) {
            flushParagraph()
            out.add(MdBlock.HRule)
            i++
            continue
        }
        // Heading
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            out.add(MdBlock.Heading(level, text))
            i++
            continue
        }
        // Block quote — accumulate consecutive `> ...` lines into one Quote block.
        if (Regex("^>\\s?.*$").matches(line)) {
            flushParagraph()
            val quoteBuf = StringBuilder()
            while (i < lines.size && Regex("^>\\s?.*$").matches(lines[i])) {
                val part = Regex("^>\\s?(.*)$").matchEntire(lines[i])!!.groupValues[1]
                if (quoteBuf.isNotEmpty()) quoteBuf.append(' ')
                quoteBuf.append(part.trim())
                i++
            }
            out.add(MdBlock.Quote(quoteBuf.toString()))
            continue
        }
        // Table — needs header row, then a separator row of |---|---|.
        if (line.contains('|')) {
            val sepLine = lines.getOrNull(i + 1) ?: ""
            val sepMatch = Regex("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?\\s*$").matchEntire(sepLine)
            if (sepMatch != null) {
                flushParagraph()
                val headers = splitRow(line)
                val alignments = splitRow(sepLine).map { alignOf(it) }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows.add(splitRow(lines[i]))
                    i++
                }
                out.add(MdBlock.Table(headers, rows, alignments))
                continue
            }
        }
        // Numbered list (allow leading spaces for nesting)
        val numMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.+)$").matchEntire(line)
        if (numMatch != null) {
            flushParagraph()
            val depth = numMatch.groupValues[1].length / 2
            val n = numMatch.groupValues[2].toIntOrNull() ?: 1
            out.add(MdBlock.ListItem(numMatch.groupValues[3], ordered = true, n = n, depth = depth))
            i++
            continue
        }
        // Bullet list (allow leading spaces for nesting)
        val bulletMatch = Regex("^(\\s*)[-*+]\\s+(.+)$").matchEntire(line)
        if (bulletMatch != null) {
            flushParagraph()
            val depth = bulletMatch.groupValues[1].length / 2
            out.add(MdBlock.ListItem(bulletMatch.groupValues[2], ordered = false, n = 0, depth = depth))
            i++
            continue
        }
        // Blank line: paragraph break
        if (line.isBlank()) {
            flushParagraph()
            i++
            continue
        }
        // Default: append to paragraph buffer
        if (paragraphBuf.isNotEmpty()) paragraphBuf.append(' ')
        paragraphBuf.append(line.trim())
        i++
    }
    flushParagraph()
    return out
}

/** Split a markdown table row into cells. Handles leading/trailing pipes. */
private fun splitRow(line: String): List<String> {
    val trimmed = line.trim().trim('|')
    return trimmed.split("\\|").map { it.trim() }.map { unescapePipes(it) }
}

private fun unescapePipes(s: String): String = s.replace("\\|", "|")

private fun alignOf(spec: String): TableAlign {
    val s = spec.trim()
    val leftColon = s.startsWith(':')
    val rightColon = s.endsWith(':')
    return when {
        leftColon && rightColon -> TableAlign.CENTER
        rightColon -> TableAlign.RIGHT
        else -> TableAlign.LEFT
    }
}

// ── Inline rendering (bold, italic, strikethrough, code, links) ──────────────

/**
 * Inline markdown renderer. Uses a token-based scan instead of a per-char
 * state machine, so we correctly handle `**bold**`, `*italic*`, `***both***`,
 * `` `code` ``, `~~strike~~`, and `[text](url)` without accidentally
 * consuming the inner `*` of `**bold**` as italic.
 *
 * Unknown / malformed markup is emitted verbatim — the user always sees the
 * raw text the model produced, never a swallowed token.
 */
private fun renderInline(src: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = src
    while (i < s.length) {
        // Inline code `...`
        if (s[i] == '`') {
            // Allow triple backtick inline `` `like this` `` if it appears.
            val backticks = countBackticks(s, i)
            val closer = findBacktickRun(s, i + backticks, backticks)
            if (closer > i) {
                val inner = s.substring(i + backticks, closer)
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    background = androidx.compose.ui.graphics.Color(0x22808080)
                )) {
                    append(inner)
                }
                i = closer + backticks
                continue
            }
        }
        // Bold ***...*** (bold + italic)
        if (s.startsWith("***", i)) {
            val end = s.indexOf("***", i + 3)
            if (end > i + 3) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append(s.substring(i + 3, end))
                }
                i = end + 3
                continue
            }
        }
        // Bold **...**
        if (s.startsWith("**", i)) {
            val end = s.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(s.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // Strikethrough ~~...~~
        if (s.startsWith("~~", i)) {
            val end = s.indexOf("~~", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                    append(s.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // Italic *...* or _..._
        if (s[i] == '*' || s[i] == '_') {
            val marker = s[i]
            // Skip if it's actually part of ** or *** (handled above).
            if (i + 1 < s.length && s[i + 1] == marker) {
                // Already handled — fall through to literal append.
            } else {
                val end = findMatchingItalic(s, i + 1, marker)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
        }
        // Link [text](url)
        if (s[i] == '[') {
            val closeText = s.indexOf(']', i + 1)
            if (closeText > i && closeText + 1 < s.length && s[closeText + 1] == '(') {
                val closeUrl = s.indexOf(')', closeText + 2)
                if (closeUrl > closeText) {
                    val text = s.substring(i + 1, closeText)
                    val url = s.substring(closeText + 2, closeUrl)
                    // Render link text in a slightly emphasized style; tappable
                    // links are out of scope for this minimal renderer.
                    withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF1A73E8))) {
                        append(text)
                    }
                    i = closeUrl + 1
                    continue
                }
            }
        }
        // Backslash escape for the next char (e.g. \* not a marker)
        if (s[i] == '\\' && i + 1 < s.length) {
            append(s[i + 1])
            i += 2
            continue
        }
        append(s[i])
        i++
    }
}

private fun countBackticks(s: String, start: Int): Int {
    var n = 0
    var i = start
    while (i < s.length && s[i] == '`') { n++; i++ }
    return n
}

private fun findBacktickRun(s: String, start: Int, len: Int): Int {
    // Find a run of `len` backticks starting at or after `start`.
    if (len <= 0) return -1
    var i = start
    while (i + len <= s.length) {
        var match = true
        for (k in 0 until len) {
            if (s[i + k] != '`') { match = false; break }
        }
        if (match) return i
        i++
    }
    return -1
}

/** Find the matching closing italic marker, ignoring escaped ones. */
private fun findMatchingItalic(s: String, start: Int, marker: Char): Int {
    var i = start
    while (i < s.length) {
        if (s[i] == '\\' && i + 1 < s.length) { i += 2; continue }
        if (s[i] == marker) {
            // Don't match if it's actually the start of ** (bold).
            if (i + 1 < s.length && s[i + 1] == marker) return -1
            return i
        }
        i++
    }
    return -1
}
