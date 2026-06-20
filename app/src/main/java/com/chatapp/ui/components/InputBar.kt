package com.chatapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.chatapp.ui.animation.smoothExpandVertically
import com.chatapp.ui.animation.smoothFadeIn
import com.chatapp.ui.animation.smoothFadeOut
import com.chatapp.ui.animation.smoothShrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean,
    isFocused: Boolean,
    placeholder: String = "Message",
    modifier: Modifier = Modifier,
    onAttachImage: (() -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null
) {
    val active = isFocused || value.isNotEmpty() || isStreaming
    val hasAttach = onAttachImage != null || onAttachFile != null
    var showAttachMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Expandable attachment menu
        if (hasAttach) {
            AnimatedVisibility(
                visible = showAttachMenu,
                enter = smoothExpandVertically() + smoothFadeIn(),
                exit = smoothShrinkVertically() + smoothFadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onAttachImage != null) {
                        IconButton(
                            onClick = {
                                showAttachMenu = false
                                onAttachImage()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (onAttachFile != null) {
                        IconButton(
                            onClick = {
                                showAttachMenu = false
                                onAttachFile()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Add file",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            // "+" button — right side of input, expands attachment menu
            if (hasAttach && !isStreaming) {
                IconButton(
                    onClick = { showAttachMenu = !showAttachMenu },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (showAttachMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showAttachMenu) "Close menu" else "Add attachment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = active,
                enter = smoothFadeIn(),
                exit = smoothFadeOut()
            ) {
                IconButton(
                    onClick = if (isStreaming) onStop else onSend,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Close
                        else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isStreaming) "Stop" else "Send",
                        tint = if (isStreaming || value.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
