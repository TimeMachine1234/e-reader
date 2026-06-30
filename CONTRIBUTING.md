# Contributing to PageTurn

Thank you for your interest in contributing to PageTurn! This document explains how to get the project building locally and outlines the conventions we follow.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Iguana (2023.2.1) or later |
| JDK | 17 (Temurin recommended) |
| Android SDK | API 35 (compile), API 26 minimum |
| Gradle | Bundled via wrapper (no separate install needed) |

---

## Building the project

1. **Clone the repository**
   ```bash
   git clone https://github.com/aravgandhi/e-reader.git
   cd e-reader
   ```

2. **Open in Android Studio**
   - Choose *File → Open* and select the cloned directory.
   - Android Studio will detect the Gradle project automatically.

3. **Sync Gradle**
   - Click *Sync Now* in the notification bar, or go to *File → Sync Project with Gradle Files*.
   - All dependencies will be downloaded from Maven Central, Google, and JitPack.

4. **Run the app**
   - Create a tablet AVD (Pixel Tablet or similar) targeting API 26 or higher.
   - Select the AVD from the device dropdown and click *Run*.
   - The app also runs on physical tablets and large-screen phones.

---

## Font assets

PageTurn bundles several typefaces that are **not checked into the repository** due to licensing constraints. Before building you must manually copy the following files into `app/src/main/assets/fonts/`:

| File | Font |
|------|------|
| `Georgia.ttf` | Georgia |
| `Palatino.ttf` | Palatino Linotype |
| `OpenDyslexic-Regular.ttf` | OpenDyslexic (available at opendyslexic.org) |
| `Lato-Regular.ttf` | Lato (available at Google Fonts) |
| `Merriweather-Regular.ttf` | Merriweather (available at Google Fonts) |
| `EBGaramond-Regular.ttf` | EB Garamond (available at Google Fonts) |

Fonts that require a commercial license (Georgia, Palatino) must be sourced from a licensed copy of Windows or macOS. The app falls back to the system default serif if a font file is missing.

---

## Running tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or running emulator)
./gradlew connectedAndroidTest
```

---

## Code style

- **Language**: Kotlin only — no Java source files in new code.
- **Style guide**: Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) (`kotlin.code.style=official` is set in `gradle.properties`).
- **Formatting**: We use [ktlint](https://pinterest.github.io/ktlint/). Run `./gradlew ktlintCheck` before opening a PR and fix any violations with `./gradlew ktlintFormat`.
- **No hardcoded strings**: All user-visible text must live in `app/src/main/res/values/strings.xml`. Do not put string literals directly in Composables or elsewhere.
- **No hardcoded colors**: Define colors in `ui/theme/Color.kt` and reference them via `MaterialTheme.colorScheme`. Do not use `Color(0xFF…)` inline in UI code.
- **No hardcoded dimensions**: Define spacing/sizing constants in `ui/theme/Dimens.kt`.
- **Dependency injection**: Use Hilt for all injection points. Do not instantiate ViewModels, repositories, or use-cases directly.
- **Architecture**: Follow MVVM + Clean Architecture layering (data → domain → presentation). Keep `@Composable` functions free of business logic.

---

## Pull request guidelines

- **One feature or fix per PR** — keep diffs focused and reviewable.
- **Branch naming**: `feature/<short-description>`, `fix/<short-description>`, or `chore/<short-description>`.
- **Test coverage**: Include unit tests for new use-cases and repository methods. UI logic tests (ViewModel) are strongly encouraged.
- **Strings**: Any new UI text must be added to `strings.xml` in the same PR.
- **License header**: Every new Kotlin source file must begin with the Apache 2.0 header:

  ```kotlin
  /*
   * Copyright 2024 PageTurn Contributors
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     https://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  ```

- **CI**: All PRs must pass the GitHub Actions CI pipeline (lint + debug build) before merging.
- **Changelog**: Update `CHANGELOG.md` if your change is user-facing.

---

## License

By contributing you agree that your contributions will be licensed under the [Apache License, Version 2.0](LICENSE).
