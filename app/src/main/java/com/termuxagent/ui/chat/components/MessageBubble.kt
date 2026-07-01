package com.termuxagent.ui.chat.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.termuxagent.data.chat.AssistantBlock
import com.termuxagent.data.chat.UiMessage
import com.termuxagent.util.MarkdownText

@Composable
fun MessageBubble(message: UiMessage, modifier: Modifier = Modifier) {
    when (message) {
        is UiMessage.User -> UserBubble(message, modifier)
        is UiMessage.Assistant -> AssistantBubble(message, modifier)
    }
}

@Composable
private fun UserBubble(msg: UiMessage.User, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AssistantBubble(msg: UiMessage.Assistant, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.widthIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (msg.blocks.isEmpty() && msg.isStreaming) {
                    StreamingDots()
                }
                for (block in msg.blocks) {
                    when (block) {
                        is AssistantBlock.Text -> {
                            if (block.text.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    MarkdownText(markdown = block.text)
                                }
                            }
                        }
                        is AssistantBlock.ToolCall -> {
                            ToolCallCard(block = block)
                        }
                    }
                }
                if (msg.isStreaming && msg.blocks.isNotEmpty()) {
                    val lastText = msg.blocks.lastOrNull() as? AssistantBlock.Text
                    if (lastText?.isStreaming == true) {
                        // Show a thin blinking caret — handled implicitly via "▍" append by stream.
                    }
                }
                if (msg.error != null) {
                    Text(
                        text = msg.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingDots() {
    // Simple "thinking" indicator: three dots in a small pill.
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "• • •",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
