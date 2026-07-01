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

package com.pageturn.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import com.pageturn.domain.model.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturn.reader.R
import com.pageturn.domain.repository.SortBy
import com.pageturn.domain.usecase.FilterBy
import com.pageturn.ui.library.components.BookGrid
import com.pageturn.ui.library.components.ImportFab
import com.pageturn.util.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var renamingBook by remember { mutableStateOf<Book?>(null) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchFocusRequester = remember { FocusRequester() }

    // SAF file picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBook(it) }
    }

    // Show import progress / success in snackbar
    val importInProgressLabel = stringResource(R.string.import_in_progress)
    val importSuccessLabel = stringResource(R.string.import_success)
    LaunchedEffect(uiState.importProgress) {
        val progress = uiState.importProgress
        if (progress != null) {
            snackbarHostState.showSnackbar(message = "$importInProgressLabel $progress")
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearError()
        }
    }

    // Focus search field when activated
    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.isSearchActive) {
                SearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClose = viewModel::deactivateSearch,
                    focusRequester = searchFocusRequester
                )
            } else {
                LibraryTopBar(
                    sortBy = uiState.sortBy,
                    showSortMenu = showSortMenu,
                    onShowSortMenu = { showSortMenu = true },
                    onDismissSortMenu = { showSortMenu = false },
                    onSortChange = { sort ->
                        showSortMenu = false
                        viewModel.onSortChange(sort)
                    },
                    onSearchClick = viewModel::activateSearch,
                    onFilterClick = { showFilterSheet = true },
                    onCollectionsClick = onNavigateToCollections,
                    onSettingsClick = onNavigateToSettings
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSearchActive) {
                ImportFab(
                    onImportClick = {
                        importLauncher.launch(FileUtils.SUPPORTED_MIME_TYPES)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.books.isEmpty() -> {
                    EmptyLibraryContent(
                        onImportClick = {
                            importLauncher.launch(FileUtils.SUPPORTED_MIME_TYPES)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    BookGrid(
                        books = uiState.books,
                        onBookClick = onBookClick,
                        onAddToCollection = { book ->
                            // Navigation to collection picker would be handled here
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onMarkFinished = viewModel::markFinished,
                        onRename = { book -> renamingBook = book },
                        onDelete = { book -> viewModel.deleteBook(book.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState
        ) {
            FilterBottomSheetContent(
                currentFilter = uiState.filterBy,
                onFilterSelected = { filter ->
                    viewModel.onFilterChange(filter)
                    scope.launch { filterSheetState.hide() }.invokeOnCompletion {
                        showFilterSheet = false
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Duplicate book dialog
    if (uiState.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDuplicateCancel,
            title = { Text(stringResource(R.string.import_duplicate_title)) },
            text = { Text(stringResource(R.string.import_duplicate_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDuplicateConfirm) {
                    Text(stringResource(R.string.import_duplicate_replace))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDuplicateCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Rename book dialog
    renamingBook?.let { book ->
        var newTitle by remember(book.id) { mutableStateOf(book.title) }
        AlertDialog(
            onDismissRequest = { renamingBook = null },
            title = { Text(stringResource(R.string.book_action_rename)) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.rename_title_label)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameBook(book, newTitle)
                        renamingBook = null
                    },
                    enabled = newTitle.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingBook = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    sortBy: SortBy,
    showSortMenu: Boolean,
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortChange: (SortBy) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.library_title)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.action_search)
                )
            }
            Box {
                IconButton(onClick = onShowSortMenu) {
                    Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = stringResource(R.string.library_sort_by)
                    )
                }
                SortDropdownMenu(
                    expanded = showSortMenu,
                    currentSortBy = sortBy,
                    onDismiss = onDismissSortMenu,
                    onSortSelected = onSortChange
                )
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = stringResource(R.string.library_filter)
                )
            }
            IconButton(onClick = onCollectionsClick) {
                Icon(
                    imageVector = Icons.Rounded.CollectionsBookmark,
                    contentDescription = stringResource(R.string.collections_title)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.app_settings_title)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.library_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* dismiss keyboard */ }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_close)
                )
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSortBy: SortBy,
    onDismiss: () -> Unit,
    onSortSelected: (SortBy) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortBy.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.displayName()) },
                trailingIcon = {
                    if (sort == currentSortBy) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = { onSortSelected(sort) }
            )
        }
    }
}

@Composable
private fun SortBy.displayName(): String = when (this) {
    SortBy.RECENTLY_OPENED -> stringResource(R.string.sort_recently_opened)
    SortBy.TITLE           -> stringResource(R.string.sort_title_az)
    SortBy.AUTHOR          -> stringResource(R.string.sort_author)
    SortBy.DATE_ADDED      -> stringResource(R.string.sort_date_added)
    SortBy.PROGRESS        -> stringResource(R.string.sort_reading_progress)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheetContent(
    currentFilter: FilterBy,
    onFilterSelected: (FilterBy) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.library_filter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Format filters
        Text(
            text = stringResource(R.string.filter_all_formats),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = currentFilter is FilterBy.None,
                onClick = { onFilterSelected(FilterBy.None) },
                label = { Text(stringResource(R.string.filter_all_books)) }
            )
            listOf("epub" to R.string.filter_epub,
                   "pdf"  to R.string.filter_pdf,
                   "txt"  to R.string.filter_plain_text,
                   "cbz"  to R.string.filter_cbz,
                   "cbr"  to R.string.filter_cbr
            ).forEach { (fmt, labelRes) ->
                FilterChip(
                    selected = currentFilter is FilterBy.Format && currentFilter.format == fmt,
                    onClick = { onFilterSelected(FilterBy.Format(fmt)) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Status filters
        Text(
            text = stringResource(R.string.filter_all_books),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data class StatusOption(val filter: FilterBy, val labelRes: Int)
            listOf(
                StatusOption(FilterBy.Unread,     R.string.filter_unread),
                StatusOption(FilterBy.InProgress, R.string.filter_in_progress),
                StatusOption(FilterBy.Finished,   R.string.filter_finished),
                StatusOption(FilterBy.Favorites,  R.string.filter_favorites)
            ).forEach { option ->
                FilterChip(
                    selected = currentFilter == option.filter,
                    onClick = { onFilterSelected(option.filter) },
                    label = { Text(stringResource(option.labelRes)) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyLibraryContent(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CollectionsBookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .then(Modifier.padding(0.dp))
        )
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportClick) {
            Text(stringResource(R.string.library_import_book))
        }
    }
}
