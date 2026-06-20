package com.chatapp.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatapp.domain.model.Attachment
import com.chatapp.domain.model.AttachmentType
import com.chatapp.ui.theme.LocalStrings
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole

@Composable
fun ChatMessage(
    message: Message,
    modifier: Modifier = Modifier,
    providerName: String = "Assistant",
    onRegenerate: (() -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    val s = LocalStrings.current
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = if (isUser) s.you else providerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Standard pattern: Row with Arrangement.End pushes user content right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // Thinking (assistant only)
                if (!isUser && !message.thinking.isNullOrBlank()) {
                    var thinkingExpanded by remember { mutableStateOf(true) }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { thinkingExpanded = !thinkingExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.thought, style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.5f))
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatTokenCount((message.thinking?.length?.div(2.5)?.toLong() ?: 0L) +
                                (message.content.length / 2.5).toLong()) + " " + s.tokens,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.5f))
                        Icon(
                            if (thinkingExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            if (thinkingExpanded) "Collapse" else "Expand",
                            tint = contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                    if (thinkingExpanded) {
                        Spacer(Modifier.height(4.dp))
                        Text(message.thinking, style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(6.dp))
                }

                // Content
                MarkdownContent(
                    text = message.content,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isUser) TextAlign.End else null
                )

                // Attachments
                if (message.attachments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    message.attachments.forEach { AttachmentThumbnail(it) }
                }

                // Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton({ clipboardManager.setText(AnnotatedString(message.content)) },
                        Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy",
                            tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                    if (!isUser && onRegenerate != null) {
                        IconButton(onRegenerate, Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, "Regenerate",
                                tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentThumbnail(attachment: Attachment) {
    when (attachment.type) {
        AttachmentType.IMAGE -> {
            if (attachment.dataBase64.isNotBlank()) {
                val bytes = remember(attachment.dataBase64) { Base64.decode(attachment.dataBase64, Base64.DEFAULT) }
                val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                bitmap?.let { bm ->
                    Image(
                        bitmap = bm.asImageBitmap(), contentDescription = attachment.name,
                        modifier = Modifier
                            .fillMaxWidth().padding(vertical = 4.dp).height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
        AttachmentType.FILE -> {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(attachment.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun formatTokenCount(tokens: Long): String {
    if (tokens >= 1000) { val w = tokens / 1000; val f = (tokens % 1000) / 100; return "$w.${f}k" }
    return "$tokens"
}
