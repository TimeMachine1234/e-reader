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

package com.pageturn.domain.repository

import com.pageturn.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(sortBy: SortBy): Flow<List<Book>>
    fun getBook(id: String): Flow<Book?>
    fun searchBooks(query: String): Flow<List<Book>>
    fun getBooksByFormat(format: String): Flow<List<Book>>
    fun getUnreadBooks(): Flow<List<Book>>
    fun getInProgressBooks(): Flow<List<Book>>
    fun getFinishedBooks(): Flow<List<Book>>
    fun getFavoriteBooks(): Flow<List<Book>>
    suspend fun insertBook(book: Book)
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(id: String)
    suspend fun updateReadingProgress(id: String, page: Int, cfi: String?, progress: Float, deltaMs: Long)
    suspend fun getBookCount(): Int
    suspend fun getBookByPath(path: String): Book?
}

enum class SortBy {
    RECENTLY_OPENED, TITLE, AUTHOR, DATE_ADDED, PROGRESS
}
