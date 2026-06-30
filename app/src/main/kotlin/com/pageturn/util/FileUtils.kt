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

package com.pageturn.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class FileUtils @Inject constructor() {
    fun copyToAppStorage(uri: Uri, context: Context): Pair<String, String> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val format = mimeTypeToFormat(mimeType)
        val booksDir = File(context.filesDir, "books").also { it.mkdirs() }
        val destFile = File(booksDir, "${UUID.randomUUID()}.$format")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
        return Pair(destFile.absolutePath, format)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getTotalBooksSize(filesDir: File): Long {
        val booksDir = File(filesDir, "books")
        return booksDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun mimeTypeToFormat(mimeType: String): String = when (mimeType) {
        "application/epub+zip" -> "epub"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "application/vnd.comicbook+zip", "application/zip" -> "cbz"
        "application/vnd.comicbook-rar", "application/x-rar-compressed" -> "cbr"
        else -> "epub"
    }

    companion object {
        val SUPPORTED_MIME_TYPES = arrayOf(
            "application/epub+zip",
            "application/pdf",
            "text/plain",
            "application/vnd.comicbook+zip",
            "application/vnd.comicbook-rar"
        )
    }
}
