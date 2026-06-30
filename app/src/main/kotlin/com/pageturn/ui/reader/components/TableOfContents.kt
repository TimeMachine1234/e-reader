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

package com.pageturn.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TocEntry(
    val title: String,
    val href: String,
    val level: Int,
    val index: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContents(
    entries: List<TocEntry>,
    currentIndex: Int,
    onEntryClick: (TocEntry) -> Unit,
    isLandscape: Boolean = false,
    onClose: () -> Unit = {}
) {
    if (isLandscape) {
        TocSidePanel(
            entries = entries,
            currentIndex = currentIndex,
            onEntryClick = onEntryClick,
            onClose = onClose
        )
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState
        ) {
            TocContent(
                entries = entries,
                currentIndex = currentIndex,
                onEntryClick = { entry ->
                    onEntryClick(entry)
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun TocSidePanel(
    entries: List<TocEntry>,
    currentIndex: Int,
    onEntryClick: (TocEntry) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Contents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        HorizontalDivider()
        TocContent(
            entries = entries,
            currentIndex = currentIndex,
            onEntryClick = onEntryClick
        )
    }
}

@Composable
private fun TocContent(
    entries: List<TocEntry>,
    currentIndex: Int,
    onEntryClick: (TocEntry) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        val idx = entries.indexOfFirst { it.index == currentIndex }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(entries) { _, entry ->
            val isActive = entry.index == currentIndex
            ListItem(
                headlineContent = {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (entry.level * 16).dp)
                    .clickable { onEntryClick(entry) }
            )
        }
    }
}
