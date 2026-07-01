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

package com.pageturn.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.pageturn.proto.ReaderSettings
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

val defaultReaderSettings: ReaderSettings = ReaderSettings.newBuilder()
    .setFontFamily("georgia")
    .setFontSizeSp(18f)
    .setLineSpacing(1.6f)
    .setLetterSpacing(0f)
    .setParagraphSpacing(16f)
    .setJustifyText(true)
    .setBoldText(false)
    .setHorizontalMarginDp(32)
    .setVerticalPaddingDp(24)
    .setColumns(1)
    .setPaginateMode(true)
    .setPageTurnAnimation("slide")
    .setTheme("Light")
    .setCustomBgColor("")
    .setCustomTextColor("")
    .setBrightness(-1)
    .setShowProgressBar(true)
    .setShowChapterProgress(true)
    .setShowTimeRemaining(true)
    .setKeepScreenAwake(true)
    .setVolumeButtonPageTurn(true)
    .setTapZoneLayout("sides")
    .setDualPageLandscape(false)
    .setBlinkReminder(false)
    .setBlinkIntervalSec(60)
    .build()

object ReaderSettingsSerializer : Serializer<ReaderSettings> {
    override val defaultValue: ReaderSettings = defaultReaderSettings

    override suspend fun readFrom(input: InputStream): ReaderSettings {
        return try {
            ReaderSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw androidx.datastore.core.CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ReaderSettings, output: OutputStream) {
        t.writeTo(output)
    }
}

@Singleton
class ReaderSettingsDataStore @Inject constructor(
    private val dataStore: DataStore<ReaderSettings>
) {
    val settingsFlow: Flow<ReaderSettings> = dataStore.data

    suspend fun updateFontFamily(fontFamily: String) {
        dataStore.updateData { it.toBuilder().setFontFamily(fontFamily).build() }
    }

    suspend fun updateFontSize(sp: Float) {
        dataStore.updateData { it.toBuilder().setFontSizeSp(sp).build() }
    }

    suspend fun updateLineSpacing(spacing: Float) {
        dataStore.updateData { it.toBuilder().setLineSpacing(spacing).build() }
    }

    suspend fun updateLetterSpacing(spacing: Float) {
        dataStore.updateData { it.toBuilder().setLetterSpacing(spacing).build() }
    }

    suspend fun updateParagraphSpacing(spacing: Float) {
        dataStore.updateData { it.toBuilder().setParagraphSpacing(spacing).build() }
    }

    suspend fun updateJustifyText(justify: Boolean) {
        dataStore.updateData { it.toBuilder().setJustifyText(justify).build() }
    }

    suspend fun updateBoldText(bold: Boolean) {
        dataStore.updateData { it.toBuilder().setBoldText(bold).build() }
    }

    suspend fun updateHorizontalMargin(dp: Int) {
        dataStore.updateData { it.toBuilder().setHorizontalMarginDp(dp).build() }
    }

    suspend fun updateVerticalPadding(dp: Int) {
        dataStore.updateData { it.toBuilder().setVerticalPaddingDp(dp).build() }
    }

    suspend fun updateColumns(columns: Int) {
        dataStore.updateData { it.toBuilder().setColumns(columns).build() }
    }

    suspend fun updatePaginateMode(paginate: Boolean) {
        dataStore.updateData { it.toBuilder().setPaginateMode(paginate).build() }
    }

    suspend fun updatePageTurnAnimation(animation: String) {
        dataStore.updateData { it.toBuilder().setPageTurnAnimation(animation).build() }
    }

    suspend fun updateTheme(theme: String) {
        dataStore.updateData { it.toBuilder().setTheme(theme).build() }
    }

    suspend fun updateCustomBgColor(color: String) {
        dataStore.updateData { it.toBuilder().setCustomBgColor(color).build() }
    }

    suspend fun updateCustomTextColor(color: String) {
        dataStore.updateData { it.toBuilder().setCustomTextColor(color).build() }
    }

    suspend fun updateBrightness(brightness: Int) {
        dataStore.updateData { it.toBuilder().setBrightness(brightness).build() }
    }

    suspend fun updateShowProgressBar(show: Boolean) {
        dataStore.updateData { it.toBuilder().setShowProgressBar(show).build() }
    }

    suspend fun updateShowChapterProgress(show: Boolean) {
        dataStore.updateData { it.toBuilder().setShowChapterProgress(show).build() }
    }

    suspend fun updateShowTimeRemaining(show: Boolean) {
        dataStore.updateData { it.toBuilder().setShowTimeRemaining(show).build() }
    }

    suspend fun updateKeepScreenAwake(keep: Boolean) {
        dataStore.updateData { it.toBuilder().setKeepScreenAwake(keep).build() }
    }

    suspend fun updateVolumeButtonPageTurn(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setVolumeButtonPageTurn(enabled).build() }
    }

    suspend fun updateTapZoneLayout(layout: String) {
        dataStore.updateData { it.toBuilder().setTapZoneLayout(layout).build() }
    }

    suspend fun updateDualPageLandscape(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setDualPageLandscape(enabled).build() }
    }

    suspend fun updateBlinkReminder(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setBlinkReminder(enabled).build() }
    }

    suspend fun updateBlinkIntervalSec(seconds: Int) {
        dataStore.updateData { it.toBuilder().setBlinkIntervalSec(seconds).build() }
    }

    suspend fun resetToDefaults() {
        dataStore.updateData { defaultReaderSettings }
    }
}
