package com.chatapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.ui.theme.LocalStrings
import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.ProviderType
import com.chatapp.ui.chat.ChatScreen
import com.chatapp.ui.chat.ChatViewModel
import com.chatapp.ui.conversationlist.ConversationListViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversationId: Long,
    onConversationClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    convListViewModel: ConversationListViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val convUiState by convListViewModel.uiState.collectAsStateWithLifecycle()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }
    val temp = chatUiState.temperature
    val maxTokens = chatUiState.maxTokensUi
    val contextRounds = chatUiState.contextRounds
    val multimodalEnabled = chatUiState.conversation?.multimodalEnabled ?: chatUiState.multimodalEnabled
    val topP = chatUiState.topP
    val totalTokens = chatUiState.messages.sumOf { (it.content.length / 2.5).toLong() + (it.thinking?.length?.div(2.5)?.toLong() ?: 0L) }
    val tokenDisplay = when {
        totalTokens >= 1_000_000 -> "${"%.1f".format(totalTokens / 1_000_000.0)}M"
        totalTokens >= 1_000 -> "${"%.1f".format(totalTokens / 1_000.0)}k"
        else -> "$totalTokens"
    }

    val grouped = groupByDay(convUiState.conversations)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = s.history,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (convUiState.conversations.isEmpty()) {
                        Text(
                            text = s.noConversations,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            grouped.forEach { group ->
                                item(key = group.dateLabel) {
                                    Text(
                                        text = group.dateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(
                                    items = group.conversations,
                                    key = { it.id }
                                ) { conversation ->
                                    var showMenu by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onConversationClick(conversation.id)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = conversation.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = convListViewModel.getProviderDisplayName(conversation.provider),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                offset = androidx.compose.ui.unit.DpOffset(x = (-40).dp, y = 0.dp)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(s.delete) },
                                                    onClick = {
                                                        showMenu = false
                                                        deleteConfirmId = conversation.id
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSettingsClick()
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.settings,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (totalTokens > 0) "$tokenDisplay ${s.tokens}" else "Chat AI",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = s.history
                            )
                        }
                    },
                    actions = {
                        val searchSupported = chatViewModel.supportsSearch()
                        IconButton(
                            onClick = { if (searchSupported) chatViewModel.toggleSearch() },
                            modifier = Modifier.size(32.dp),
                            enabled = searchSupported
                        ) {
                            Icon(
                                imageVector = if (chatUiState.enableSearch) Icons.Filled.Search else Icons.Filled.SearchOff,
                                contentDescription = s.search,
                                tint = when {
                                    !searchSupported -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    chatUiState.enableSearch -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onConversationClick(-1L) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = s.newConversation,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    // Provider selector
                                    Text(s.provider, style = MaterialTheme.typography.labelSmall)
                                    Column(
                                        modifier = Modifier.heightIn(max = 120.dp).width(180.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        val configuredProviders = chatViewModel.getConfiguredProviders()
                                        configuredProviders.forEach { provider ->
                                            Text(
                                                text = chatViewModel.getProviderDisplayName(provider), style = MaterialTheme.typography.bodySmall,
                                                color = if (provider == chatUiState.activeProvider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(vertical = 2.dp).clickable { chatViewModel.selectProvider(provider) }
                                            )
                                        }
                                        if (configuredProviders.isEmpty()) {
                                            Text(s.noConversations, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${s.temperature}: ${"%.2f".format(temp)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Slider(value = temp, onValueChange = { chatViewModel.updateTemperature(it) }, valueRange = 0f..2f, modifier = Modifier.width(180.dp).height(32.dp))
                                    Text("${s.topP}: ${"%.2f".format(topP)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Slider(value = topP, onValueChange = { chatViewModel.updateTopP(it) }, valueRange = 0f..1f, modifier = Modifier.width(180.dp).height(32.dp))
                                    OutlinedTextField(
                                        value = contextRounds.toString(), onValueChange = { v -> v.toIntOrNull()?.let { chatViewModel.updateContextRounds(it) } },
                                        label = { Text(s.contextRounds) }, singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(180.dp).height(52.dp)
                                    )
                                    OutlinedTextField(
                                        value = maxTokens.toString(), onValueChange = { v -> v.toIntOrNull()?.let { chatViewModel.updateMaxTokens(it) } },
                                        label = { Text(s.maxTokens) }, singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(180.dp).height(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = s.multimodal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Switch(
                                            checked = multimodalEnabled,
                                            onCheckedChange = { chatViewModel.toggleMultimodal() },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            key(conversationId) {
                ChatScreen(
                    conversationId = conversationId,
                    onBack = { onConversationClick(-1L) },
                    onConversationCreated = { newId ->
                        onConversationClick(newId)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("${s.delete} Conversation") },
            text = { Text(if (LocalStrings.current == com.chatapp.ui.theme.ZH) "确定删除？所有消息将被移除。" else "Delete this conversation? All messages will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    convListViewModel.deleteConversation(id)
                    deleteConfirmId = null
                }) {
                    Text(s.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text(s.cancel)
                }
            }
        )
    }
}

private data class DayGroup(
    val dateLabel: String,
    val conversations: List<Conversation>,
    val sortKey: Int = 0
)

private fun groupByDay(conversations: List<Conversation>): List<DayGroup> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = fmt.format(Date())
    val yesterday = fmt.format(Date(System.currentTimeMillis() - 86_400_000))

    return conversations
        .groupBy { fmt.format(Date(it.createdAt)) }
        .map { (date, convs) ->
            val (sortKey, label) = when (date) {
                today -> 0 to "Today"
                yesterday -> 1 to "Yesterday"
                else -> 2 to date
            }
            DayGroup(label, convs.sortedByDescending { it.createdAt }, sortKey)
        }
        .sortedBy { it.sortKey }
        .map { it.copy(sortKey = 0) }
}

