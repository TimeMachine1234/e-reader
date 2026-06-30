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

package com.pageturn.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.Collection
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.CollectionRepository
import com.pageturn.domain.repository.SortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val editingCollection: Collection? = null,
    val error: String? = null
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    val allBooks: StateFlow<List<Book>> = bookRepository
        .getAllBooks(SortBy.RECENTLY_OPENED)
        .catch { e -> _uiState.update { it.copy(error = e.message) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        loadCollections()
    }

    fun loadCollections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            collectionRepository.getAllCollections()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { collections ->
                    _uiState.update { it.copy(collections = collections, isLoading = false) }
                }
        }
    }

    fun createCollection(name: String, emoji: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            try {
                val collection = Collection(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    emoji = emoji?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis(),
                    books = emptyList()
                )
                collectionRepository.insertCollection(collection)
                _uiState.update { it.copy(showCreateDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            try {
                collectionRepository.deleteCollection(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun renameCollection(collection: Collection, newName: String, newEmoji: String?) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            try {
                collectionRepository.updateCollection(
                    collection.copy(
                        name = trimmedName,
                        emoji = newEmoji?.trim()?.takeIf { it.isNotBlank() }
                    )
                )
                _uiState.update { it.copy(editingCollection = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addBookToCollection(bookId: String, collectionId: String) {
        viewModelScope.launch {
            try {
                collectionRepository.addBookToCollection(bookId, collectionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeBookFromCollection(bookId: String, collectionId: String) {
        viewModelScope.launch {
            try {
                collectionRepository.removeBookFromCollection(bookId, collectionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun startEditCollection(collection: Collection) {
        _uiState.update { it.copy(editingCollection = collection) }
    }

    fun clearEditingCollection() {
        _uiState.update { it.copy(editingCollection = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
