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

package com.pageturn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pageturn.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY CASE WHEN lastOpened IS NULL THEN 1 ELSE 0 END, lastOpened DESC")
    fun getAllBooksByLastOpened(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooksByTitle(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY author ASC")
    fun getAllBooksByAuthor(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooksByDateAdded(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY progressPercent DESC")
    fun getAllBooksByProgress(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE format = :format ORDER BY CASE WHEN lastOpened IS NULL THEN 1 ELSE 0 END, lastOpened DESC")
    fun getBooksByFormat(format: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFinished = 0 AND progressPercent = 0 ORDER BY dateAdded DESC")
    fun getUnreadBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFinished = 0 AND progressPercent > 0 ORDER BY lastOpened DESC")
    fun getInProgressBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFinished = 1 ORDER BY lastOpened DESC")
    fun getFinishedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastOpened DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("UPDATE books SET currentPage = :page, currentCfi = :cfi, progressPercent = :progress, lastOpened = :timestamp, readingTimeMs = readingTimeMs + :deltaMs WHERE id = :id")
    suspend fun updateReadingProgress(id: String, page: Int, cfi: String?, progress: Float, timestamp: Long, deltaMs: Long)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM books WHERE filePath = :path LIMIT 1")
    suspend fun getBookByPath(path: String): BookEntity?
}
