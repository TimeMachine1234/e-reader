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

import com.pageturn.data.db.dao.BookDao
import com.pageturn.data.db.entity.BookEntity
import com.pageturn.domain.model.Book
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.SortBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooks(sortBy: SortBy): Flow<List<Book>> {
        val flow = when (sortBy) {
            SortBy.RECENTLY_OPENED -> bookDao.getAllBooksByLastOpened()
            SortBy.TITLE -> bookDao.getAllBooksByTitle()
            SortBy.AUTHOR -> bookDao.getAllBooksByAuthor()
            SortBy.DATE_ADDED -> bookDao.getAllBooksByDateAdded()
            SortBy.PROGRESS -> bookDao.getAllBooksByProgress()
        }
        return flow.map { list -> list.map { it.toBook() } }
    }

    override fun getBook(id: String): Flow<Book?> =
        bookDao.getBookById(id).map { it?.toBook() }

    override fun searchBooks(query: String): Flow<List<Book>> =
        bookDao.searchBooks(query).map { list -> list.map { it.toBook() } }

    override fun getBooksByFormat(format: String): Flow<List<Book>> =
        bookDao.getBooksByFormat(format).map { list -> list.map { it.toBook() } }

    override fun getUnreadBooks(): Flow<List<Book>> =
        bookDao.getUnreadBooks().map { list -> list.map { it.toBook() } }

    override fun getInProgressBooks(): Flow<List<Book>> =
        bookDao.getInProgressBooks().map { list -> list.map { it.toBook() } }

    override fun getFinishedBooks(): Flow<List<Book>> =
        bookDao.getFinishedBooks().map { list -> list.map { it.toBook() } }

    override fun getFavoriteBooks(): Flow<List<Book>> =
        bookDao.getFavoriteBooks().map { list -> list.map { it.toBook() } }

    override suspend fun insertBook(book: Book) =
        bookDao.insertBook(book.toEntity())

    override suspend fun updateBook(book: Book) =
        bookDao.updateBook(book.toEntity())

    override suspend fun deleteBook(id: String) =
        bookDao.deleteBook(id)

    override suspend fun updateReadingProgress(
        id: String,
        page: Int,
        cfi: String?,
        progress: Float,
        deltaMs: Long
    ) {
        bookDao.updateReadingProgress(
            id = id,
            page = page,
            cfi = cfi,
            progress = progress,
            timestamp = System.currentTimeMillis(),
            deltaMs = deltaMs
        )
    }

    override suspend fun getBookCount(): Int = bookDao.getBookCount()

    override suspend fun getBookByPath(path: String): Book? =
        bookDao.getBookByPath(path)?.toBook()
}

private fun BookEntity.toBook(): Book = Book(
    id = id,
    title = title,
    author = author,
    filePath = filePath,
    format = format,
    coverPath = coverPath,
    totalPages = totalPages,
    currentPage = currentPage,
    currentCfi = currentCfi,
    progressPercent = progressPercent,
    dateAdded = dateAdded,
    lastOpened = lastOpened,
    isFinished = isFinished,
    isFavorite = isFavorite,
    readingTimeMs = readingTimeMs
)

private fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    filePath = filePath,
    format = format,
    coverPath = coverPath,
    totalPages = totalPages,
    currentPage = currentPage,
    currentCfi = currentCfi,
    progressPercent = progressPercent,
    dateAdded = dateAdded,
    lastOpened = lastOpened,
    isFinished = isFinished,
    isFavorite = isFavorite,
    readingTimeMs = readingTimeMs
)
