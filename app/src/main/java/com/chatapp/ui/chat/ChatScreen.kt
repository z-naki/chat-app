package com.chatapp.ui.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.chatapp.ui.animation.smoothExpandVertically
import com.chatapp.ui.animation.smoothFadeIn
import com.chatapp.ui.animation.smoothFadeOut
import com.chatapp.ui.animation.smoothShrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.domain.model.Attachment
import com.chatapp.domain.model.AttachmentType
import com.chatapp.ui.theme.LocalStrings
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.ProviderType
import com.chatapp.ui.components.ChatMessage
import com.chatapp.ui.components.InputBar
import com.chatapp.ui.components.StreamingMessage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onConversationCreated: ((Long) -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val providerName = uiState.activeProvider.displayName
    val modelName = uiState.currentModel
    val s = LocalStrings.current
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleAttachment(it, context, viewModel) }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleAttachment(it, context, viewModel) }
    }

    LaunchedEffect(onConversationCreated) {
        viewModel.setOnConversationCreated(onConversationCreated)
    }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    // Initial scroll to bottom when messages load for existing conversation
    LaunchedEffect(conversationId, uiState.messages.lastOrNull()?.id) {
        if (conversationId > 0 && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    // Auto-scroll only during streaming + user near bottom. Debounced to avoid per-token jank.
    LaunchedEffect(uiState.isStreaming) {
        if (uiState.isStreaming) {
            snapshotFlow {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                lastVisible to total
            }.collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 2) {
                    listState.animateScrollToItem(total - 1)
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {},
        bottomBar = {
            val focusManager = LocalFocusManager.current
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pending attachments
                AnimatedVisibility(
                    visible = uiState.pendingAttachments.isNotEmpty(),
                    enter = smoothExpandVertically() + smoothFadeIn(),
                    exit = smoothShrinkVertically() + smoothFadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.pendingAttachments.forEach { att ->
                            AttachmentChip(
                                attachment = att,
                                onRemove = { viewModel.removeAttachment(att.id) }
                            )
                        }
                    }
                }
                // Info bar: provider - model (aligned with input text start)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Provider selector
                    var showProviderPicker by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            text = uiState.activeProvider.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { showProviderPicker = true }
                        )
                        DropdownMenu(
                            expanded = showProviderPicker,
                            onDismissRequest = { showProviderPicker = false }
                        ) {
                            ProviderType.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = provider.displayName,
                                            fontWeight = if (provider == uiState.activeProvider) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectProvider(provider)
                                        showProviderPicker = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleModelPicker() }
                        ) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh models",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp).padding(start = 2.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = uiState.showModelPicker,
                            onDismissRequest = { viewModel.toggleModelPicker() }
                        ) {
                            val models = uiState.availableModels.ifEmpty { listOf(modelName) }
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = model,
                                            fontWeight = if (model == modelName) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { viewModel.selectModel(model) }
                                )
                            }
                        }
                    }
                }
                InputBar(
                    value = uiState.inputText,
                    onValueChange = { viewModel.onInputChange(it) },
                    onSend = { viewModel.sendMessage(); focusManager.clearFocus() },
                    onStop = { viewModel.stopGeneration() },
                    isStreaming = uiState.isStreaming,
                    isFocused = uiState.inputText.isNotEmpty(),
                    placeholder = s.message,
                    onAttachImage = { imagePicker.launch("image/*") },
                    onAttachFile = { filePicker.launch("*/*") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                ChatMessage(
                    message = message,
                    providerName = providerName,
                    onRegenerate = if (message.role == MessageRole.ASSISTANT) {
                        { viewModel.regenerateMessage(message.id) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            if (uiState.isStreaming && (uiState.streamingOutput.isNotEmpty() || uiState.streamingThinking.isNotEmpty())) {
                item(key = "streaming") {
                    StreamingMessage(
                        output = uiState.streamingOutput,
                        thinking = uiState.streamingThinking,
                        isThinkingCollapsed = uiState.isThinkingCollapsed,
                        thinkingTokenCount = uiState.thinkingTokenCount,
                        outputTokenCount = uiState.outputTokenCount,
                        onToggleThinking = { viewModel.toggleThinkingCollapse() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

}

private fun handleAttachment(uri: Uri, context: android.content.Context, viewModel: ChatViewModel) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "image/*"
    val name = try {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else "attachment"
            } else "attachment"
        } ?: "attachment"
    } catch (e: Exception) {
        "attachment"
    }

    val bytes = try {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.use { stream -> stream.readBytes() } ?: ByteArray(0)
    } catch (e: Exception) {
        ByteArray(0)
    }

    if (bytes.isNotEmpty()) {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val att = Attachment(
            id = UUID.randomUUID().toString(),
            type = if (mimeType.startsWith("image/")) AttachmentType.IMAGE else AttachmentType.FILE,
            name = name,
            mimeType = mimeType,
            dataBase64 = base64,
            localPath = uri.toString()
        )
        viewModel.addAttachment(att)
        // For text files, also attach content as a text preview
        if (mimeType.startsWith("text/") || mimeType in listOf("application/json", "application/xml")) {
            val textContent = String(bytes, Charsets.UTF_8).take(2000)
            if (textContent.isNotBlank()) {
                viewModel.onInputChange(textContent)
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: Attachment,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = attachment.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
