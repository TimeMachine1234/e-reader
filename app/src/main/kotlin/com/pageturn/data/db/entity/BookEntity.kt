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

package com.pageturn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val format: String,       // "epub" | "pdf" | "txt" | "cbz" | "cbr"
    val coverPath: String?,
    val totalPages: Int?,
    val currentPage: Int = 0,
    val currentCfi: String? = null,
    val progressPercent: Float = 0f,
    val dateAdded: Long,
    val lastOpened: Long? = null,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    val readingTimeMs: Long = 0L
)
