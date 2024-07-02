/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package ru.tech.imageresizershrinker.core.settings.presentation.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import ru.tech.imageresizershrinker.core.settings.domain.SimpleSettingInteractor
import ru.tech.imageresizershrinker.core.settings.presentation.model.EditPresetsController
import ru.tech.imageresizershrinker.core.settings.presentation.model.UiSettingsState

val LocalSettingsState =
    compositionLocalOf<UiSettingsState> { error("UiSettingsState not present") }

val LocalSimpleSettingInteractor =
    compositionLocalOf<SimpleSettingInteractor> { error("SimpleSettingInteractor not present") }

val LocalEditPresetsController =
    compositionLocalOf<EditPresetsController> { error("EditPresetsController not present") }

@Composable
fun rememberEditPresetsController(
    initialVisibility: Boolean = false
) = remember(initialVisibility) {
    EditPresetsController(initialVisibility)
}