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

package com.pageturn.domain.model

data class ReaderSettings(
    val fontFamily: String = "georgia",
    val fontSizeSp: Float = 18f,
    val lineSpacing: Float = 1.6f,
    val letterSpacing: Float = 0f,
    val paragraphSpacing: Float = 16f,
    val justifyText: Boolean = true,
    val boldText: Boolean = false,
    val horizontalMarginDp: Int = 32,
    val verticalPaddingDp: Int = 24,
    val columns: Int = 1,
    val paginateMode: Boolean = true,
    val pageTurnAnimation: String = "slide",
    val theme: String = "Light",
    val customBgColor: String = "",
    val customTextColor: String = "",
    val brightness: Int = -1,
    val showProgressBar: Boolean = true,
    val showChapterProgress: Boolean = true,
    val showTimeRemaining: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val volumeButtonPageTurn: Boolean = true,
    val tapZoneLayout: String = "sides"
) {
    companion object {
        val FONT_FAMILIES = listOf(
            "georgia",
            "palatino",
            "opendyslexic",
            "lato",
            "merriweather",
            "eb_garamond",
            "system"
        )
        val ANIMATIONS = listOf("slide", "curl", "fade", "none")
        val TAP_ZONES = listOf("sides", "thirds")
    }
}
