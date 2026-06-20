package com.chatapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import com.chatapp.ui.animation.smoothExpandVertically
import com.chatapp.ui.animation.smoothFadeIn
import com.chatapp.ui.animation.smoothFadeOut
import com.chatapp.ui.animation.smoothShrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.ui.theme.LocalStrings
import com.chatapp.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProvider: (ProviderType) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings) },
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
            SectionHeader(s.appearance)
            ThemeSelector(
                currentMode = uiState.themeMode,
                onSelect = { viewModel.setThemeMode(it) }
            )
            LanguageSelector(
                currentLang = uiState.language,
                onSelect = { viewModel.setLanguage(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- API Key / Token ---
            SectionHeader(s.apiKeyToken)

            val configured = uiState.configuredProviders
            val unconfigured = ProviderType.entries.filter { it !in configured }

            // Show configured providers
            configured.forEach { provider ->
                ProviderRow(
                    provider = provider,
                    onEdit = { onEditProvider(provider) }
                )
            }

            // Unconfigured providers (when expanded)
            AnimatedVisibility(
                visible = uiState.showOtherProviders && unconfigured.isNotEmpty(),
                enter = smoothExpandVertically() + smoothFadeIn(),
                exit = smoothShrinkVertically() + smoothFadeOut()
            ) {
                Column {
                    unconfigured.forEach { provider ->
                        ProviderRow(
                            provider = provider,
                            onEdit = { onEditProvider(provider) }
                        )
                    }
                }
            }

            // Expand/collapse toggle at the bottom
            if (unconfigured.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleOtherProviders() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.addProvider,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (uiState.showOtherProviders) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = if (uiState.showOtherProviders) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- Network ---
            SectionHeader(s.network)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.proxy,
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.proxyEnabled,
                    onCheckedChange = { viewModel.setProxyEnabled(it) }
                )
            }

            AnimatedVisibility(
                visible = uiState.proxyEnabled,
                enter = smoothExpandVertically() + smoothFadeIn(),
                exit = smoothShrinkVertically() + smoothFadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.proxyAddress,
                    onValueChange = { viewModel.setProxyAddress(it) },
                    label = { Text(s.proxy) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- Third-party Multimodal ---
            SectionHeader(s.thirdPartyMultimodal)

            OutlinedTextField(
                value = uiState.multimodalProvider,
                onValueChange = { viewModel.setMultimodalProvider(it) },
                label = { Text(s.providerName) },
                placeholder = { Text("Default") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.multimodalApiUrl,
                onValueChange = { viewModel.setMultimodalApiUrl(it) },
                label = { Text(s.apiEndpoint) },
                placeholder = { Text("https://api.example.com/analyze") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.multimodalApiKey,
                onValueChange = { viewModel.setMultimodalApiKey(it) },
                label = { Text(s.apiKey) },
                placeholder = { Text("Enter API key") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // --- About ---
            SectionHeader(s.about)
            Text(
                text = "${s.version} 0.0.21-alpha",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
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
private fun LanguageSelector(
    currentLang: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val s = LocalStrings.current
    val label = when (currentLang) { "zh" -> "中文" else -> "English" }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = s.language, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("en" to "English", "zh" to "中文").forEach { (value, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(value); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    currentMode: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val s = LocalStrings.current
    val label = when (currentMode) {
        "light" -> s.light
        "dark" -> s.dark
        else -> s.system
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = s.theme, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        // Anchor at bottom-end: DropdownMenu expands from right, below the Row
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("system" to s.system, "light" to s.light, "dark" to s.dark).forEach { (value, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onSelect(value); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderType,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val s = LocalStrings.current
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = s.edit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onEdit() }
                .padding(horizontal = 8.dp)
        )
    }
}

