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

@file:Suppress("KotlinConstantConditions")

package ru.tech.imageresizershrinker.feature.main.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.resources.BuildConfig
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.model.isFirstLaunch
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.utils.helper.AppVersionPreRelease
import ru.tech.imageresizershrinker.core.ui.utils.helper.ProvidesValue
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedIconButton
import ru.tech.imageresizershrinker.core.ui.widget.modifier.pulsate
import ru.tech.imageresizershrinker.core.ui.widget.modifier.rotateAnimation
import ru.tech.imageresizershrinker.core.ui.widget.modifier.scaleOnTap
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBar
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBarType
import ru.tech.imageresizershrinker.core.ui.widget.other.TopAppBarEmoji
import ru.tech.imageresizershrinker.core.ui.widget.text.marquee

@Composable
internal fun MainTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onShowSnowfall: () -> Unit,
    onNavigateToSettings: () -> Unit,
    sideSheetState: DrawerState,
    isSheetSlideable: Boolean
) {
    val scope = rememberCoroutineScope()
    val settingsState = LocalSettingsState.current

    EnhancedTopAppBar(
        type = EnhancedTopAppBarType.Large,
        title = {
            LocalLayoutDirection.ProvidesValue(LayoutDirection.Ltr) {
                val titleText = remember {
                    "${Screen.FEATURES_COUNT}".plus(
                        if (BuildConfig.FLAVOR == "market") {
                            AppVersionPreRelease
                        } else {
                            " ${BuildConfig.FLAVOR.uppercase()} $AppVersionPreRelease"
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.marquee()
                ) {
                    Text(stringResource(R.string.app_name))
                    Badge(
                        content = {
                            Text(titleText)
                        },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .padding(bottom = 12.dp)
                            .scaleOnTap {
                                onShowSnowfall()
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    TopAppBarEmoji()
                }
            }
        },
        actions = {
            if (isSheetSlideable || settingsState.useFullscreenSettings) {
                EnhancedIconButton(
                    containerColor = Color.Transparent,
                    contentColor = LocalContentColor.current,
                    enableAutoShadowAndBorder = false,
                    onClick = {
                        if (settingsState.useFullscreenSettings) {
                            onNavigateToSettings()
                        } else {
                            scope.launch {
                                sideSheetState.open()
                            }
                        }
                    },
                    modifier = Modifier
                        .pulsate(
                            range = 0.95f..1.2f,
                            enabled = settingsState.isFirstLaunch()
                        )
                        .rotateAnimation(enabled = settingsState.isFirstLaunch())
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}