package com.chatapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.domain.model.Conversation
import com.chatapp.ui.chat.ChatScreen
import com.chatapp.ui.conversationlist.ConversationListViewModel
import com.chatapp.util.DebugLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConversationClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    convListViewModel: ConversationListViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val convUiState by convListViewModel.uiState.collectAsStateWithLifecycle()
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    var showDebugPanel by remember { mutableStateOf(false) }
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
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (convUiState.conversations.isEmpty()) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
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
                                                text = conversation.provider.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
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
                                                text = { Text("Delete") },
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

                    Spacer(modifier = Modifier.weight(1f))

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
                            text = "Settings",
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
                            text = "Chat AI v0.0.11-a",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open history"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDebugPanel = !showDebugPanel }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Debug",
                                tint = if (showDebugPanel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (showDebugPanel) {
                DebugPanelContent()
            }
            ChatScreen(
                conversationId = -1L,
                onBack = {},
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Delete confirmation dialog
    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? All messages will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    convListViewModel.deleteConversation(id)
                    deleteConfirmId = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Cancel")
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

@Composable
private fun DebugPanelContent() {
    val entries by DebugLog.entries.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(8.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries.reversed()) { entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
