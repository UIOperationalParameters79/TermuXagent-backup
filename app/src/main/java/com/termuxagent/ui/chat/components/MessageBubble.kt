package com.termuxagent.ui.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.termuxagent.data.chat.AssistantBlock
import com.termuxagent.data.chat.UiMessage
import com.termuxagent.util.MarkdownText

@Composable
fun MessageBubble(message: UiMessage, modifier: Modifier = Modifier) {
    when (message) {
        is UiMessage.User -> UserRow(message, modifier)
        is UiMessage.Assistant -> AssistantRow(message, modifier)
    }
}

@Composable
private fun UserRow(msg: UiMessage.User, modifier: Modifier) {
    val context = LocalContext.current
    // Modern look: right-aligned text inside a subtle surface, no harsh bubble tail.
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        // Compact copy icon for the user's own message — handy when you want
        // to re-use a prompt elsewhere.
        IconButton(
            onClick = { copyToClipboard(context, msg.text, "Message copied") },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.size(4.dp))
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = msg.text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AssistantRow(msg: UiMessage.Assistant, modifier: Modifier) {
    val context = LocalContext.current
    var showActions by remember(msg.id) { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Small avatar dot — monochrome.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(
                modifier = Modifier.widthIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (msg.blocks.isEmpty() && msg.isStreaming) {
                    ThinkingIndicator()
                }
                for (block in msg.blocks) {
                    when (block) {
                        is AssistantBlock.Text -> {
                            if (block.text.isNotBlank()) {
                                // Render the assistant's text as Markdown so
                                // **bold**, lists, `code`, tables, etc. show
                                // properly instead of as raw markup.
                                if (block.isStreaming) {
                                    // While streaming we render the partial
                                    // markdown plus a caret, so the user can
                                    // see text appear live.
                                    Column {
                                        MarkdownText(markdown = block.text)
                                        Text(
                                            text = "▋",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                } else {
                                    MarkdownText(
                                        markdown = block.text,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        is AssistantBlock.ToolCall -> {
                            ToolCallCard(block = block)
                        }
                    }
                }
                if (msg.error != null) {
                    Text(
                        text = msg.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Copy-action row for the whole assistant message. Only show
                // once the message has finished streaming — no point copying
                // a half-finished answer.
                if (!msg.isStreaming && msg.blocks.any { it is AssistantBlock.Text && it.text.isNotBlank() }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val plainText = remember(msg.id, msg.blocks) {
                            msg.blocks
                                .filterIsInstance<AssistantBlock.Text>()
                                .joinToString("\n\n") { it.text }
                                .trim()
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(context, plainText, "Answer copied")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy answer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Copy [text] to the system clipboard and toast a confirmation. */
private fun copyToClipboard(context: Context, text: String, toastMsg: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("TermuXagent", text))
    // Don't toast on Android 13+ — the system shows its own chip.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ThinkingIndicator() {
    // Three pulsing dots — pure monochrome.
    val transition = rememberInfiniteTransition(label = "thinking")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingAlpha"
    )
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    .alpha(alpha * (1f - i * 0.2f))
            )
        }
    }
}
