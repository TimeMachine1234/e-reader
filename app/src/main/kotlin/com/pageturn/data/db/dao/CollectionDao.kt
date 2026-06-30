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
import androidx.room.Transaction
import androidx.room.Update
import com.pageturn.data.db.entity.BookCollectionCrossRef
import com.pageturn.data.db.entity.CollectionEntity
import com.pageturn.data.db.entity.CollectionWithBooks
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Transaction
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)

    @Query("DELETE FROM book_collections WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBookFromCollection(bookId: String, collectionId: String)

    @Query(
        """
        SELECT c.* FROM collections c
        INNER JOIN book_collections bc ON c.id = bc.collectionId
        WHERE bc.bookId = :bookId
        ORDER BY c.createdAt DESC
        """
    )
    fun getCollectionsForBook(bookId: String): Flow<List<CollectionEntity>>
}
