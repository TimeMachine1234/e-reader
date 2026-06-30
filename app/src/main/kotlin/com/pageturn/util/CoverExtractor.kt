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

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class CoverExtractor @Inject constructor() {
    fun extractCover(filePath: String, format: String, destDir: File): String? {
        return try {
            when (format) {
                "epub" -> extractEpubCover(filePath, destDir)
                "pdf" -> extractPdfCover(filePath, destDir)
                else -> null
            }
        } catch (e: Exception) { null }
    }

    private fun extractEpubCover(filePath: String, destDir: File): String? {
        val book = EpubReader().readEpub(FileInputStream(filePath))
        val coverImage = book.coverImage ?: return null
        val destFile = File(destDir, "${filePath.substringAfterLast("/").substringBeforeLast(".")}_cover.jpg")
        FileOutputStream(destFile).use { it.write(coverImage.data) }
        return destFile.absolutePath
    }

    private fun extractPdfCover(filePath: String, destDir: File): String? {
        val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        if (renderer.pageCount == 0) { renderer.close(); fd.close(); return null }
        val page = renderer.openPage(0)
        val scale = 300f / page.width.coerceAtLeast(1)
        val width = (page.width * scale).toInt().coerceAtLeast(1)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        fd.close()
        val destFile = File(destDir, "${filePath.substringAfterLast("/").substringBeforeLast(".")}_cover.jpg")
        FileOutputStream(destFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        return destFile.absolutePath
    }
}
