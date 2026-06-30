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

import com.pageturn.domain.model.Book
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.SortBy
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

sealed class FilterBy {
    object None : FilterBy()
    data class Format(val format: String) : FilterBy()
    object Unread : FilterBy()
    object InProgress : FilterBy()
    object Finished : FilterBy()
    object Favorites : FilterBy()
    data class CollectionFilter(val collectionId: String) : FilterBy()
}

class GetLibraryUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(sortBy: SortBy = SortBy.RECENTLY_OPENED, filterBy: FilterBy = FilterBy.None): Flow<List<Book>> {
        return when (filterBy) {
            is FilterBy.None -> bookRepository.getAllBooks(sortBy)
            is FilterBy.Format -> bookRepository.getBooksByFormat(filterBy.format)
            is FilterBy.Unread -> bookRepository.getUnreadBooks()
            is FilterBy.InProgress -> bookRepository.getInProgressBooks()
            is FilterBy.Finished -> bookRepository.getFinishedBooks()
            is FilterBy.Favorites -> bookRepository.getFavoriteBooks()
            is FilterBy.CollectionFilter -> bookRepository.getAllBooks(sortBy)
        }
    }

    fun search(query: String): Flow<List<Book>> = bookRepository.searchBooks(query)
}
