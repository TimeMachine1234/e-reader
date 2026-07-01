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

package com.pageturn.ui.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.ui.theme.readerThemeByName
import java.io.File

/**
 * Builds a draw-time color filter that re-themes a rendered (white-bg/black-text)
 * PDF page:
 *  - near-white themes  → no filter (original)
 *  - dark themes        → invert (dark background, light text)
 *  - light tinted themes → multiply the white background toward the theme color
 *    while keeping dark text dark (e.g. sepia / warm gray)
 */
private fun pdfThemeFilter(themeName: String): ColorFilter? {
    val bg = readerThemeByName(themeName).backgroundColor
    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return when {
        lum > 0.92f -> null
        lum < 0.5f -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        else -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    bg.red, 0f, 0f, 0f, 0f,
                    0f, bg.green, 0f, 0f, 0f,
                    0f, 0f, bg.blue, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
}

@Composable
fun PdfReader(
    book: Book,
    currentPage: Int,
    readerSettings: ReaderSettings,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmapCache = remember { LruCache<Int, Bitmap>(50) }

    val pageCountState = produceState(initialValue = 0, book.filePath) {
        var renderer: PdfRenderer? = null
        try {
            val file = File(book.filePath)
            if (file.exists()) {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                value = renderer.pageCount
            }
        } catch (e: Exception) {
            value = 0
        } finally {
            renderer?.close()
        }
    }

    val pageCount = pageCountState.value

    if (pageCount == 0) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            Text(
                text = "Loading PDF...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val colorFilter = pdfThemeFilter(readerSettings.theme)

    if (readerSettings.paginateMode) {
        PaginatedPdfReader(
            filePath = book.filePath,
            pageCount = pageCount,
            currentPage = currentPage,
            onPageChange = onPageChange,
            colorFilter = colorFilter,
            modifier = modifier
        )
    } else {
        ScrollingPdfReader(
            filePath = book.filePath,
            pageCount = pageCount,
            currentPage = currentPage,
            onPageChange = onPageChange,
            colorFilter = colorFilter,
            modifier = modifier
        )
    }
}

@Composable
private fun PaginatedPdfReader(
    filePath: String,
    pageCount: Int,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        pageCount = { pageCount }
    )

    // Report swipes outward…
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChange(page)
        }
    }
    // …and follow external page changes (tap zones, volume buttons, scrubber).
    LaunchedEffect(currentPage) {
        val target = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (target != pagerState.currentPage) pagerState.animateScrollToPage(target)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        PdfPageView(
            filePath = filePath,
            pageIndex = page,
            colorFilter = colorFilter,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScrollingPdfReader(
    filePath: String,
    pageCount: Int,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            onPageChange(index)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = List(pageCount) { it }
        ) { _, pageIndex ->
            PdfPageView(
                filePath = filePath,
                pageIndex = pageIndex,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}
