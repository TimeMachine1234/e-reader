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

import com.pageturn.domain.model.Collection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun insertCollection(collection: Collection)
    suspend fun updateCollection(collection: Collection)
    suspend fun deleteCollection(id: String)
    suspend fun addBookToCollection(bookId: String, collectionId: String)
    suspend fun removeBookFromCollection(bookId: String, collectionId: String)
}
