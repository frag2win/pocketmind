package com.frag2win.pocketmind.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.remote.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedVariant by viewModel.selectedVariant.collectAsState()
    val isAutoSelect by viewModel.isAutoSelect.collectAsState()
    val hfToken by viewModel.hfToken.collectAsState()
    val tavilyApiKey by viewModel.tavilyApiKey.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val downloadStatuses by viewModel.downloadStatuses.collectAsState()
    val ram = viewModel.getAvailableRamGb()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .selectableGroup()
        ) {
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = hfToken,
                onValueChange = { viewModel.updateHfToken(it) },
                label = { Text("Hugging Face Read Token") },
                placeholder = { Text("hf_...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Text(
                "Required for gated Gemma models.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Web Search (RAG)",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = tavilyApiKey,
                onValueChange = { viewModel.updateTavilyApiKey(it) },
                label = { Text("Tavily API Key") },
                placeholder = { Text("tvly-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Text(
                "Required for real-time web search grounding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GitHub Integration",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = githubToken,
                onValueChange = { viewModel.updateGitHubToken(it) },
                label = { Text("GitHub Personal Access Token") },
                placeholder = { Text("ghp_...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Text(
                "Used for repo browsing and AI code analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AI Model Selection",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Your device has ~${String.format(Locale.getDefault(), "%.1f", ram)} GB free RAM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Select Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-select (Recommended)")
                    Text(
                        "Automatically chooses the best model for your device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = isAutoSelect,
                    onCheckedChange = { viewModel.setAutoSelect(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("Model Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            GemmaVariant.entries.forEach { variant ->
                val state = downloadStatuses[variant] ?: DownloadState.Idle
                ModelItem(
                    variant = variant,
                    isSelected = (variant == selectedVariant),
                    state = state,
                    onSelect = { viewModel.updateVariant(variant) },
                    onDownload = { viewModel.downloadModel(variant) }
                ) { viewModel.deleteModel(variant) }
            }
        }
    }
}

@Composable
fun ModelItem(
    variant: GemmaVariant,
    isSelected: Boolean,
    state: DownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .selectable(
                    selected = isSelected,
                    onClick = onSelect,
                    role = Role.RadioButton
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = variant.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Size: ${variant.modelSize} | RAM: ${variant.ramRequired}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                when (state) {
                    is DownloadState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                    else -> {}
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            when (state) {
                DownloadState.Idle, is DownloadState.Error -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
                DownloadState.Completed -> {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                is DownloadState.Downloading -> {
                    Text(
                        text = "${state.progress}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp)
    }
}
