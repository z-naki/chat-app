package com.chatapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // --- Appearance ---
            SectionHeader("Appearance")
            ThemeSelector(
                currentMode = uiState.themeMode,
                onSelect = { viewModel.setThemeMode(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- API Key Management ---
            SectionHeader("API Key / Token")

            // DeepSeek always visible
            ApiKeyRow(
                provider = ProviderType.DEEPSEEK,
                hasKey = uiState.apiKeys[ProviderType.DEEPSEEK] ?: false,
                isVisible = uiState.showKey[ProviderType.DEEPSEEK] ?: false,
                onEdit = { viewModel.startEditApiKey(ProviderType.DEEPSEEK) },
                onToggleVisibility = { viewModel.toggleKeyVisibility(ProviderType.DEEPSEEK) }
            )

            val otherProviders = ProviderType.entries.filter { it != ProviderType.DEEPSEEK }
            var showExtraProviders by remember { mutableStateOf(false) }

            if (!showExtraProviders) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExtraProviders = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Provider",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Add Provider",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                otherProviders.forEach { provider ->
                    ApiKeyRow(
                        provider = provider,
                        hasKey = uiState.apiKeys[provider] ?: false,
                        isVisible = uiState.showKey[provider] ?: false,
                        onEdit = { viewModel.startEditApiKey(provider) },
                        onToggleVisibility = { viewModel.toggleKeyVisibility(provider) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- Network ---
            SectionHeader("Network")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proxy",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.proxyEnabled,
                    onCheckedChange = { viewModel.setProxyEnabled(it) }
                )
            }

            if (uiState.proxyEnabled) {
                OutlinedTextField(
                    value = uiState.proxyAddress,
                    onValueChange = { viewModel.setProxyAddress(it) },
                    label = { Text("Proxy Address") },
                    placeholder = { Text("127.0.0.1:7890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- About ---
            SectionHeader("About")
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- API Key Edit Dialog ---
    if (uiState.editingProvider != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("${uiState.editingProvider?.displayName ?: ""} API Key") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.editingKeyValue,
                        onValueChange = { viewModel.onEditKeyValueChange(it) },
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = if (uiState.showKey[uiState.editingProvider] == true) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.showProxyWarning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This provider may not be accessible without a proxy. Proxy is currently disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveApiKey() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ThemeSelector(
    currentMode: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (currentMode) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "System"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeyRow(
    provider: ProviderType,
    hasKey: Boolean,
    isVisible: Boolean,
    onEdit: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (hasKey) "Configured" else "Not configured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleVisibility) {
            Icon(
                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle key visibility"
            )
        }
        Text(
            text = "Edit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onEdit() }
                .padding(horizontal = 8.dp)
        )
    }
}
