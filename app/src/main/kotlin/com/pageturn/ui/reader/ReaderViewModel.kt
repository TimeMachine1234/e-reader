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

package com.pageturn.ui.reader

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.data.datastore.ReaderSettingsDataStore
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.Bookmark
import com.pageturn.domain.model.Highlight
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.domain.repository.BookmarkRepository
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.HighlightRepository
import com.pageturn.domain.usecase.SaveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val isLoading: Boolean = true,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    // 1-based page within the current chapter (EPUB pager). 0 = not reported.
    val displayPage: Int = 0,
    val displayPageCount: Int = 0,
    val progressPercent: Float = 0f,
    val currentCfi: String? = null,
    val currentChapter: String = "",
    val readerSettings: ReaderSettings = ReaderSettings(),
    val highlights: List<Highlight> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val isChromeVisible: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val isTocOpen: Boolean = false,
    val isHighlightsOpen: Boolean = false,
    val isBookmarksOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val selectedText: String = "",
    val showHighlightMenu: Boolean = false,
    val highlightMenuPosition: Offset = Offset.Zero,
    val searchResults: List<SearchResult> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

data class SearchResult(
    val snippet: String,
    val location: String,
    val page: Int?,
    val cfi: String?
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val highlightRepository: HighlightRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val readerSettingsDataStore: ReaderSettingsDataStore
) : ViewModel() {

    val bookId: String = savedStateHandle["bookId"] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var sessionStartMs: Long = 0L

    init {
        loadBook()
        collectHighlights()
        collectBookmarks()
        collectReaderSettings()
    }

    // ---------------------------------------------------------------------------
    // Init helpers
    // ---------------------------------------------------------------------------

    private fun loadBook() {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBook(bookId).first()
                _uiState.update { state ->
                    state.copy(
                        book = book,
                        isLoading = false,
                        currentPage = book?.currentPage ?: 0,
                        totalPages = book?.totalPages ?: 0,
                        progressPercent = book?.progressPercent ?: 0f,
                        currentCfi = book?.currentCfi
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun collectHighlights() {
        viewModelScope.launch {
            highlightRepository.getHighlightsForBook(bookId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { highlights ->
                    _uiState.update { it.copy(highlights = highlights) }
                }
        }
    }

    private fun collectBookmarks() {
        viewModelScope.launch {
            bookmarkRepository.getBookmarksForBook(bookId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { bookmarks ->
                    _uiState.update { it.copy(bookmarks = bookmarks) }
                }
        }
    }

    private fun collectReaderSettings() {
        viewModelScope.launch {
            readerSettingsDataStore.settingsFlow
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { proto ->
                    val settings = ReaderSettings(
                        fontFamily = proto.fontFamily,
                        fontSizeSp = proto.fontSizeSp,
                        lineSpacing = proto.lineSpacing,
                        letterSpacing = proto.letterSpacing,
                        paragraphSpacing = proto.paragraphSpacing,
                        justifyText = proto.justifyText,
                        boldText = proto.boldText,
                        horizontalMarginDp = proto.horizontalMarginDp,
                        verticalPaddingDp = proto.verticalPaddingDp,
                        columns = proto.columns,
                        paginateMode = proto.paginateMode,
                        pageTurnAnimation = proto.pageTurnAnimation,
                        theme = proto.theme,
                        customBgColor = proto.customBgColor,
                        customTextColor = proto.customTextColor,
                        brightness = proto.brightness,
                        showProgressBar = proto.showProgressBar,
                        showChapterProgress = proto.showChapterProgress,
                        showTimeRemaining = proto.showTimeRemaining,
                        keepScreenAwake = proto.keepScreenAwake,
                        volumeButtonPageTurn = proto.volumeButtonPageTurn,
                        tapZoneLayout = proto.tapZoneLayout,
                        dualPageLandscape = proto.dualPageLandscape,
                        blinkReminder = proto.blinkReminder,
                        blinkIntervalSec = proto.blinkIntervalSec.coerceIn(20, 300)
                    )
                    _uiState.update { it.copy(readerSettings = settings) }
                }
        }
    }

    // ---------------------------------------------------------------------------
    // Reading session lifecycle
    // ---------------------------------------------------------------------------

    fun onReaderResumed() {
        sessionStartMs = System.currentTimeMillis()
    }

    fun onReaderPaused() {
        if (sessionStartMs > 0L) {
            val delta = System.currentTimeMillis() - sessionStartMs
            sessionStartMs = 0L
            saveProgressNow(delta)
        }
    }

    private fun saveProgressNow(readingTimeDeltaMs: Long = 0L) {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            try {
                saveProgressUseCase(
                    bookId = book.id,
                    page = state.currentPage,
                    cfi = state.currentCfi,
                    progressPercent = state.progressPercent,
                    readingTimeDeltaMs = readingTimeDeltaMs
                )
            } catch (e: Exception) {
                // best-effort; don't surface progress-save errors to UI
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        onReaderPaused()
    }

    // ---------------------------------------------------------------------------
    // Page / chapter tracking
    // ---------------------------------------------------------------------------

    fun onPageChange(page: Int, cfi: String? = null) {
        val total = _uiState.value.totalPages.coerceAtLeast(1)
        val progress = (page.toFloat() / total).coerceIn(0f, 1f)
        _uiState.update { it.copy(currentPage = page, currentCfi = cfi, progressPercent = progress) }
        saveProgressNow()
    }

    fun onChapterChange(chapter: String) {
        _uiState.update { it.copy(currentChapter = chapter) }
    }

    /** Navigate to a chapter (spine item) by index, clamped to the valid range. */
    fun goToChapter(index: Int) {
        val total = _uiState.value.totalPages.coerceAtLeast(1)
        val clamped = index.coerceIn(0, total - 1)
        if (clamped != _uiState.value.currentPage) onPageChange(clamped)
    }

    fun nextChapter() = goToChapter(_uiState.value.currentPage + 1)
    fun prevChapter() = goToChapter(_uiState.value.currentPage - 1)

    /**
     * Reports per-page progress within the current chapter (from the EPUB pager).
     * Combines the chapter index with the in-chapter fraction for an overall percent.
     */
    fun onChapterPageInfo(pageInChapter: Int, pagesInChapter: Int) {
        // Position within the chapter (0..1), stored so we can reopen exactly here.
        val within = if (pagesInChapter > 1)
            (pageInChapter.toFloat() / (pagesInChapter - 1)).coerceIn(0f, 1f) else 0f
        _uiState.update { state ->
            val chapters = state.totalPages.coerceAtLeast(1)
            val frac = if (pagesInChapter > 0)
                (pageInChapter.toFloat() / pagesInChapter).coerceIn(0f, 1f) else 0f
            val progress = ((state.currentPage + frac) / chapters).coerceIn(0f, 1f)
            state.copy(
                progressPercent = progress,
                displayPage = pageInChapter + 1,
                displayPageCount = pagesInChapter,
                currentCfi = "f:$within"
            )
        }
        saveProgressNow()
    }

    fun onTotalPagesResolved(total: Int) {
        _uiState.update { state ->
            val progress = if (total > 0) (state.currentPage.toFloat() / total).coerceIn(0f, 1f)
                           else state.progressPercent
            state.copy(totalPages = total, progressPercent = progress)
        }
    }

    // ---------------------------------------------------------------------------
    // Chrome visibility
    // ---------------------------------------------------------------------------

    fun toggleChrome() = _uiState.update { it.copy(isChromeVisible = !it.isChromeVisible) }
    fun showChrome() = _uiState.update { it.copy(isChromeVisible = true) }
    fun hideChrome() = _uiState.update { it.copy(isChromeVisible = false) }

    // ---------------------------------------------------------------------------
    // Panel toggles
    // ---------------------------------------------------------------------------

    fun toggleSettings() = _uiState.update { it.copy(isSettingsOpen = !it.isSettingsOpen) }
    fun openSettings() = _uiState.update { it.copy(isSettingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(isSettingsOpen = false) }

    fun toggleToc() = _uiState.update { it.copy(isTocOpen = !it.isTocOpen) }
    fun openToc() = _uiState.update { it.copy(isTocOpen = true) }
    fun closeToc() = _uiState.update { it.copy(isTocOpen = false) }

    fun toggleBookmarks() = _uiState.update { it.copy(isBookmarksOpen = !it.isBookmarksOpen) }
    fun openBookmarks() = _uiState.update { it.copy(isBookmarksOpen = true) }
    fun closeBookmarks() = _uiState.update { it.copy(isBookmarksOpen = false) }

    fun toggleHighlights() = _uiState.update { it.copy(isHighlightsOpen = !it.isHighlightsOpen) }
    fun openHighlights() = _uiState.update { it.copy(isHighlightsOpen = true) }
    fun closeHighlights() = _uiState.update { it.copy(isHighlightsOpen = false) }

    fun openSearch() = _uiState.update { it.copy(isSearchOpen = true) }
    fun closeSearch() = _uiState.update { it.copy(isSearchOpen = false, searchQuery = "", searchResults = emptyList()) }

    // ---------------------------------------------------------------------------
    // Highlight menu
    // ---------------------------------------------------------------------------

    fun onTextSelected(text: String, position: Offset) {
        _uiState.update { it.copy(selectedText = text, showHighlightMenu = true, highlightMenuPosition = position) }
    }

    fun dismissHighlightMenu() {
        _uiState.update { it.copy(showHighlightMenu = false, selectedText = "") }
    }

    // ---------------------------------------------------------------------------
    // Highlights CRUD
    // ---------------------------------------------------------------------------

    fun addHighlight(selectedText: String, color: String, cfi: String?, page: Int?) {
        val state = _uiState.value
        val highlight = Highlight(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            cfi = cfi ?: state.currentCfi,
            page = page ?: state.currentPage,
            selectedText = selectedText,
            color = color,
            note = null,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                highlightRepository.insertHighlight(highlight)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            try {
                highlightRepository.deleteHighlight(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateHighlightNote(id: String, note: String) {
        val highlight = _uiState.value.highlights.find { it.id == id } ?: return
        viewModelScope.launch {
            try {
                highlightRepository.updateHighlight(highlight.copy(note = note))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Bookmarks CRUD
    // ---------------------------------------------------------------------------

    fun addBookmark(cfi: String? = null, page: Int? = null, label: String? = null) {
        val state = _uiState.value
        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            cfi = cfi ?: state.currentCfi,
            page = page ?: state.currentPage,
            label = label,
            note = null,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                bookmarkRepository.insertBookmark(bookmark)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmark(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun jumpToBookmark(bookmark: Bookmark) {
        _uiState.update { state ->
            state.copy(
                currentPage = bookmark.page ?: state.currentPage,
                currentCfi = bookmark.cfi ?: state.currentCfi,
                isBookmarksOpen = false
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------------

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchResultsReceived(results: List<SearchResult>) {
        _uiState.update { it.copy(searchResults = results) }
    }

    fun onSearchResultSelected(result: SearchResult) {
        _uiState.update { state ->
            state.copy(
                isSearchOpen = false,
                currentPage = result.page ?: state.currentPage,
                currentCfi = result.cfi ?: state.currentCfi
            )
        }
    }

    fun onProgressChanged(progressPercent: Float) {
        _uiState.update { it.copy(progressPercent = progressPercent) }
    }

    fun onReadError(message: String?) {
        _uiState.update { it.copy(isLoading = false, error = message ?: "Unknown error") }
    }

    // ---------------------------------------------------------------------------
    // Settings updates — delegate to DataStore
    // ---------------------------------------------------------------------------

    fun updateFontSize(sp: Float) = viewModelScope.launch { readerSettingsDataStore.updateFontSize(sp) }
    fun updateTheme(theme: String) = viewModelScope.launch { readerSettingsDataStore.updateTheme(theme) }
    fun updateFontFamily(family: String) = viewModelScope.launch { readerSettingsDataStore.updateFontFamily(family) }
    fun updateLineSpacing(spacing: Float) = viewModelScope.launch { readerSettingsDataStore.updateLineSpacing(spacing) }
    fun updateLetterSpacing(spacing: Float) = viewModelScope.launch { readerSettingsDataStore.updateLetterSpacing(spacing) }
    fun updateParagraphSpacing(sp: Float) = viewModelScope.launch { readerSettingsDataStore.updateParagraphSpacing(sp) }
    fun updateJustifyText(j: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateJustifyText(j) }
    fun updateBoldText(b: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateBoldText(b) }
    fun updateHorizontalMargin(dp: Int) = viewModelScope.launch { readerSettingsDataStore.updateHorizontalMargin(dp) }
    fun updateVerticalPadding(dp: Int) = viewModelScope.launch { readerSettingsDataStore.updateVerticalPadding(dp) }
    fun updateColumns(c: Int) = viewModelScope.launch { readerSettingsDataStore.updateColumns(c) }
    fun updatePaginateMode(p: Boolean) = viewModelScope.launch { readerSettingsDataStore.updatePaginateMode(p) }
    fun updatePageTurnAnimation(a: String) = viewModelScope.launch { readerSettingsDataStore.updatePageTurnAnimation(a) }
    fun updateBrightness(b: Int) = viewModelScope.launch { readerSettingsDataStore.updateBrightness(b) }
    fun updateShowProgressBar(s: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateShowProgressBar(s) }
    fun updateShowChapterProgress(s: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateShowChapterProgress(s) }
    fun updateShowTimeRemaining(s: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateShowTimeRemaining(s) }
    fun updateKeepScreenAwake(k: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateKeepScreenAwake(k) }
    fun updateVolumeButtonPageTurn(v: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateVolumeButtonPageTurn(v) }
    fun updateTapZoneLayout(t: String) = viewModelScope.launch { readerSettingsDataStore.updateTapZoneLayout(t) }
    fun updateDualPageLandscape(e: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateDualPageLandscape(e) }
    fun updateBlinkReminder(e: Boolean) = viewModelScope.launch { readerSettingsDataStore.updateBlinkReminder(e) }
    fun updateBlinkInterval(s: Int) = viewModelScope.launch { readerSettingsDataStore.updateBlinkIntervalSec(s) }
    fun resetSettings() = viewModelScope.launch { readerSettingsDataStore.resetToDefaults() }
}
