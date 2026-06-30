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

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.pageturn.domain.usecase.BookMetadata
import java.io.File
import javax.inject.Inject

class PdfMetadataExtractor @Inject constructor() {
    fun extractMetadata(filePath: String): BookMetadata {
        val title = filePath.substringAfterLast("/").substringBeforeLast(".")
        val pageCount = try {
            val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) { null }
        return BookMetadata(title = title, author = null, totalPages = pageCount)
    }
}
