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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.domain.model.Book
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.SortBy
import com.pageturn.domain.usecase.FilterBy
import com.pageturn.domain.usecase.GetLibraryUseCase
import com.pageturn.domain.usecase.ImportBookUseCase
import com.pageturn.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.RECENTLY_OPENED,
    val filterBy: FilterBy = FilterBy.None,
    val isSearchActive: Boolean = false,
    val importProgress: String? = null,
    val showDuplicateDialog: Boolean = false,
    val pendingImportUri: android.net.Uri? = null,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryUseCase: GetLibraryUseCase,
    private val importBookUseCase: ImportBookUseCase,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var booksJob: Job? = null

    init {
        loadBooks()
    }

    fun loadBooks() {
        booksJob?.cancel()
        booksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getLibraryUseCase(
                sortBy = _uiState.value.sortBy,
                filterBy = _uiState.value.filterBy
            )
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { books ->
                    _uiState.update { it.copy(books = books, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        booksJob?.cancel()
        if (query.isBlank()) {
            loadBooks()
        } else {
            booksJob = viewModelScope.launch {
                getLibraryUseCase.search(query)
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message) }
                    }
                    .collect { books ->
                        _uiState.update { it.copy(books = books, isLoading = false) }
                    }
            }
        }
    }

    fun onSortChange(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        loadBooks()
    }

    fun onFilterChange(filterBy: FilterBy) {
        _uiState.update { it.copy(filterBy = filterBy) }
        loadBooks()
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            importBookUseCase(uri)
                .collect { result ->
                    when (result) {
                        is ImportResult.Progress -> {
                            _uiState.update { it.copy(importProgress = result.step) }
                        }
                        is ImportResult.Success -> {
                            _uiState.update {
                                it.copy(importProgress = null, pendingImportUri = null)
                            }
                        }
                        is ImportResult.Error -> {
                            _uiState.update {
                                it.copy(importProgress = null, error = result.message)
                            }
                        }
                        is ImportResult.Duplicate -> {
                            _uiState.update {
                                it.copy(
                                    importProgress = null,
                                    showDuplicateDialog = true,
                                    pendingImportUri = uri
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onDuplicateConfirm() {
        val uri = _uiState.value.pendingImportUri ?: return
        _uiState.update { it.copy(showDuplicateDialog = false) }
        viewModelScope.launch {
            importBookUseCase(uri, replaceIfDuplicate = true)
                .collect { result ->
                    when (result) {
                        is ImportResult.Progress -> {
                            _uiState.update { it.copy(importProgress = result.step) }
                        }
                        is ImportResult.Success -> {
                            _uiState.update {
                                it.copy(importProgress = null, pendingImportUri = null)
                            }
                        }
                        is ImportResult.Error -> {
                            _uiState.update {
                                it.copy(importProgress = null, error = result.message)
                            }
                        }
                        else -> Unit
                    }
                }
        }
    }

    fun onDuplicateCancel() {
        _uiState.update { it.copy(showDuplicateDialog = false, pendingImportUri = null) }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            bookRepository.updateBook(book.copy(isFavorite = !book.isFavorite))
        }
    }

    fun markFinished(book: Book) {
        viewModelScope.launch {
            bookRepository.updateBook(book.copy(isFinished = true))
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun activateSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun deactivateSearch() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "") }
        loadBooks()
    }
}
