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

package com.pageturn.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pageturn.data.db.dao.BookDao
import com.pageturn.data.db.dao.BookmarkDao
import com.pageturn.data.db.dao.CollectionDao
import com.pageturn.data.db.dao.HighlightDao
import com.pageturn.data.db.entity.BookCollectionCrossRef
import com.pageturn.data.db.entity.BookEntity
import com.pageturn.data.db.entity.BookmarkEntity
import com.pageturn.data.db.entity.CollectionEntity
import com.pageturn.data.db.entity.HighlightEntity

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val DATABASE_NAME = "pageturn_db"
    }
}
