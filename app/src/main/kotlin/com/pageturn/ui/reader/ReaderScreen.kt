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

import android.app.Activity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.Highlight
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.ui.reader.components.BookmarkPanel
import com.pageturn.ui.reader.components.HighlightMenu
import com.pageturn.ui.reader.components.ReaderBottomBar
import com.pageturn.ui.reader.components.ReaderTopBar
import com.pageturn.ui.reader.components.SearchOverlay
import com.pageturn.ui.reader.components.SettingsDrawer
import com.pageturn.ui.reader.components.TableOfContents
import com.pageturn.ui.reader.components.TocEntry
import com.pageturn.ui.reader.epub.EpubReader
import com.pageturn.ui.reader.pdf.PdfReader
import com.pageturn.ui.theme.readerThemeByName
import kotlinx.coroutines.delay
import java.io.File
import java.util.zip.ZipFile

@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Immersive mode — expand content behind system bars
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    // Keep screen awake
    LaunchedEffect(uiState.readerSettings.keepScreenAwake) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        if (uiState.readerSettings.keepScreenAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Reading session tracking
    DisposableEffect(Unit) {
        viewModel.onReaderResumed()
        onDispose { viewModel.onReaderPaused() }
    }

    // Auto-hide chrome after a few seconds — but never while a panel/drawer is open.
    val anyPanelOpen = uiState.isSettingsOpen || uiState.isTocOpen ||
        uiState.isBookmarksOpen || uiState.isHighlightsOpen || uiState.isSearchOpen
    LaunchedEffect(uiState.isChromeVisible, anyPanelOpen) {
        if (uiState.isChromeVisible && !anyPanelOpen) {
            delay(5_000)
            viewModel.hideChrome()
        }
    }

    val theme = readerThemeByName(uiState.readerSettings.theme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .onKeyEvent { keyEvent ->
                if (uiState.readerSettings.volumeButtonPageTurn) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            viewModel.onPageChange(uiState.currentPage + 1, uiState.currentCfi)
                            true
                        }
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            val prev = (uiState.currentPage - 1).coerceAtLeast(0)
                            viewModel.onPageChange(prev, uiState.currentCfi)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "Error: ${uiState.error}",
                    color = theme.textColor,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }
            uiState.book != null -> {
                val book = uiState.book!!

                // Format-specific content
                when (book.format.lowercase()) {
                    "epub" -> EpubReader(book = book, uiState = uiState, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    "pdf"  -> PdfReader(
                        book = book,
                        currentPage = uiState.currentPage,
                        readerSettings = uiState.readerSettings,
                        onPageChange = { page -> viewModel.onPageChange(page) }
                    )
                    "txt"  -> TxtReader(book = book, uiState = uiState, viewModel = viewModel)
                    "cbz", "cbr" -> ComicReader(book = book, uiState = uiState, viewModel = viewModel)
                    else -> {
                        Text(
                            text = "Unsupported format: ${book.format}",
                            color = theme.textColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Tap zones overlay — EPUB handles its own tap + swipe gestures
                // inside the WebView (via JS), so the Compose overlay is only used
                // for the other formats to avoid intercepting their gestures.
                if (book.format.lowercase() != "epub") {
                    TapZoneOverlay(
                        tapZoneLayout = uiState.readerSettings.tapZoneLayout,
                        onLeftTap = {
                            val prev = (uiState.currentPage - 1).coerceAtLeast(0)
                            viewModel.onPageChange(prev, uiState.currentCfi)
                        },
                        onRightTap = {
                            viewModel.onPageChange(uiState.currentPage + 1, uiState.currentCfi)
                        },
                        onCenterTap = { viewModel.toggleChrome() }
                    )
                }

                // Top chrome
                AnimatedVisibility(
                    visible = uiState.isChromeVisible,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ReaderTopBar(
                        book = book,
                        onBack = onBack,
                        onToc = { viewModel.toggleToc() },
                        onSearch = { viewModel.openSearch() },
                        onBookmarks = { viewModel.toggleBookmarks() },
                        onHighlights = { viewModel.toggleHighlights() },
                        onSettings = { viewModel.toggleSettings() }
                    )
                }

                // Bottom chrome
                AnimatedVisibility(
                    visible = uiState.isChromeVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomBar(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        progressPercent = uiState.progressPercent,
                        currentChapter = uiState.currentChapter,
                        readerSettings = uiState.readerSettings,
                        onPageScrub = { progress ->
                            val page = (progress * uiState.totalPages.coerceAtLeast(1)).toInt()
                            viewModel.onPageChange(page)
                        },
                        onSettingsClick = { viewModel.toggleSettings() }
                    )
                }

                // Settings drawer
                if (uiState.isSettingsOpen) {
                    SettingsDrawer(
                        settings = uiState.readerSettings,
                        onClose = { viewModel.closeSettings() },
                        onFontSizeChange = { viewModel.updateFontSize(it) },
                        onFontFamilyChange = { viewModel.updateFontFamily(it) },
                        onThemeChange = { viewModel.updateTheme(it) },
                        onLineSpacingChange = { viewModel.updateLineSpacing(it) },
                        onLetterSpacingChange = { viewModel.updateLetterSpacing(it) },
                        onParagraphSpacingChange = { viewModel.updateParagraphSpacing(it) },
                        onJustifyTextChange = { viewModel.updateJustifyText(it) },
                        onBoldTextChange = { viewModel.updateBoldText(it) },
                        onHorizontalMarginChange = { viewModel.updateHorizontalMargin(it) },
                        onVerticalPaddingChange = { viewModel.updateVerticalPadding(it) },
                        onColumnsChange = { viewModel.updateColumns(it) },
                        onPaginateModeChange = { viewModel.updatePaginateMode(it) },
                        onPageTurnAnimationChange = { viewModel.updatePageTurnAnimation(it) },
                        onBrightnessChange = { viewModel.updateBrightness(it) },
                        onShowProgressBarChange = { viewModel.updateShowProgressBar(it) },
                        onShowChapterProgressChange = { viewModel.updateShowChapterProgress(it) },
                        onShowTimeRemainingChange = { viewModel.updateShowTimeRemaining(it) },
                        onKeepScreenAwakeChange = { viewModel.updateKeepScreenAwake(it) },
                        onVolumeButtonPageTurnChange = { viewModel.updateVolumeButtonPageTurn(it) },
                        onTapZoneLayoutChange = { viewModel.updateTapZoneLayout(it) },
                        onResetDefaults = { viewModel.resetSettings() },
                        onCustomBgColorChange = { },
                        onCustomTextColorChange = { }
                    )
                }

                // Table of contents
                if (uiState.isTocOpen) {
                    TableOfContents(
                        entries = emptyList(),
                        currentIndex = uiState.currentPage,
                        onEntryClick = { viewModel.closeToc() },
                        onClose = { viewModel.closeToc() }
                    )
                }

                // Bookmarks panel
                if (uiState.isBookmarksOpen) {
                    BookmarkPanel(
                        bookmarks = uiState.bookmarks,
                        onBookmarkClick = { bookmark -> viewModel.jumpToBookmark(bookmark) },
                        onBookmarkDelete = { id -> viewModel.deleteBookmark(id) },
                        onAddBookmark = { label -> viewModel.addBookmark(label = label) },
                        onClose = { viewModel.closeBookmarks() }
                    )
                }

                // Highlights panel — shows saved highlights list
                if (uiState.isHighlightsOpen) {
                    HighlightsPanel(
                        highlights = uiState.highlights,
                        onClose = { viewModel.closeHighlights() },
                        onDelete = { viewModel.deleteHighlight(it) }
                    )
                }

                // Search overlay
                if (uiState.isSearchOpen) {
                    SearchOverlay(
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSearch = { viewModel.onSearchQueryChange(it) },
                        onResultClick = { viewModel.onSearchResultSelected(it) },
                        onClose = { viewModel.closeSearch() }
                    )
                }

                // Inline highlight menu (text selection)
                if (uiState.showHighlightMenu) {
                    HighlightMenu(
                        selectedText = uiState.selectedText,
                        position = uiState.highlightMenuPosition,
                        onHighlight = { color ->
                            viewModel.addHighlight(
                                uiState.selectedText, color, uiState.currentCfi, uiState.currentPage
                            )
                            viewModel.dismissHighlightMenu()
                        },
                        onAddNote = { },
                        onCopy = { },
                        onShare = { },
                        onDelete = { viewModel.dismissHighlightMenu() },
                        onDismiss = { viewModel.dismissHighlightMenu() }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tap zone overlay
// ---------------------------------------------------------------------------

@Composable
private fun TapZoneOverlay(
    tapZoneLayout: String,
    onLeftTap: () -> Unit,
    onRightTap: () -> Unit,
    onCenterTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(tapZoneLayout) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val x = offset.x
                    when (tapZoneLayout) {
                        "thirds" -> {
                            when {
                                x < width / 3f -> onLeftTap()
                                x > width * 2f / 3f -> onRightTap()
                                else -> onCenterTap()
                            }
                        }
                        else -> { // "sides"
                            when {
                                x < width * 0.25f -> onLeftTap()
                                x > width * 0.75f -> onRightTap()
                                else -> onCenterTap()
                            }
                        }
                    }
                }
            }
    )
}

// ---------------------------------------------------------------------------
// TxtReader (inline)
// ---------------------------------------------------------------------------

@Composable
private fun TxtReader(
    book: Book,
    uiState: ReaderUiState,
    viewModel: ReaderViewModel
) {
    val theme = readerThemeByName(uiState.readerSettings.theme)
    val settings = uiState.readerSettings
    val scrollState = rememberLazyListState()

    val text = remember(book.filePath) {
        runCatching { File(book.filePath).readText() }.getOrElse { "Unable to read file: ${it.message}" }
    }

    // Report total pages as line chunks
    val chunkSize = 3000
    val chunks = remember(text) {
        if (text.length <= chunkSize) listOf(text)
        else text.chunked(chunkSize)
    }

    LaunchedEffect(chunks.size) {
        viewModel.onTotalPagesResolved(chunks.size)
    }

    // Restore page position
    LaunchedEffect(uiState.currentPage, chunks.size) {
        if (chunks.isNotEmpty()) {
            val index = uiState.currentPage.coerceIn(0, chunks.size - 1)
            scrollState.animateScrollToItem(index)
        }
    }

    SelectionContainer {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(
                    horizontal = settings.horizontalMarginDp.dp,
                    vertical = settings.verticalPaddingDp.dp
                )
        ) {
            items(chunks.size) { index ->
                Text(
                    text = chunks[index],
                    style = TextStyle(
                        color = theme.textColor,
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * settings.lineSpacing).sp,
                        letterSpacing = settings.letterSpacing.sp,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                        textAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = settings.paragraphSpacing.dp)
                )
            }
        }
    }

    // Track page as user scrolls
    LaunchedEffect(scrollState.firstVisibleItemIndex) {
        val visibleIndex = scrollState.firstVisibleItemIndex
        if (visibleIndex != uiState.currentPage) {
            viewModel.onPageChange(visibleIndex)
        }
    }
}

// ---------------------------------------------------------------------------
// ComicReader (inline) — supports CBZ only; CBR falls back to a message
// ---------------------------------------------------------------------------

@Composable
private fun ComicReader(
    book: Book,
    uiState: ReaderUiState,
    viewModel: ReaderViewModel
) {
    val theme = readerThemeByName(uiState.readerSettings.theme)

    val imageFiles = remember(book.filePath) {
        runCatching {
            if (book.format.lowercase() == "cbz") {
                ZipFile(book.filePath).use { zip ->
                    zip.entries().asSequence()
                        .filter { !it.isDirectory }
                        .filter { entry ->
                            val name = entry.name.lowercase()
                            name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                name.endsWith(".png") || name.endsWith(".webp")
                        }
                        .sortedBy { it.name }
                        .map { entry ->
                            val tmpFile = File.createTempFile("comic_", "_${entry.name.substringAfterLast('/')}")
                            tmpFile.deleteOnExit()
                            zip.getInputStream(entry).use { input ->
                                tmpFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            tmpFile
                        }
                        .toList()
                }
            } else emptyList()
        }.getOrElse { emptyList() }
    }

    LaunchedEffect(imageFiles.size) {
        if (imageFiles.isNotEmpty()) viewModel.onTotalPagesResolved(imageFiles.size)
    }

    if (imageFiles.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(theme.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (book.format.lowercase() == "cbr")
                    "CBR format requires additional library support."
                else
                    "No images found in comic archive.",
                color = theme.textColor,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage.coerceIn(0, imageFiles.size - 1),
        pageCount = { imageFiles.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            viewModel.onPageChange(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val file = imageFiles[page]
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = rememberFilePainter(file),
                contentDescription = "Page ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HighlightsPanel — minimal panel listing saved highlights
// ---------------------------------------------------------------------------

@Composable
private fun HighlightsPanel(
    highlights: List<Highlight>,
    onClose: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Highlights") },
        text = {
            if (highlights.isEmpty()) {
                Text("No highlights yet")
            } else {
                androidx.compose.foundation.layout.Column {
                    highlights.forEach { highlight ->
                        Text(
                            text = highlight.selectedText,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        }
    )
}

/**
 * Simple painter that loads an image from a [File]. Falls back to an empty painter on error.
 */
@Composable
private fun rememberFilePainter(file: File): androidx.compose.ui.graphics.painter.Painter {
    val bitmap = remember(file.absolutePath) {
        runCatching {
            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }
    return if (bitmap != null) {
        androidx.compose.ui.graphics.painter.BitmapPainter(
            bitmap.asImageBitmap()
        )
    } else {
        androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray)
    }
}
