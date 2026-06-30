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

package com.pageturn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pageturn.ui.collections.CollectionsScreen
import com.pageturn.ui.library.LibraryScreen
import com.pageturn.ui.reader.ReaderScreen
import com.pageturn.ui.settings.AppSettingsScreen

object AppDestinations {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val COLLECTIONS = "collections"
    const val SETTINGS = "settings"

    fun readerRoute(bookId: String) = "reader/$bookId"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AppDestinations.LIBRARY) {
        composable(AppDestinations.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(AppDestinations.readerRoute(bookId)) },
                onNavigateToCollections = { navController.navigate(AppDestinations.COLLECTIONS) },
                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS) }
            )
        }
        composable(
            route = AppDestinations.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.COLLECTIONS) {
            CollectionsScreen(
                onBookClick = { bookId -> navController.navigate(AppDestinations.readerRoute(bookId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.SETTINGS) {
            AppSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
