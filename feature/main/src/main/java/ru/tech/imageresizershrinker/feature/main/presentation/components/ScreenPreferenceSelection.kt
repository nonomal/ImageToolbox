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

package ru.tech.imageresizershrinker.feature.main.presentation.components

import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.outlined.ContentPasteOff
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.utils.helper.clipList
import ru.tech.imageresizershrinker.core.ui.utils.helper.rememberClipboardData
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.utils.provider.LocalWindowSizeClass
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedFloatingActionButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedFloatingActionButtonType
import ru.tech.imageresizershrinker.core.ui.widget.other.BoxAnimatedVisibility
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.preferences.PreferenceItemOverload

@Composable
internal fun RowScope.ScreenPreferenceSelection(
    currentScreenList: List<Screen>,
    showScreenSearch: Boolean,
    screenSearchKeyword: String,
    isGrid: Boolean,
    isSheetSlideable: Boolean,
    onGetClipList: (List<Uri>) -> Unit,
    onNavigateToScreenWithPopUpTo: (Screen) -> Unit,
    onChangeShowScreenSearch: (Boolean) -> Unit,
    showNavRail: Boolean,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsState = LocalSettingsState.current
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val canSearchScreens = settingsState.screensSearchEnabled

    val compactHeight =
        LocalWindowSizeClass.current.heightSizeClass == WindowHeightSizeClass.Compact

    AnimatedContent(
        modifier = Modifier
            .weight(1f)
            .widthIn(min = 1.dp),
        targetState = currentScreenList.isNotEmpty(),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }
    ) { hasScreens ->
        if (hasScreens) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val clipboardData by rememberClipboardData()
                val allowAutoPaste = settingsState.allowAutoClipboardPaste
                val showClipButton = clipboardData.isNotEmpty() || !allowAutoPaste
                val showSearchButton = !showScreenSearch && canSearchScreens

                LazyVerticalStaggeredGrid(
                    reverseLayout = showScreenSearch && screenSearchKeyword.isNotEmpty() && canSearchScreens,
                    modifier = Modifier.fillMaxSize(),
                    columns = StaggeredGridCells.Adaptive(220.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.CenterHorizontally
                    ),
                    contentPadding = PaddingValues(
                        bottom = 12.dp + if (isGrid) {
                            WindowInsets
                                .navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding() + if (!compactHeight) {
                                128.dp
                            } else 0.dp
                        } else {
                            0.dp
                        } + showClipButton.let {
                            if (it) 76.dp else 0.dp
                        } + showSearchButton.let {
                            if (it && showClipButton) 48.dp else if (!showClipButton) 76.dp else 0.dp
                        },
                        top = 12.dp,
                        end = 12.dp + if (isSheetSlideable) {
                            cutout.calculateEndPadding(
                                LocalLayoutDirection.current
                            )
                        } else 0.dp,
                        start = 12.dp + if (!showNavRail) {
                            cutout.calculateStartPadding(
                                LocalLayoutDirection.current
                            )
                        } else 0.dp
                    ),
                    content = {
                        items(currentScreenList) { screen ->
                            val interactionSource = remember {
                                MutableInteractionSource()
                            }
                            val pressed by interactionSource.collectIsPressedAsState()

                            val cornerSize by animateDpAsState(
                                if (pressed) 6.dp
                                else 18.dp
                            )
                            PreferenceItemOverload(
                                onClick = {
                                    onNavigateToScreenWithPopUpTo(screen)
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .widthIn(min = 1.dp)
                                    .fillMaxWidth()
                                    .animateItem(),
                                shape = RoundedCornerShape(cornerSize),
                                title = stringResource(screen.title),
                                subtitle = stringResource(screen.subtitle),
                                startIcon = {
                                    AnimatedContent(
                                        targetState = screen.icon,
                                        transitionSpec = {
                                            (slideInVertically() + fadeIn() + scaleIn())
                                                .togetherWith(slideOutVertically { it / 2 } + fadeOut() + scaleOut())
                                                .using(SizeTransform(false))
                                        }
                                    ) { icon ->
                                        icon?.let {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                interactionSource = interactionSource
                            )
                        }
                    }
                )
                val toastHostState = LocalToastHostState.current
                val clipboardManager = remember(context) {
                    context.getSystemService<ClipboardManager>()
                }
                BoxAnimatedVisibility(
                    visible = showClipButton,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .then(
                            if (showNavRail) {
                                Modifier.navigationBarsPadding()
                            } else Modifier
                        ),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    BadgedBox(
                        badge = {
                            if (clipboardData.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(clipboardData.size.toString())
                                }
                            }
                        }
                    ) {
                        EnhancedFloatingActionButton(
                            onClick = {
                                if (!allowAutoPaste) {
                                    val list = clipboardManager.clipList()
                                    if (list.isEmpty()) {
                                        scope.launch {
                                            toastHostState.showToast(
                                                message = context.getString(R.string.clipboard_paste_invalid_empty),
                                                icon = Icons.Outlined.ContentPasteOff
                                            )
                                        }
                                    } else onGetClipList(list)
                                } else onGetClipList(clipboardData)
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = stringResource(R.string.copy)
                            )
                        }
                    }
                }
                BoxAnimatedVisibility(
                    visible = showSearchButton,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .then(
                            if (showClipButton) {
                                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                            } else Modifier.padding(16.dp)
                        )
                        .then(
                            if (showNavRail) {
                                Modifier.navigationBarsPadding()
                            } else Modifier
                        )
                        .then(
                            if (showClipButton) {
                                Modifier.padding(bottom = 76.dp)
                            } else Modifier
                        )
                ) {
                    EnhancedFloatingActionButton(
                        containerColor = if (showClipButton) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else MaterialTheme.colorScheme.tertiaryContainer,
                        type = if (showClipButton) {
                            EnhancedFloatingActionButtonType.Small
                        } else EnhancedFloatingActionButtonType.Primary,
                        onClick = { onChangeShowScreenSearch(canSearchScreens) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                            contentDescription = stringResource(R.string.search_here)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.nothing_found_by_search),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                )
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(2f)
                        .sizeIn(maxHeight = 140.dp, maxWidth = 140.dp)
                        .fillMaxSize()
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}