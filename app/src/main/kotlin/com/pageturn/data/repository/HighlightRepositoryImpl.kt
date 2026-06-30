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

package com.pageturn.data.repository

import com.pageturn.data.db.dao.HighlightDao
import com.pageturn.data.db.entity.HighlightEntity
import com.pageturn.domain.model.Highlight
import com.pageturn.domain.repository.HighlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HighlightRepositoryImpl @Inject constructor(
    private val highlightDao: HighlightDao
) : HighlightRepository {

    override fun getHighlightsForBook(bookId: String): Flow<List<Highlight>> =
        highlightDao.getAllHighlightsForBook(bookId).map { list -> list.map { it.toHighlight() } }

    override fun getHighlightCount(bookId: String): Flow<Int> =
        highlightDao.getHighlightCount(bookId)

    override suspend fun insertHighlight(highlight: Highlight) =
        highlightDao.insertHighlight(highlight.toEntity())

    override suspend fun updateHighlight(highlight: Highlight) =
        highlightDao.updateHighlight(highlight.toEntity())

    override suspend fun deleteHighlight(id: String) =
        highlightDao.deleteHighlight(id)

    override suspend fun deleteAllHighlightsForBook(bookId: String) =
        highlightDao.deleteAllHighlightsForBook(bookId)
}

private fun HighlightEntity.toHighlight(): Highlight = Highlight(
    id = id,
    bookId = bookId,
    cfi = cfi,
    page = page,
    selectedText = selectedText,
    color = color,
    note = note,
    createdAt = createdAt
)

private fun Highlight.toEntity(): HighlightEntity = HighlightEntity(
    id = id,
    bookId = bookId,
    cfi = cfi,
    page = page,
    selectedText = selectedText,
    color = color,
    note = note,
    createdAt = createdAt
)
