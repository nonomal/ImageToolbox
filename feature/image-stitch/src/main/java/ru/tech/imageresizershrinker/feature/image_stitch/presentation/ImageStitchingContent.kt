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

package ru.tech.imageresizershrinker.feature.image_stitch.presentation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.dynamic.theme.LocalDynamicThemeState
import dev.olshevski.navigation.reimagined.hilt.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.utils.confetti.LocalConfettiHostState
import ru.tech.imageresizershrinker.core.ui.utils.helper.Picker
import ru.tech.imageresizershrinker.core.ui.utils.helper.asClip
import ru.tech.imageresizershrinker.core.ui.utils.helper.isPortraitOrientationAsState
import ru.tech.imageresizershrinker.core.ui.utils.helper.localImagePickerMode
import ru.tech.imageresizershrinker.core.ui.utils.helper.parseSaveResult
import ru.tech.imageresizershrinker.core.ui.utils.helper.rememberImagePicker
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.widget.AdaptiveLayoutScreen
import ru.tech.imageresizershrinker.core.ui.widget.buttons.BottomButtonsBlock
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ShareButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ZoomButton
import ru.tech.imageresizershrinker.core.ui.widget.controls.ImageReorderCarousel
import ru.tech.imageresizershrinker.core.ui.widget.controls.ScaleSmallImagesToLargeToggle
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.BackgroundColorSelector
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.ImageFormatSelector
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.QualitySelector
import ru.tech.imageresizershrinker.core.ui.widget.dialogs.ExitWithoutSavingDialog
import ru.tech.imageresizershrinker.core.ui.widget.dialogs.OneTimeSaveLocationSelectionDialog
import ru.tech.imageresizershrinker.core.ui.widget.image.AutoFilePicker
import ru.tech.imageresizershrinker.core.ui.widget.image.ImageContainer
import ru.tech.imageresizershrinker.core.ui.widget.image.ImageNotPickedWidget
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container
import ru.tech.imageresizershrinker.core.ui.widget.other.LoadingDialog
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.other.TopAppBarEmoji
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ProcessImagesPreferenceSheet
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ZoomModalSheet
import ru.tech.imageresizershrinker.core.ui.widget.text.TopAppBarTitle
import ru.tech.imageresizershrinker.feature.image_stitch.presentation.components.ImageFadingEdgesSelector
import ru.tech.imageresizershrinker.feature.image_stitch.presentation.components.ImageScaleSelector
import ru.tech.imageresizershrinker.feature.image_stitch.presentation.components.SpacingSelector
import ru.tech.imageresizershrinker.feature.image_stitch.presentation.components.StitchModeSelector
import ru.tech.imageresizershrinker.feature.image_stitch.presentation.viewModel.ImageStitchingViewModel
import kotlin.math.roundToLong

