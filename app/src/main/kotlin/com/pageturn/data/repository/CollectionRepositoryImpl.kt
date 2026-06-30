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

import com.pageturn.data.db.dao.CollectionDao
import com.pageturn.data.db.entity.BookCollectionCrossRef
import com.pageturn.data.db.entity.CollectionEntity
import com.pageturn.data.db.entity.CollectionWithBooks
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.Collection
import com.pageturn.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override fun getAllCollections(): Flow<List<Collection>> =
        collectionDao.getAllCollectionsWithBooks().map { list -> list.map { it.toCollection() } }

    override suspend fun insertCollection(collection: Collection) =
        collectionDao.insertCollection(collection.toEntity())

    override suspend fun updateCollection(collection: Collection) =
        collectionDao.updateCollection(collection.toEntity())

    override suspend fun deleteCollection(id: String) =
        collectionDao.deleteCollection(id)

    override suspend fun addBookToCollection(bookId: String, collectionId: String) =
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId = bookId, collectionId = collectionId))

    override suspend fun removeBookFromCollection(bookId: String, collectionId: String) =
        collectionDao.removeBookFromCollection(bookId = bookId, collectionId = collectionId)
}

private fun CollectionWithBooks.toCollection(): Collection = Collection(
    id = collection.id,
    name = collection.name,
    emoji = collection.emoji,
    createdAt = collection.createdAt,
    books = books.map { entity ->
        Book(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            filePath = entity.filePath,
            format = entity.format,
            coverPath = entity.coverPath,
            totalPages = entity.totalPages,
            currentPage = entity.currentPage,
            currentCfi = entity.currentCfi,
            progressPercent = entity.progressPercent,
            dateAdded = entity.dateAdded,
            lastOpened = entity.lastOpened,
            isFinished = entity.isFinished,
            isFavorite = entity.isFavorite,
            readingTimeMs = entity.readingTimeMs
        )
    }
)

private fun Collection.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    emoji = emoji,
    createdAt = createdAt
)
