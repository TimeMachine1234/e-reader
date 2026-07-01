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

package com.pageturn.ui.theme

import androidx.compose.ui.graphics.Color

data class ReaderTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color
)

val READER_THEMES = listOf(
    ReaderTheme("Light",     Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFF4A90D9)),
    // A real e-ink display reads as a distinct light *gray*, not bright white.
    // Warm carbon-gray paper with near-black text to cut glare/eye strain.
    ReaderTheme("E-Ink",     Color(0xFFC7C4BB), Color(0xFF1A1917), Color(0xFF565650)),
    // Warmer, amber-tinted paper for low-light / stronger blue-light reduction.
    ReaderTheme("E-Ink Warm", Color(0xFFD3C9B4), Color(0xFF221E17), Color(0xFF7A6A4E)),
    ReaderTheme("Sepia",     Color(0xFFF5ECD7), Color(0xFF3B2F1E), Color(0xFF8B5E3C)),
    ReaderTheme("Dark",      Color(0xFF1C1C1E), Color(0xFFE5E5E7), Color(0xFF5E9FE0)),
    ReaderTheme("AMOLED",    Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF5E9FE0)),
    ReaderTheme("Forest",    Color(0xFF1B2A1E), Color(0xFFC8E6C9), Color(0xFF81C784)),
    ReaderTheme("Ocean",     Color(0xFF0D1B2A), Color(0xFFB3CDE0), Color(0xFF5B9EC9)),
    ReaderTheme("Warm Gray", Color(0xFFF0EDE8), Color(0xFF2D2926), Color(0xFF9B7653)),
)

fun readerThemeByName(name: String): ReaderTheme =
    READER_THEMES.find { it.name == name } ?: READER_THEMES.first()

fun highlightColorToCompose(colorName: String): Color = when (colorName) {
    "yellow" -> Color(0xFFFFEB3B)
    "green"  -> Color(0xFF4CAF50)
    "pink"   -> Color(0xFFE91E63)
    "blue"   -> Color(0xFF2196F3)
    else     -> Color(0xFFFFEB3B)
}
