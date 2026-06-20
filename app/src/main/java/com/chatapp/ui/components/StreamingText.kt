package com.chatapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chatapp.ui.theme.LocalStrings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingMessage(
    output: String,
    thinking: String = "",
    isThinkingCollapsed: Boolean = false,
    thinkingTokenCount: Long = 0,
    outputTokenCount: Long = 0,
    onToggleThinking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showThinking = thinking.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (showThinking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LocalStrings.current.thought,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTokenCount(thinkingTokenCount + outputTokenCount) + " " + LocalStrings.current.tokens,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Icon(
                    imageVector = if (isThinkingCollapsed) {
                        Icons.Default.ArrowDropDown
                    } else {
                        Icons.Default.ArrowDropUp
                    },
                    contentDescription = if (isThinkingCollapsed) {
                        "Expand thinking"
                    } else {
                        "Collapse thinking"
                    },
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable { onToggleThinking() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )

            if (!isThinkingCollapsed) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = thinking,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        MarkdownContent(
            text = output,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatTokenCount(tokens: Long): String {
    if (tokens >= 1000) {
        val whole = tokens / 1000
        val frac = ((tokens % 1000) / 100)
        return "$whole.${frac}k"
    }
    return "$tokens"
}