@Composable
fun ImageStitchingContent(
    uriState: List<Uri>?,
    onGoBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: ImageStitchingViewModel = hiltViewModel()
) {
    val settingsState = LocalSettingsState.current

    val context = LocalContext.current as ComponentActivity
    val toastHostState = LocalToastHostState.current
    val themeState = LocalDynamicThemeState.current
    val allowChangeColor = settingsState.allowChangeColorByImage

    val scope = rememberCoroutineScope()
    val confettiHostState = LocalConfettiHostState.current
    val showConfetti: () -> Unit = {
        scope.launch {
            confettiHostState.showConfetti()
        }
    }

    LaunchedEffect(uriState) {
        uriState?.takeIf { it.isNotEmpty() }?.let { uris ->
            viewModel.updateUris(uris)
        }
    }

    LaunchedEffect(viewModel.previewBitmap) {
        viewModel.previewBitmap?.let {
            if (allowChangeColor) {
                themeState.updateColorByImage(it)
            }
        }
    }

    val pickImageLauncher =
        rememberImagePicker(
            mode = localImagePickerMode(Picker.Multiple)
        ) { list ->
            list.takeIf { it.isNotEmpty() }?.let { uris ->
                viewModel.updateUris(uris)
            }
        }

    val addImagesLauncher =
        rememberImagePicker(
            mode = localImagePickerMode(Picker.Multiple)
        ) { list ->
            list.takeIf { it.isNotEmpty() }?.let { uris ->
                viewModel.addUrisToEnd(uris)
            }
        }

    val addImages = {
        addImagesLauncher.pickImage()
    }

    val pickImage = pickImageLauncher::pickImage

    AutoFilePicker(
        onAutoPick = pickImage,
        isPickedAlready = !uriState.isNullOrEmpty()
    )

    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val onBack = {
        if (viewModel.haveChanges) showExitDialog = true
        else onGoBack()
    }

    val saveBitmaps: (oneTimeSaveLocationUri: String?) -> Unit = {
        viewModel.saveBitmaps(it) { saveResult ->
            context.parseSaveResult(
                saveResult = saveResult,
                onSuccess = showConfetti,
                toastHostState = toastHostState,
                scope = scope
            )
        }
    }

    val isPortrait by isPortraitOrientationAsState()

    var showZoomSheet by rememberSaveable { mutableStateOf(false) }

    ZoomModalSheet(
        data = viewModel.previewBitmap,
        visible = showZoomSheet,
        onDismiss = {
            showZoomSheet = false
        }
    )

    AdaptiveLayoutScreen(
        title = {
            TopAppBarTitle(
                title = stringResource(R.string.image_stitching),
                input = viewModel.uris,
                isLoading = viewModel.isImageLoading,
                size = viewModel
                    .imageByteSize?.times(viewModel.imageScale)?.roundToLong(),
                updateOnSizeChange = false
            )
        },
        onGoBack = onBack,
        actions = {
            var editSheetData by remember {
                mutableStateOf(listOf<Uri>())
            }
            ShareButton(
                enabled = viewModel.previewBitmap != null,
                onShare = {
                    viewModel.shareBitmap(showConfetti)
                },
                onCopy = { manager ->
                    viewModel.cacheCurrentImage { uri ->
                        manager.setClip(uri.asClip(context))
                        showConfetti()
                    }
                },
                onEdit = {
                    viewModel.cacheCurrentImage {
                        editSheetData = listOf(it)
                    }
                }
            )
            ProcessImagesPreferenceSheet(
                uris = editSheetData,
                visible = editSheetData.isNotEmpty(),
                onDismiss = {
                    if (!it) {
                        editSheetData = emptyList()
                    }
                },
                onNavigate = { screen ->
                    scope.launch {
                        editSheetData = emptyList()
                        delay(200)
                        onNavigate(screen)
                    }
                }
            )
            ZoomButton(
                onClick = { showZoomSheet = true },
                visible = viewModel.previewBitmap != null,
            )
        },
        imagePreview = {
            ImageContainer(
                imageInside = isPortrait,
                showOriginal = false,
                previewBitmap = viewModel.previewBitmap,
                originalBitmap = null,
                isLoading = viewModel.isImageLoading,
                shouldShowPreview = true
            )
        },
        topAppBarPersistentActions = {
            if (viewModel.uris.isNullOrEmpty()) {
                TopAppBarEmoji()
            }
        },
        controls = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImageReorderCarousel(
                    images = viewModel.uris,
                    onReorder = viewModel::updateUris,
                    onNeedToAddImage = addImages,
                    onNeedToRemoveImageAt = viewModel::removeImageAt
                )
                ImageScaleSelector(
                    modifier = Modifier.padding(top = 8.dp),
                    value = viewModel.imageScale,
                    onValueChange = viewModel::updateImageScale,
                    approximateImageSize = viewModel.imageSize
                )
                StitchModeSelector(
                    value = viewModel.combiningParams.stitchMode,
                    onValueChange = viewModel::setStitchMode
                )
                SpacingSelector(
                    value = viewModel.combiningParams.spacing,
                    onValueChange = viewModel::updateImageSpacing
                )
                AnimatedVisibility(
                    visible = viewModel.combiningParams.spacing < 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ImageFadingEdgesSelector(
                        value = viewModel.combiningParams.fadingEdgesMode,
                        onValueChange = viewModel::setFadingEdgesMode
                    )
                }
                ScaleSmallImagesToLargeToggle(
                    checked = viewModel.combiningParams.scaleSmallImagesToLarge,
                    onCheckedChange = viewModel::toggleScaleSmallImagesToLarge
                )
                BackgroundColorSelector(
                    value = Color(viewModel.combiningParams.backgroundColor),
                    onColorChange = {
                        viewModel.updateBackgroundSelector(it.toArgb())
                    },
                    modifier = Modifier.container(
                        shape = RoundedCornerShape(
                            24.dp
                        )
                    )
                )
                QualitySelector(
                    imageFormat = viewModel.imageInfo.imageFormat,
                    enabled = !viewModel.uris.isNullOrEmpty(),
                    quality = viewModel.imageInfo.quality,
                    onQualityChange = viewModel::setQuality
                )
                ImageFormatSelector(
                    value = viewModel.imageInfo.imageFormat,
                    onValueChange = viewModel::setImageFormat
                )
            }
        },
        buttons = { actions ->
            var showFolderSelectionDialog by rememberSaveable {
                mutableStateOf(false)
            }
            BottomButtonsBlock(
                isPrimaryButtonVisible = viewModel.previewBitmap != null,
                targetState = (viewModel.uris.isNullOrEmpty()) to isPortrait,
                onSecondaryButtonClick = pickImage,
                onPrimaryButtonClick = {
                    saveBitmaps(null)
                },
                onPrimaryButtonLongClick = {
                    showFolderSelectionDialog = true
                },
                actions = {
                    if (isPortrait) actions()
                }
            )
            if (showFolderSelectionDialog) {
                OneTimeSaveLocationSelectionDialog(
                    onDismiss = { showFolderSelectionDialog = false },
                    onSaveRequest = saveBitmaps
                )
            }
        },
        noDataControls = {
            if (!viewModel.isImageLoading) {
                ImageNotPickedWidget(onPickImage = pickImage)
            }
        },
        canShowScreenData = !viewModel.uris.isNullOrEmpty(),
        isPortrait = isPortrait
    )

    if (viewModel.isSaving) {
        LoadingDialog(
            done = viewModel.done,
            left = viewModel.uris?.size ?: 1,
            onCancelLoading = viewModel::cancelSaving
        )
    }

    ExitWithoutSavingDialog(
        onExit = onGoBack,
        onDismiss = { showExitDialog = false },
        visible = showExitDialog
    )
}