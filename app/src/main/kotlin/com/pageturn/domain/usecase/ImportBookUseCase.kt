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

package com.pageturn.domain.usecase

import android.content.Context
import android.net.Uri
import com.pageturn.domain.model.Book
import com.pageturn.domain.repository.BookRepository
import com.pageturn.util.CoverExtractor
import com.pageturn.util.EpubMetadataExtractor
import com.pageturn.util.FileUtils
import com.pageturn.util.PdfMetadataExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed class ImportResult {
    data class Success(val bookId: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
    data class Progress(val step: String) : ImportResult()
    object Duplicate : ImportResult()
}

class ImportBookUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val fileUtils: FileUtils,
    private val coverExtractor: CoverExtractor,
    @ApplicationContext private val context: Context
) {
    operator fun invoke(uri: Uri, replaceIfDuplicate: Boolean = false): Flow<ImportResult> = flow {
        try {
            emit(ImportResult.Progress("Copying file..."))
            val (destPath, format) = fileUtils.copyToAppStorage(uri, context)

            val existingBook = bookRepository.getBookByPath(destPath)
            if (existingBook != null && !replaceIfDuplicate) {
                File(destPath).delete()
                emit(ImportResult.Duplicate)
                return@flow
            }

            emit(ImportResult.Progress("Extracting metadata..."))
            val metadata = when (format) {
                "epub" -> EpubMetadataExtractor().extractMetadata(destPath)
                "pdf" -> PdfMetadataExtractor().extractMetadata(destPath)
                else -> BookMetadata(title = File(destPath).nameWithoutExtension, author = null, totalPages = null)
            }

            emit(ImportResult.Progress("Extracting cover..."))
            val coversDir = File(context.filesDir, "covers")
            coversDir.mkdirs()
            val coverPath = coverExtractor.extractCover(destPath, format, coversDir)

            val bookId = existingBook?.id ?: UUID.randomUUID().toString()
            val book = Book(
                id = bookId,
                title = metadata.title,
                author = metadata.author,
                filePath = destPath,
                format = format,
                coverPath = coverPath,
                totalPages = metadata.totalPages,
                dateAdded = System.currentTimeMillis()
            )
            bookRepository.insertBook(book)
            emit(ImportResult.Success(bookId))
        } catch (e: Exception) {
            emit(ImportResult.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
}

data class BookMetadata(
    val title: String,
    val author: String?,
    val totalPages: Int?
)
