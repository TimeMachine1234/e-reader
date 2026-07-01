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

package com.pageturn.ui.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfPageView(
    filePath: String,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val screenWidthPx = context.resources.displayMetrics.widthPixels

    val bitmapState = produceState<Bitmap?>(initialValue = null, filePath, pageIndex) {
        value = withContext(Dispatchers.IO) {
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext null

                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

                page = renderer.openPage(pageIndex)

                val renderScale = 2.0f * density
                val pageWidth = (page.width * renderScale).toInt()
                    .coerceAtMost(screenWidthPx * 2)
                val pageHeight = (page.height.toFloat() / page.width.toFloat() * pageWidth).toInt()

                val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                null
            } finally {
                page?.close()
                renderer?.close()
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
