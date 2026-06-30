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

import com.pageturn.data.db.dao.BookmarkDao
import com.pageturn.data.db.entity.BookmarkEntity
import com.pageturn.domain.model.Bookmark
import com.pageturn.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.getAllBookmarksForBook(bookId).map { list -> list.map { it.toBookmark() } }

    override suspend fun insertBookmark(bookmark: Bookmark) =
        bookmarkDao.insertBookmark(bookmark.toEntity())

    override suspend fun updateBookmark(bookmark: Bookmark) =
        bookmarkDao.updateBookmark(bookmark.toEntity())

    override suspend fun deleteBookmark(id: String) =
        bookmarkDao.deleteBookmark(id)

    override suspend fun deleteAllBookmarksForBook(bookId: String) =
        bookmarkDao.deleteAllBookmarksForBook(bookId)
}

private fun BookmarkEntity.toBookmark(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    cfi = cfi,
    page = page,
    label = label,
    note = note,
    createdAt = createdAt
)

private fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    cfi = cfi,
    page = page,
    label = label,
    note = note,
    createdAt = createdAt
)
