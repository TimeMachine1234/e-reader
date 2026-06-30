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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.data.datastore.ReaderSettingsDataStore
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.SortBy
import com.pageturn.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject

data class AppSettingsUiState(
    val bookCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val defaultReaderSettings: ReaderSettings = ReaderSettings(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val readerSettingsDataStore: ReaderSettingsDataStore,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        loadBookCount()
        loadStorageSize()
        loadDefaultReaderSettings()
    }

    private fun loadBookCount() {
        viewModelScope.launch {
            try {
                val count = bookRepository.getBookCount()
                _uiState.update { it.copy(bookCount = count) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    private fun loadStorageSize() {
        viewModelScope.launch {
            try {
                val totalBytes = fileUtils.getTotalBooksSize(context.filesDir)
                _uiState.update { it.copy(totalStorageBytes = totalBytes) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    private fun loadDefaultReaderSettings() {
        viewModelScope.launch {
            readerSettingsDataStore.settingsFlow
                .catch { e -> _uiState.update { it.copy(message = e.message) } }
                .collect { protoSettings ->
                    val domainSettings = ReaderSettings(
                        fontFamily = protoSettings.fontFamily,
                        fontSizeSp = protoSettings.fontSizeSp,
                        lineSpacing = protoSettings.lineSpacing,
                        letterSpacing = protoSettings.letterSpacing,
                        paragraphSpacing = protoSettings.paragraphSpacing,
                        justifyText = protoSettings.justifyText,
                        boldText = protoSettings.boldText,
                        horizontalMarginDp = protoSettings.horizontalMarginDp,
                        verticalPaddingDp = protoSettings.verticalPaddingDp,
                        columns = protoSettings.columns,
                        paginateMode = protoSettings.paginateMode,
                        pageTurnAnimation = protoSettings.pageTurnAnimation,
                        theme = protoSettings.theme,
                        customBgColor = protoSettings.customBgColor,
                        customTextColor = protoSettings.customTextColor,
                        brightness = protoSettings.brightness,
                        showProgressBar = protoSettings.showProgressBar,
                        showChapterProgress = protoSettings.showChapterProgress,
                        showTimeRemaining = protoSettings.showTimeRemaining,
                        keepScreenAwake = protoSettings.keepScreenAwake,
                        volumeButtonPageTurn = protoSettings.volumeButtonPageTurn,
                        tapZoneLayout = protoSettings.tapZoneLayout
                    )
                    _uiState.update { it.copy(defaultReaderSettings = domainSettings) }
                }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                // Clear Coil disk cache directory
                val coilCacheDir = context.cacheDir.resolve("image_cache")
                coilCacheDir.deleteRecursively()

                // Clear general temp/cache files
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                    else if (file.name != "image_cache") file.deleteRecursively()
                }

                _uiState.update { it.copy(message = "Cache cleared") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    fun exportLibrary(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val books = bookRepository.getAllBooks(SortBy.DATE_ADDED).first()
                val mapSerializer = ListSerializer(MapSerializer(String.serializer(), String.serializer()))
                val jsonContent = json.encodeToString(mapSerializer, books.map { it.toExportMap() })
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Cannot open output stream for URI")
                _uiState.update { it.copy(isExporting = false, message = "Library exported successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, message = e.message) }
            }
        }
    }

    fun importLibrary(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("Cannot open input stream for URI")

                val mapSerializer = ListSerializer(MapSerializer(String.serializer(), String.serializer()))
                val exportedBooks = json.decodeFromString(mapSerializer, jsonContent)
                var importedCount = 0
                exportedBooks.forEach { bookMap ->
                    val book = bookMap.toBook()
                    if (book != null) {
                        try {
                            bookRepository.insertBook(book)
                            importedCount++
                        } catch (ignored: Exception) {
                            // Skip duplicates or errors for individual books
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        message = "Imported $importedCount books"
                    )
                }
                loadBookCount()
                loadStorageSize()
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, message = e.message) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun updateDefaultFontFamily(fontFamily: String) {
        viewModelScope.launch {
            readerSettingsDataStore.updateFontFamily(fontFamily)
        }
    }

    fun updateDefaultFontSize(sp: Float) {
        viewModelScope.launch {
            readerSettingsDataStore.updateFontSize(sp)
        }
    }

    fun updateDefaultTheme(theme: String) {
        viewModelScope.launch {
            readerSettingsDataStore.updateTheme(theme)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            readerSettingsDataStore.resetToDefaults()
        }
    }

    // Helpers for JSON export/import
    private fun Book.toExportMap(): Map<String, String> = buildMap {
        put("id", id)
        put("title", title)
        author?.let { put("author", it) }
        put("filePath", filePath)
        put("format", format)
        coverPath?.let { put("coverPath", it) }
        totalPages?.let { put("totalPages", it.toString()) }
        put("currentPage", currentPage.toString())
        put("progressPercent", progressPercent.toString())
        put("dateAdded", dateAdded.toString())
        lastOpened?.let { put("lastOpened", it.toString()) }
        put("isFinished", isFinished.toString())
        put("isFavorite", isFavorite.toString())
        put("readingTimeMs", readingTimeMs.toString())
    }

    private fun Map<String, String>.toBook(): Book? {
        val id = get("id") ?: return null
        val title = get("title") ?: return null
        val filePath = get("filePath") ?: return null
        val format = get("format") ?: return null
        val dateAdded = get("dateAdded")?.toLongOrNull() ?: System.currentTimeMillis()
        return Book(
            id = id,
            title = title,
            author = get("author"),
            filePath = filePath,
            format = format,
            coverPath = get("coverPath"),
            totalPages = get("totalPages")?.toIntOrNull(),
            currentPage = get("currentPage")?.toIntOrNull() ?: 0,
            progressPercent = get("progressPercent")?.toFloatOrNull() ?: 0f,
            dateAdded = dateAdded,
            lastOpened = get("lastOpened")?.toLongOrNull(),
            isFinished = get("isFinished")?.toBooleanStrictOrNull() ?: false,
            isFavorite = get("isFavorite")?.toBooleanStrictOrNull() ?: false,
            readingTimeMs = get("readingTimeMs")?.toLongOrNull() ?: 0L
        )
    }
}
