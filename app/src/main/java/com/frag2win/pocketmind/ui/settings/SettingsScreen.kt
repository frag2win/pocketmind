package com.frag2win.pocketmind.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frag2win.pocketmind.domain.inference.GemmaVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedVariant by viewModel.selectedVariant.collectAsState()
    val isAutoSelect by viewModel.isAutoSelect.collectAsState()
    val ram = viewModel.getAvailableRamGb()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .selectableGroup()
        ) {
            Text(
                text = "AI Model Selection",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Your device has ~${String.format("%.1f", ram)} GB free RAM.",
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

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Manual Override", style = MaterialTheme.typography.titleMedium)
            
            GemmaVariant.values().forEach { variant ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .selectable(
                            selected = (variant == selectedVariant),
                            onClick = { viewModel.updateVariant(variant) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (variant == selectedVariant),
                        onClick = null // Selected by parent Row
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(text = variant.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Size: ${variant.modelSize} | Requires: ${variant.ramRequired}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
