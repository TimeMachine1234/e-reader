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

package com.pageturn.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.ui.theme.READER_THEMES
import kotlin.math.roundToInt

// (storage key, display label) — the key is what's persisted and consumed by the renderer.
private val FONT_FAMILIES = listOf(
    "georgia" to "Georgia",
    "palatino" to "Palatino",
    "opendyslexic" to "OpenDyslexic",
    "lato" to "Lato",
    "merriweather" to "Merriweather",
    "eb_garamond" to "EB Garamond",
    "system" to "System"
)

private val PAGE_TURN_ANIMATIONS = listOf(
    "slide" to "Slide",
    "curl" to "Curl",
    "fade" to "Fade",
    "none" to "None"
)

// (storage key, display label)
private val TAP_ZONE_LAYOUTS = listOf(
    "sides" to "Sides",
    "thirds" to "Thirds"
)

@Composable
fun SettingsDrawer(
    settings: ReaderSettings,
    format: String = "epub",
    onFontFamilyChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onParagraphSpacingChange: (Float) -> Unit,
    onJustifyTextChange: (Boolean) -> Unit,
    onBoldTextChange: (Boolean) -> Unit,
    onHorizontalMarginChange: (Int) -> Unit,
    onVerticalPaddingChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onPaginateModeChange: (Boolean) -> Unit,
    onDualPageLandscapeChange: (Boolean) -> Unit,
    onPageTurnAnimationChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onCustomBgColorChange: (String) -> Unit,
    onCustomTextColorChange: (String) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onShowProgressBarChange: (Boolean) -> Unit,
    onShowChapterProgressChange: (Boolean) -> Unit,
    onShowTimeRemainingChange: (Boolean) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onBlinkReminderChange: (Boolean) -> Unit,
    onBlinkIntervalChange: (Int) -> Unit,
    onVolumeButtonPageTurnChange: (Boolean) -> Unit,
    onTapZoneLayoutChange: (String) -> Unit,
    onResetDefaults: () -> Unit,
    isLandscape: Boolean = false,
    onClose: () -> Unit = {}
) {
    // Which settings actually affect each format. Hiding the rest avoids
    // confusing the reader with controls that do nothing for the open file.
    val fmt = format.lowercase()
    val isEpub = fmt == "epub"
    val isPdf = fmt == "pdf"
    val isTxt = fmt == "txt"
    val supportsTypography = isEpub || isTxt          // font size, spacing, justify, bold
    val supportsFontFamily = isEpub                   // bundled @font-face fonts
    val supportsMargins = isEpub || isTxt             // page margins/padding
    val supportsReadMode = isEpub || isPdf            // paginate vs scroll
    val supportsColumns = isEpub
    val supportsDualPage = isEpub
    val supportsAnimation = isEpub
    val supportsTheme = isEpub || isPdf || isTxt      // not comics (raw images)
    val supportsLayoutSection = supportsMargins || supportsReadMode ||
        supportsColumns || supportsDualPage || supportsAnimation

    val panelContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Reading Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Typography Section
            if (supportsTypography) {
                SectionHeader("Typography")

                if (supportsFontFamily) {
                    // Font family chips
                    SettingLabel("Font Family")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        FONT_FAMILIES.forEach { (key, label) ->
                            FilterChip(
                                selected = settings.fontFamily == key,
                                onClick = { onFontFamilyChange(key) },
                                label = {
                                    Text(
                                        text = label,
                                        fontFamily = when (key) {
                                            "georgia", "merriweather", "eb_garamond", "palatino" -> FontFamily.Serif
                                            else -> FontFamily.Default
                                        }
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                SliderSettingRow(
                    label = "Font Size",
                    value = settings.fontSizeSp,
                    valueRange = 12f..36f,
                    steps = 24,
                    displayText = "${settings.fontSizeSp.roundToInt()}sp",
                    onValueChangeFinished = onFontSizeChange
                )

                SliderSettingRow(
                    label = "Line Spacing",
                    value = settings.lineSpacing,
                    valueRange = 1f..2.5f,
                    steps = 0,
                    displayText = "%.1f".format(settings.lineSpacing),
                    onValueChangeFinished = onLineSpacingChange
                )

                SliderSettingRow(
                    label = "Letter Spacing",
                    value = settings.letterSpacing,
                    valueRange = -1f..5f,
                    steps = 0,
                    displayText = "%.1f".format(settings.letterSpacing),
                    onValueChangeFinished = onLetterSpacingChange
                )

                SliderSettingRow(
                    label = "Paragraph Spacing",
                    value = settings.paragraphSpacing,
                    valueRange = 0f..32f,
                    steps = 0,
                    displayText = "${settings.paragraphSpacing.roundToInt()}dp",
                    onValueChangeFinished = onParagraphSpacingChange
                )

                SwitchSettingRow(
                    label = "Justify Text",
                    checked = settings.justifyText,
                    onCheckedChange = onJustifyTextChange
                )

                SwitchSettingRow(
                    label = "Bold Text",
                    checked = settings.boldText,
                    onCheckedChange = onBoldTextChange
                )

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // Layout Section
            if (supportsLayoutSection) {
                SectionHeader("Layout")

                if (supportsMargins) {
                    SliderSettingRow(
                        label = "Horizontal Margin",
                        value = settings.horizontalMarginDp.toFloat(),
                        valueRange = 0f..80f,
                        steps = 0,
                        displayText = "${settings.horizontalMarginDp}dp",
                        onValueChangeFinished = { onHorizontalMarginChange(it.roundToInt()) }
                    )

                    SliderSettingRow(
                        label = "Vertical Padding",
                        value = settings.verticalPaddingDp.toFloat(),
                        valueRange = 8f..64f,
                        steps = 0,
                        displayText = "${settings.verticalPaddingDp}dp",
                        onValueChangeFinished = { onVerticalPaddingChange(it.roundToInt()) }
                    )
                }

                if (supportsColumns) {
                    SettingRowContainer(label = "Columns") {
                        SegmentedButtonRow(
                            options = listOf("1 Col", "2 Col"),
                            selectedIndex = settings.columns - 1,
                            onSelect = { onColumnsChange(it + 1) }
                        )
                    }
                }

                if (supportsReadMode) {
                    SettingRowContainer(label = "Read Mode") {
                        SegmentedButtonRow(
                            options = listOf("Paginate", "Scroll"),
                            selectedIndex = if (settings.paginateMode) 0 else 1,
                            onSelect = { onPaginateModeChange(it == 0) }
                        )
                    }
                }

                if (supportsDualPage) {
                    SwitchSettingRow(
                        label = "Two Pages in Landscape",
                        checked = settings.dualPageLandscape,
                        onCheckedChange = onDualPageLandscapeChange
                    )
                }

                if (supportsAnimation) {
                    // Page turn animation chips
                    SettingLabel("Page Turn Animation")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        PAGE_TURN_ANIMATIONS.forEach { (key, label) ->
                            FilterChip(
                                selected = settings.pageTurnAnimation == key,
                                onClick = { onPageTurnAnimationChange(key) },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // Appearance Section
            SectionHeader("Appearance")

            // Theme swatches
            if (supportsTheme) {
            SettingLabel("Theme")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                READER_THEMES.forEach { theme ->
                    val isSelected = settings.theme == theme.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(theme.backgroundColor)
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable { onThemeChange(theme.name) }
                        ) {
                            // Sample letter in the theme's text color, so the swatch
                            // previews actual contrast (not just the background).
                            Text(
                                text = "A",
                                color = theme.textColor,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            }

            // Brightness slider
            SliderSettingRow(
                label = "Brightness",
                value = if (settings.brightness == -1) 0.5f else settings.brightness / 100f,
                valueRange = 0f..1f,
                steps = 0,
                displayText = if (settings.brightness == -1) "System" else "${settings.brightness}%",
                onValueChangeFinished = { value ->
                    onBrightnessChange(if (value < 0.05f) -1 else (value * 100).roundToInt())
                }
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Reading Aids Section
            SectionHeader("Reading Aids")

            SwitchSettingRow(
                label = "Show Progress Bar",
                checked = settings.showProgressBar,
                onCheckedChange = onShowProgressBarChange
            )

            SwitchSettingRow(
                label = "Show Chapter Progress",
                checked = settings.showChapterProgress,
                onCheckedChange = onShowChapterProgressChange
            )

            SwitchSettingRow(
                label = "Show Time Remaining",
                checked = settings.showTimeRemaining,
                onCheckedChange = onShowTimeRemainingChange
            )

            SwitchSettingRow(
                label = "Keep Screen Awake",
                checked = settings.keepScreenAwake,
                onCheckedChange = onKeepScreenAwakeChange
            )

            SwitchSettingRow(
                label = "Blink Reminder",
                checked = settings.blinkReminder,
                onCheckedChange = onBlinkReminderChange
            )

            if (settings.blinkReminder) {
                SliderSettingRow(
                    label = "Blink Every",
                    value = settings.blinkIntervalSec.toFloat(),
                    valueRange = 20f..180f,
                    steps = 0,
                    displayText = "${settings.blinkIntervalSec}s",
                    onValueChangeFinished = { onBlinkIntervalChange(it.roundToInt()) }
                )
            }

            SwitchSettingRow(
                label = "Volume Button Page Turn",
                checked = settings.volumeButtonPageTurn,
                onCheckedChange = onVolumeButtonPageTurnChange
            )

            // Tap zone layout
            SettingRowContainer(label = "Tap Zone Layout") {
                SegmentedButtonRow(
                    options = TAP_ZONE_LAYOUTS.map { it.second },
                    selectedIndex = TAP_ZONE_LAYOUTS.indexOfFirst { it.first == settings.tapZoneLayout }
                        .coerceAtLeast(0),
                    onSelect = { onTapZoneLayoutChange(TAP_ZONE_LAYOUTS[it].first) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset button
            OutlinedButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Reset to Defaults")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (isLandscape) {
        panelContent()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            ) {
                panelContent()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SwitchSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: String,
    onValueChangeFinished: (Float) -> Unit
) {
    var sliderVal = value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChangeFinished(sliderVal) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingRowContainer(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

@Composable
private fun SegmentedButtonRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val shape = when {
                options.size == 1 -> RoundedCornerShape(50)
                index == 0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                index == options.size - 1 -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        shape
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
            if (index < options.size - 1) {
                Spacer(modifier = Modifier.width(0.dp))
            }
        }
    }
}
