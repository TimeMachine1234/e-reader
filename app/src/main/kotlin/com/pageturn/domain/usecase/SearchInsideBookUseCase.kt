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

import com.pageturn.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class SearchResult(
    val snippet: String,
    val location: String,
    val page: Int?,
    val cfi: String?
)

class SearchInsideBookUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    // The actual search for EPUB is done via JS in the WebView (EpubJsBridge).
    // For PDF the ReaderViewModel calls PdfRenderer text extraction.
    // This use case is a pass-through coordinator placeholder.
    operator fun invoke(bookId: String, query: String): Flow<List<SearchResult>> = flow {
        emit(emptyList())
    }
}
