// Copyright 2024 PageTurn Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pageturn.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturn.reader.BuildConfig
import com.pageturn.reader.R
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.ui.theme.READER_THEMES
import com.pageturn.util.FileUtils

private const val GITHUB_URL = "https://github.com/pageturn/e-reader"
private const val GITHUB_ISSUES_URL = "https://github.com/pageturn/e-reader/issues"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val fileUtils = remember { FileUtils() }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportLibrary(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importLibrary(it) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Section: Default Reader Settings
            item {
                SettingsSectionHeader(stringResource(R.string.app_settings_default_reader))
            }
            item {
                FontFamilyPicker(
                    selectedFont = uiState.defaultReaderSettings.fontFamily,
                    onFontSelected = { viewModel.updateDefaultFontFamily(it) }
                )
            }
            item {
                FontSizeSlider(
                    fontSize = uiState.defaultReaderSettings.fontSizeSp,
                    onFontSizeChange = { viewModel.updateDefaultFontSize(it) }
                )
            }
            item {
                ThemePicker(
                    selectedTheme = uiState.defaultReaderSettings.theme,
                    onThemeSelected = { viewModel.updateDefaultTheme(it) }
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.app_settings_more_settings_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            item {
                TextButton(
                    onClick = { viewModel.resetDefaults() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.settings_reset_defaults))
                }
                HorizontalDivider()
            }

            // Section: Storage
            item {
                SettingsSectionHeader(stringResource(R.string.app_settings_storage))
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.app_settings_storage_summary,
                                uiState.bookCount,
                                fileUtils.formatFileSize(uiState.totalStorageBytes)
                            )
                        )
                    }
                )
            }
            item {
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.app_settings_clear_cache))
                }
                HorizontalDivider()
            }

            // Section: Library Backup
            item {
                SettingsSectionHeader(stringResource(R.string.app_settings_library_backup))
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = {
                            exportLauncher.launch("pageturn_library_backup.json")
                        },
                        enabled = !uiState.isExporting && !uiState.isImporting
                    ) {
                        Text(stringResource(R.string.app_settings_export_library))
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        enabled = !uiState.isExporting && !uiState.isImporting
                    ) {
                        Text(stringResource(R.string.app_settings_import_library))
                    }
                }
                HorizontalDivider()
            }

            // Section: About
            item {
                SettingsSectionHeader(stringResource(R.string.app_settings_about))
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.app_settings_version,
                                BuildConfig.VERSION_NAME
                            )
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.app_settings_open_source_licenses))
                    },
                    modifier = Modifier.clickable { showLicensesDialog = true }
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.app_settings_view_on_github))
                    },
                    supportingContent = {
                        Text(
                            text = GITHUB_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(GITHUB_URL))
                            }
                        ) {
                            Text(stringResource(R.string.action_copy))
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.app_settings_report_bug))
                    },
                    supportingContent = {
                        Text(
                            text = GITHUB_ISSUES_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(GITHUB_ISSUES_URL))
                            }
                        ) {
                            Text(stringResource(R.string.action_copy))
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Clear cache confirmation dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.app_settings_clear_cache)) },
            text = { Text(stringResource(R.string.app_settings_clear_cache_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Licenses dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text(stringResource(R.string.app_settings_open_source_licenses)) },
            text = {
                Text(
                    text = stringResource(R.string.app_settings_licenses_content),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun FontFamilyPicker(
    selectedFont: String,
    onFontSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.typography_font_family),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            ReaderSettings.FONT_FAMILIES.forEach { font ->
                FilterChip(
                    selected = font == selectedFont,
                    onClick = { onFontSelected(font) },
                    label = { Text(fontDisplayName(font)) }
                )
            }
        }
    }
}

@Composable
private fun FontSizeSlider(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.typography_font_size),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${fontSize.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 12f..36f,
            steps = 23, // 1sp steps: 12..36 = 24 values = 23 steps
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ThemePicker(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.appearance_theme),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            READER_THEMES.forEach { theme ->
                val isSelected = theme.name == selectedTheme
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.backgroundColor)
                            .border(
                                border = if (isSelected) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                },
                                shape = CircleShape
                            )
                            .clickable { onThemeSelected(theme.name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(theme.textColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun fontDisplayName(font: String): String = when (font) {
    "georgia" -> "Georgia"
    "palatino" -> "Palatino"
    "opendyslexic" -> "OpenDyslexic"
    "lato" -> "Lato"
    "merriweather" -> "Merriweather"
    "eb_garamond" -> "EB Garamond"
    "system" -> "System"
    else -> font.replaceFirstChar { it.uppercase() }
}
