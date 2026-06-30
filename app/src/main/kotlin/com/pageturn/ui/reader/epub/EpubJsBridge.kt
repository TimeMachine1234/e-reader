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

package com.pageturn.ui.reader.epub

import android.webkit.JavascriptInterface
import androidx.compose.ui.geometry.Offset
import com.pageturn.ui.reader.ReaderViewModel
import com.pageturn.ui.reader.SearchResult
import org.json.JSONArray
import org.json.JSONException

/**
 * JavaScript interface exposed as `window.Android` inside the EPUB WebView.
 *
 * All methods annotated with [JavascriptInterface] are called from the WebView's
 * JavaScript thread. Any UI/ViewModel interactions are dispatched through
 * [ReaderViewModel], which handles thread-switching internally via
 * `viewModelScope.launch`.
 */
class EpubJsBridge(
    private val viewModel: ReaderViewModel,
    private val onRequestNextChapter: () -> Unit = {},
    private val onRequestPrevChapter: () -> Unit = {}
) {

    /**
     * Reports the current page and total page count within the active chapter,
     * so the ViewModel can compute fine-grained reading progress.
     */
    @JavascriptInterface
    fun onChapterPage(page: Int, total: Int) {
        viewModel.onChapterPageInfo(page, total)
    }

    /** Requested by the JS pager when advancing past the last page of a chapter. */
    @JavascriptInterface
    fun requestNextChapter() {
        onRequestNextChapter()
    }

    /** Requested by the JS pager when going back before the first page of a chapter. */
    @JavascriptInterface
    fun requestPrevChapter() {
        onRequestPrevChapter()
    }

    /**
     * Called by JS when the reader moves to a new page / location.
     *
     * @param page        0-based page index reported by the JS layer.
     * @param totalPages  Total page count as computed by epubjs or equivalent.
     * @param cfi         Current EPUB CFI string identifying the location.
     */
    @JavascriptInterface
    fun onPageChanged(page: Int, totalPages: Int, cfi: String) {
        if (totalPages > 0) {
            viewModel.onTotalPagesResolved(totalPages)
        }
        viewModel.onPageChange(page, cfi.takeIf { it.isNotBlank() })
    }

    /**
     * Called by JS when the reader enters a new chapter.
     *
     * @param chapterTitle Human-readable chapter title from the TOC.
     */
    @JavascriptInterface
    fun onChapterChanged(chapterTitle: String) {
        viewModel.onChapterChange(chapterTitle)
    }

    /**
     * Called by JS when the user finishes a text selection.
     *
     * @param selectedText The highlighted text string.
     * @param cfi          CFI of the selection range (may be empty if not computed).
     * @param x            X coordinate of the selection anchor in CSS pixels.
     * @param y            Y coordinate of the selection anchor in CSS pixels.
     */
    @JavascriptInterface
    fun onTextSelected(selectedText: String, cfi: String, x: Float, y: Float) {
        if (selectedText.isBlank()) return
        viewModel.onTextSelected(selectedText, Offset(x, y))
    }

    /**
     * Called by JS with search results encoded as a JSON array.
     *
     * Expected JSON format:
     * ```json
     * [
     *   { "snippet": "…text…", "location": "chapter-id", "cfi": "epubcfi(…)", "page": 3 },
     *   …
     * ]
     * ```
     *
     * @param resultsJson JSON string of result objects.
     */
    @JavascriptInterface
    fun onSearchResults(resultsJson: String) {
        val results = mutableListOf<SearchResult>()
        try {
            val array = JSONArray(resultsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                results.add(
                    SearchResult(
                        snippet = obj.optString("snippet", ""),
                        location = obj.optString("location", ""),
                        cfi = obj.optString("cfi").takeIf { it.isNotBlank() },
                        page = if (obj.has("page")) obj.getInt("page") else null
                    )
                )
            }
        } catch (e: JSONException) {
            // Malformed results — no-op; partial list (possibly empty) is still forwarded
        }
        viewModel.onSearchResultsReceived(results)
    }

    /**
     * Called by JS to report fine-grained reading progress (e.g. scroll percentage)
     * without a full page-change event.
     *
     * @param progressPercent Value in [0, 1].
     */
    @JavascriptInterface
    fun onProgressChanged(progressPercent: Float) {
        val clamped = progressPercent.coerceIn(0f, 1f)
        viewModel.onProgressChanged(clamped)
    }

    /**
     * Called by JS when the user taps the content area (not a link or selection).
     * Toggles the reader chrome (top/bottom bars).
     *
     * @param x CSS pixel X of the tap.
     * @param y CSS pixel Y of the tap.
     */
    @JavascriptInterface
    fun onTap(x: Float, y: Float) {
        viewModel.toggleChrome()
    }
}
