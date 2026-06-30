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

package com.pageturn.di

import com.pageturn.data.repository.BookRepositoryImpl
import com.pageturn.data.repository.CollectionRepositoryImpl
import com.pageturn.data.repository.HighlightRepositoryImpl
import com.pageturn.domain.repository.BookRepository
import com.pageturn.domain.repository.CollectionRepository
import com.pageturn.domain.repository.HighlightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository
    @Binds @Singleton abstract fun bindHighlightRepository(impl: HighlightRepositoryImpl): HighlightRepository
    @Binds @Singleton abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}
