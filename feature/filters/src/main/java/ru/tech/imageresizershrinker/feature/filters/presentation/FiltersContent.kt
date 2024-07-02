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

package ru.tech.imageresizershrinker.feature.filters.presentation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Texture
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.dynamic.theme.LocalDynamicThemeState
import com.t8rin.dynamic.theme.rememberAppColorTuple
import dev.olshevski.navigation.reimagined.hilt.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.filters.presentation.model.UiFilter
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.theme.mixedContainer
import ru.tech.imageresizershrinker.core.ui.utils.animation.fancySlideTransition
import ru.tech.imageresizershrinker.core.ui.utils.confetti.LocalConfettiHostState
import ru.tech.imageresizershrinker.core.ui.utils.helper.Picker
import ru.tech.imageresizershrinker.core.ui.utils.helper.asClip
import ru.tech.imageresizershrinker.core.ui.utils.helper.isPortraitOrientationAsState
import ru.tech.imageresizershrinker.core.ui.utils.helper.localImagePickerMode
import ru.tech.imageresizershrinker.core.ui.utils.helper.parseSaveResult
import ru.tech.imageresizershrinker.core.ui.utils.helper.parseSaveResults
import ru.tech.imageresizershrinker.core.ui.utils.helper.rememberImagePicker
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.widget.buttons.BottomButtonsBlock
import ru.tech.imageresizershrinker.core.ui.widget.buttons.CompareButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedFloatingActionButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedIconButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ShareButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ShowOriginalButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ZoomButton
import ru.tech.imageresizershrinker.core.ui.widget.controls.SaveExifWidget
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.ImageFormatSelector
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.QualitySelector
import ru.tech.imageresizershrinker.core.ui.widget.dialogs.ExitWithoutSavingDialog
import ru.tech.imageresizershrinker.core.ui.widget.dialogs.OneTimeSaveLocationSelectionDialog
import ru.tech.imageresizershrinker.core.ui.widget.image.AutoFilePicker
import ru.tech.imageresizershrinker.core.ui.widget.image.ImageContainer
import ru.tech.imageresizershrinker.core.ui.widget.image.ImageCounter
import ru.tech.imageresizershrinker.core.ui.widget.image.imageStickyHeader
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container
import ru.tech.imageresizershrinker.core.ui.widget.modifier.drawHorizontalStroke
import ru.tech.imageresizershrinker.core.ui.widget.modifier.navBarsLandscapePadding
import ru.tech.imageresizershrinker.core.ui.widget.modifier.scaleOnTap
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBar
import ru.tech.imageresizershrinker.core.ui.widget.other.EnhancedTopAppBarType
import ru.tech.imageresizershrinker.core.ui.widget.other.LoadingDialog
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.other.TopAppBarEmoji
import ru.tech.imageresizershrinker.core.ui.widget.other.showError
import ru.tech.imageresizershrinker.core.ui.widget.preferences.PreferenceItem
import ru.tech.imageresizershrinker.core.ui.widget.sheets.PickImageFromUrisSheet
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ProcessImagesPreferenceSheet
import ru.tech.imageresizershrinker.core.ui.widget.sheets.SimpleSheet
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ZoomModalSheet
import ru.tech.imageresizershrinker.core.ui.widget.text.TitleItem
import ru.tech.imageresizershrinker.core.ui.widget.text.TopAppBarTitle
import ru.tech.imageresizershrinker.core.ui.widget.text.marquee
import ru.tech.imageresizershrinker.core.ui.widget.utils.isExpanded
import ru.tech.imageresizershrinker.core.ui.widget.utils.rememberAvailableHeight
import ru.tech.imageresizershrinker.core.ui.widget.utils.rememberImageState
import ru.tech.imageresizershrinker.feature.compare.presentation.components.CompareSheet
import ru.tech.imageresizershrinker.feature.filters.presentation.components.AddEditMaskSheet
import ru.tech.imageresizershrinker.feature.filters.presentation.components.AddFilterButton
import ru.tech.imageresizershrinker.feature.filters.presentation.components.AddFiltersSheet
import ru.tech.imageresizershrinker.feature.filters.presentation.components.BasicFilterPreference
import ru.tech.imageresizershrinker.feature.filters.presentation.components.FilterItem
import ru.tech.imageresizershrinker.feature.filters.presentation.components.FilterReorderSheet
import ru.tech.imageresizershrinker.feature.filters.presentation.components.MaskFilterPreference
import ru.tech.imageresizershrinker.feature.filters.presentation.components.MaskItem
import ru.tech.imageresizershrinker.feature.filters.presentation.components.MaskReorderSheet
import ru.tech.imageresizershrinker.feature.filters.presentation.viewModel.FilterViewModel
import ru.tech.imageresizershrinker.feature.pick_color.presentation.components.PickColorFromImageSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersContent(
    type: Screen.Filter.Type?,
    onGoBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val settingsState = LocalSettingsState.current

    val context = LocalContext.current as ComponentActivity
    val toastHostState = LocalToastHostState.current
    val themeState = LocalDynamicThemeState.current
    val allowChangeColor = settingsState.allowChangeColorByImage

    val appColorTuple = rememberAppColorTuple(
        defaultColorTuple = settingsState.appColorTuple,
        dynamicColor = settingsState.isDynamicColors,
        darkTheme = settingsState.isNightMode
    )

    val scope = rememberCoroutineScope()
    val confettiHostState = LocalConfettiHostState.current
    val showConfetti: () -> Unit = {
        scope.launch {
            confettiHostState.showConfetti()
        }
    }

    LaunchedEffect(type) {
        type?.let { viewModel.setType(it) }
    }

    LaunchedEffect(viewModel.previewBitmap) {
        viewModel.previewBitmap?.let {
            if (allowChangeColor) {
                themeState.updateColorByImage(it)
            }
        }
    }

    val pickImagesLauncher =
        rememberImagePicker(
            mode = localImagePickerMode(Picker.Multiple)
        ) { list ->
            list.takeIf { it.isNotEmpty() }?.let(viewModel::setBasicFilter)
        }

    val pickSingleImageLauncher = rememberImagePicker(
        mode = localImagePickerMode(Picker.Single)
    ) { list ->
        list.takeIf { it.isNotEmpty() }?.firstOrNull()?.let(viewModel::setMaskFilter)
    }

    var showAddMaskSheet by rememberSaveable { mutableStateOf(false) }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showAddFilterSheet by rememberSaveable { mutableStateOf(false) }

    val onBack = {
        if (viewModel.haveChanges) showExitDialog = true
        else if (viewModel.filterType != null) {
            viewModel.clearType()
            themeState.updateColorTuple(appColorTuple)
        } else onGoBack()
    }

    var showZoomSheet by rememberSaveable { mutableStateOf(false) }
    var showCompareSheet by rememberSaveable { mutableStateOf(false) }

    val isPortrait by isPortraitOrientationAsState()

    val focus = LocalFocusManager.current
    var showPickImageFromUrisSheet by rememberSaveable { mutableStateOf(false) }

    var showOriginal by remember { mutableStateOf(false) }
    var imageState by rememberImageState()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState, canScroll = { !imageState.isExpanded() && !showOriginal }
    )

    LaunchedEffect(imageState, showOriginal) {
        if (imageState.isExpanded() || showOriginal) {
            while (topAppBarState.heightOffset > topAppBarState.heightOffsetLimit) {
                topAppBarState.heightOffset -= 5f
                delay(1)
            }
        }
    }

    var showReorderSheet by rememberSaveable { mutableStateOf(false) }
    val actions: @Composable RowScope.() -> Unit = {
        Spacer(modifier = Modifier.width(8.dp))
        if (viewModel.bitmap != null) {
            var editSheetData by remember {
                mutableStateOf(listOf<Uri>())
            }
            ShareButton(
                enabled = viewModel.canSave,
                onShare = {
                    viewModel.performSharing(showConfetti)
                },
                onCopy = { manager ->
                    viewModel.cacheCurrentImage { uri ->
                        manager.setClip(uri.asClip(context))
                        showConfetti()
                    }
                },
                onEdit = {
                    viewModel.cacheImages {
                        editSheetData = it
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
            ShowOriginalButton(
                canShow = viewModel.canShow(),
                onStateChange = {
                    showOriginal = it
                }
            )
        }
        CompareButton(
            onClick = { showCompareSheet = true },
            visible = viewModel.previewBitmap != null
        )
        if (viewModel.bitmap != null && (viewModel.basicFilterState.filters.size >= 2 || viewModel.maskingFilterState.masks.size >= 2)) {
            EnhancedIconButton(
                containerColor = Color.Transparent,
                contentColor = LocalContentColor.current,
                enableAutoShadowAndBorder = false,
                onClick = { showReorderSheet = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.properties)
                )
            }
        }
    }

    val imageBlock = @Composable {
        ImageContainer(
            imageInside = isPortrait,
            showOriginal = showOriginal,
            previewBitmap = viewModel.previewBitmap,
            originalBitmap = viewModel.bitmap,
            isLoading = viewModel.isImageLoading,
            shouldShowPreview = true,
            animatePreviewChange = false
        )
    }

    val buttons: @Composable (filterType: Screen.Filter.Type) -> Unit = { filterType ->

        val saveBitmaps: (oneTimeSaveLocationUri: String?) -> Unit = {
            when (filterType) {
                is Screen.Filter.Type.Basic -> {
                    viewModel.saveBitmaps(it) { results ->
                        context.parseSaveResults(
                            scope = scope,
                            results = results,
                            toastHostState = toastHostState,
                            isOverwritten = settingsState.overwriteFiles,
                            showConfetti = showConfetti
                        )
                    }
                }

                is Screen.Filter.Type.Masking -> {
                    viewModel.saveMaskedBitmap(it) { saveResult ->
                        context.parseSaveResult(
                            saveResult = saveResult,
                            onSuccess = showConfetti,
                            toastHostState = toastHostState,
                            scope = scope
                        )
                    }
                }
            }
        }
        var showFolderSelectionDialog by rememberSaveable {
            mutableStateOf(false)
        }
        BottomButtonsBlock(
            targetState = (viewModel.basicFilterState.uris.isNullOrEmpty() && viewModel.maskingFilterState.uri == null) to isPortrait,
            onSecondaryButtonClick = {
                when (filterType) {
                    is Screen.Filter.Type.Basic -> pickImagesLauncher.pickImage()
                    is Screen.Filter.Type.Masking -> pickSingleImageLauncher.pickImage()
                }
            },
            onPrimaryButtonClick = {
                saveBitmaps(null)
            },
            onPrimaryButtonLongClick = {
                showFolderSelectionDialog = true
            },
            isPrimaryButtonVisible = viewModel.canSave,
            columnarFab = {
                EnhancedFloatingActionButton(
                    onClick = {
                        when (filterType) {
                            is Screen.Filter.Type.Basic -> showAddFilterSheet = true
                            is Screen.Filter.Type.Masking -> showAddMaskSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.mixedContainer
                ) {
                    when (filterType) {
                        is Screen.Filter.Type.Basic -> {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = null
                            )
                        }

                        is Screen.Filter.Type.Masking -> {
                            Icon(
                                imageVector = Icons.Rounded.Texture,
                                contentDescription = null
                            )
                        }
                    }
                }

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
    }

    val controls: @Composable (filterType: Screen.Filter.Type) -> Unit = { filterType ->
        val baseControls: @Composable (wrapped: @Composable () -> Unit) -> Unit = { wrapped ->
            val internalHeight = rememberAvailableHeight(imageState, showOriginal)
            LazyColumn(
                contentPadding = PaddingValues(
                    bottom = WindowInsets
                        .navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + WindowInsets.ime
                        .asPaddingValues()
                        .calculateBottomPadding() + (if (!isPortrait && viewModel.bitmap != null) 20.dp else 100.dp),
                    top = if (viewModel.bitmap == null || !isPortrait) 20.dp else 0.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .clipToBounds()
            ) {
                imageStickyHeader(
                    visible = isPortrait && viewModel.bitmap != null,
                    internalHeight = internalHeight,
                    imageState = imageState,
                    onStateChange = { imageState = it },
                    imageBlock = imageBlock
                )
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navBarsLandscapePadding(viewModel.bitmap == null),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        wrapped()
                    }
                }
            }
        }

        when (filterType) {
            is Screen.Filter.Type.Basic -> {
                baseControls {
                    val filterList = viewModel.basicFilterState.filters
                    if (isPortrait && viewModel.bitmap == null) imageBlock()
                    if (viewModel.bitmap != null) {
                        ImageCounter(
                            imageCount = viewModel.basicFilterState.uris?.size?.takeIf { it > 1 },
                            onRepick = {
                                showPickImageFromUrisSheet = true
                            }
                        )
                        AnimatedContent(
                            targetState = filterList.isNotEmpty(),
                            transitionSpec = {
                                fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                            }
                        ) { notEmpty ->
                            if (notEmpty) {
                                Column(Modifier.container(MaterialTheme.shapes.extraLarge)) {
                                    TitleItem(text = stringResource(R.string.filters))
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(
                                            8.dp
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        filterList.forEachIndexed { index, filter ->
                                            FilterItem(
                                                backgroundColor = MaterialTheme.colorScheme.surface,
                                                filter = filter,
                                                onFilterChange = {
                                                    viewModel.updateFilter(
                                                        value = it,
                                                        index = index,
                                                        showError = {
                                                            scope.launch {
                                                                toastHostState.showError(
                                                                    context,
                                                                    it
                                                                )
                                                            }
                                                        }
                                                    )
                                                },
                                                onLongPress = {
                                                    showReorderSheet = true
                                                },
                                                showDragHandle = false,
                                                onRemove = {
                                                    viewModel.removeFilterAtIndex(index)
                                                }
                                            )
                                        }
                                        AddFilterButton(
                                            onClick = {
                                                showAddFilterSheet = true
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp
                                            )
                                        )
                                    }
                                }
                            } else {
                                AddFilterButton(
                                    onClick = {
                                        showAddFilterSheet = true
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        SaveExifWidget(
                            imageFormat = viewModel.imageInfo.imageFormat,
                            checked = viewModel.keepExif,
                            onCheckedChange = viewModel::setKeepExif
                        )
                        if (viewModel.imageInfo.imageFormat.canChangeCompressionValue) Spacer(
                            Modifier.size(8.dp)
                        )
                        QualitySelector(
                            imageFormat = viewModel.imageInfo.imageFormat,
                            enabled = viewModel.bitmap != null,
                            quality = viewModel.imageInfo.quality,
                            onQualityChange = viewModel::setQuality
                        )
                        Spacer(Modifier.size(8.dp))
                        ImageFormatSelector(
                            value = viewModel.imageInfo.imageFormat,
                            onValueChange = {
                                viewModel.setImageFormat(it)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            is Screen.Filter.Type.Masking -> {
                baseControls {
                    val maskList = viewModel.maskingFilterState.masks
                    if (isPortrait && viewModel.bitmap == null) imageBlock()
                    if (viewModel.bitmap != null) {
                        AnimatedContent(
                            targetState = maskList.isNotEmpty(),
                            transitionSpec = {
                                fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                            }
                        ) { notEmpty ->
                            if (notEmpty) {
                                Column(Modifier.container(MaterialTheme.shapes.extraLarge)) {
                                    TitleItem(text = stringResource(R.string.masks))
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(
                                            4.dp
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        maskList.forEachIndexed { index, mask ->
                                            MaskItem(
                                                backgroundColor = MaterialTheme.colorScheme.surface,
                                                imageUri = viewModel.maskingFilterState.uri,
                                                previousMasks = maskList.take(index),
                                                mask = mask,
                                                titleText = stringResource(
                                                    R.string.mask_indexed,
                                                    index + 1
                                                ),
                                                onMaskChange = {
                                                    viewModel.updateMask(
                                                        value = it,
                                                        index = index,
                                                        showError = {
                                                            scope.launch {
                                                                toastHostState.showError(
                                                                    context = context,
                                                                    error = it
                                                                )
                                                            }
                                                        }
                                                    )
                                                },
                                                onLongPress = {
                                                    showReorderSheet = true
                                                },
                                                showDragHandle = false,
                                                onRemove = {
                                                    viewModel.removeMaskAtIndex(index)
                                                }
                                            )
                                        }
                                        EnhancedButton(
                                            containerColor = MaterialTheme.colorScheme.mixedContainer,
                                            onClick = {
                                                showAddMaskSheet = true
                                            },
                                            modifier = Modifier.padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 4.dp
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Texture,
                                                contentDescription = stringResource(R.string.add_mask)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(id = R.string.add_mask))
                                        }
                                    }
                                }
                            } else {
                                EnhancedButton(
                                    containerColor = MaterialTheme.colorScheme.mixedContainer,
                                    onClick = {
                                        showAddMaskSheet = true
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Texture,
                                        contentDescription = stringResource(R.string.add_mask)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.add_mask))
                                }
                            }
                        }

                        Spacer(Modifier.size(8.dp))
                        SaveExifWidget(
                            imageFormat = viewModel.imageInfo.imageFormat,
                            checked = viewModel.keepExif,
                            onCheckedChange = viewModel::setKeepExif
                        )
                        if (viewModel.imageInfo.imageFormat.canChangeCompressionValue) Spacer(
                            Modifier.size(8.dp)
                        )
                        QualitySelector(
                            imageFormat = viewModel.imageInfo.imageFormat,
                            enabled = viewModel.bitmap != null,
                            quality = viewModel.imageInfo.quality,
                            onQualityChange = viewModel::setQuality
                        )
                        Spacer(Modifier.size(8.dp))
                        ImageFormatSelector(
                            value = viewModel.imageInfo.imageFormat,
                            onValueChange = {
                                viewModel.setImageFormat(it)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    val content: @Composable BoxScope.(filterType: Screen.Filter.Type) -> Unit = { filterType ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!isPortrait) {
                val direction = LocalLayoutDirection.current
                Box(
                    Modifier
                        .container(RectangleShape)
                        .fillMaxHeight()
                        .padding(
                            start = WindowInsets
                                .displayCutout
                                .asPaddingValues()
                                .calculateStartPadding(direction)
                        )
                        .weight(1.2f)
                        .padding(20.dp)
                ) {
                    Box(Modifier.align(Alignment.Center)) {
                        imageBlock()
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                controls(filterType)
            }

            if (!isPortrait && viewModel.bitmap != null) {
                buttons(filterType)
            }
        }

        if (isPortrait || viewModel.bitmap == null) {
            Box(
                modifier = Modifier.align(settingsState.fabAlignment)
            ) {
                buttons(filterType)
            }
        }
    }

    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var tempColor by rememberSaveable(showColorPicker) { mutableStateOf(Color.Black) }

    PickColorFromImageSheet(
        visible = showColorPicker,
        onDismiss = {
            showColorPicker = false
        },
        bitmap = viewModel.previewBitmap,
        onColorChange = { tempColor = it },
        color = tempColor
    )

    ZoomModalSheet(
        data = viewModel.previewBitmap,
        visible = showZoomSheet,
        onDismiss = {
            showZoomSheet = false
        }
    )

    CompareSheet(
        data = viewModel.bitmap to viewModel.previewBitmap,
        visible = showCompareSheet,
        onDismiss = {
            showCompareSheet = false
        }
    )

    var tempSelectionUris by rememberSaveable {
        mutableStateOf<List<Uri>?>(
            null
        )
    }
    var showSelectionFilterPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSelectionFilterPicker) {
        if (!showSelectionFilterPicker) tempSelectionUris = null
    }
    val selectionFilterPicker = rememberImagePicker(
        mode = localImagePickerMode(Picker.Multiple)
    ) { uris ->
        uris.takeIf { it.isNotEmpty() }?.let {
            tempSelectionUris = it
            showSelectionFilterPicker = true
        }
    }

    AutoFilePicker(
        onAutoPick = selectionFilterPicker::pickImage,
        isPickedAlready = type != null
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focus.clearFocus()
                    }
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            Column(Modifier.fillMaxSize()) {
                EnhancedTopAppBar(
                    type = EnhancedTopAppBarType.Large,
                    scrollBehavior = scrollBehavior,
                    title = {
                        AnimatedContent(
                            targetState = viewModel.filterType?.let {
                                stringResource(it.title)
                            }
                        ) { title ->
                            if (title == null) {
                                val text by remember {
                                    derivedStateOf {
                                        UiFilter.groupedEntries.flatten().size.toString()
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.marquee()
                                ) {
                                    Text(
                                        text = stringResource(R.string.filter)
                                    )
                                    Badge(
                                        content = {
                                            Text(
                                                text = text
                                            )
                                        },
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .padding(bottom = 12.dp)
                                            .scaleOnTap {
                                                showConfetti()
                                            }
                                    )
                                }
                            } else {
                                TopAppBarTitle(
                                    title = title,
                                    input = viewModel.bitmap,
                                    isLoading = viewModel.isImageLoading,
                                    size = viewModel.imageInfo.sizeInBytes.toLong()
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        EnhancedIconButton(
                            containerColor = Color.Transparent,
                            contentColor = LocalContentColor.current,
                            enableAutoShadowAndBorder = false,
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.exit)
                            )
                        }
                    },
                    actions = {
                        if (viewModel.previewBitmap != null) {
                            EnhancedIconButton(
                                containerColor = Color.Transparent,
                                contentColor = LocalContentColor.current,
                                enableAutoShadowAndBorder = false,
                                onClick = {
                                    showColorPicker = true
                                },
                                enabled = viewModel.previewBitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Colorize,
                                    contentDescription = stringResource(R.string.pipette)
                                )
                            }
                            ZoomButton(
                                onClick = { showZoomSheet = true },
                                visible = viewModel.bitmap != null,
                            )
                        }
                        if (viewModel.bitmap == null) {
                            TopAppBarEmoji()
                        }
                        if (viewModel.bitmap != null && !isPortrait) actions()
                        if (viewModel.bitmap != null && isPortrait) {
                            when (viewModel.filterType) {
                                is Screen.Filter.Type.Basic -> {
                                    EnhancedIconButton(
                                        containerColor = MaterialTheme.colorScheme.mixedContainer,
                                        onClick = { showAddFilterSheet = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoFixHigh,
                                            contentDescription = stringResource(R.string.add_filter)
                                        )
                                    }
                                }

                                is Screen.Filter.Type.Masking -> {
                                    EnhancedIconButton(
                                        containerColor = MaterialTheme.colorScheme.mixedContainer,
                                        onClick = { showAddMaskSheet = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Texture,
                                            contentDescription = stringResource(R.string.add_mask)
                                        )
                                    }
                                }

                                null -> Unit
                            }
                        }
                    }
                )

                val screenWidth = LocalConfiguration.current.screenWidthDp

                AnimatedContent(
                    transitionSpec = {
                        fancySlideTransition(
                            isForward = targetState != null,
                            screenWidthDp = screenWidth
                        )
                    },
                    targetState = viewModel.filterType
                ) { filterType ->
                    when (filterType) {
                        null -> {
                            Column {
                                val cutout = WindowInsets.displayCutout.asPaddingValues()
                                LazyVerticalStaggeredGrid(
                                    modifier = Modifier.weight(1f),
                                    columns = StaggeredGridCells.Adaptive(300.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        space = 12.dp,
                                        alignment = Alignment.CenterHorizontally
                                    ),
                                    verticalItemSpacing = 12.dp,
                                    contentPadding = PaddingValues(
                                        bottom = 12.dp + WindowInsets
                                            .navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding(),
                                        top = 12.dp,
                                        end = 12.dp + cutout.calculateEndPadding(
                                            LocalLayoutDirection.current
                                        ),
                                        start = 12.dp + cutout.calculateStartPadding(
                                            LocalLayoutDirection.current
                                        )
                                    ),
                                ) {
                                    Screen.Filter.Type.entries.forEach {
                                        item {
                                            PreferenceItem(
                                                onClick = {
                                                    when (it) {
                                                        is Screen.Filter.Type.Basic -> pickImagesLauncher.pickImage()
                                                        is Screen.Filter.Type.Masking -> pickSingleImageLauncher.pickImage()
                                                    }
                                                },
                                                startIcon = it.icon,
                                                title = stringResource(it.title),
                                                subtitle = stringResource(it.subtitle),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawHorizontalStroke(true)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    EnhancedFloatingActionButton(
                                        onClick = {
                                            selectionFilterPicker.pickImage()
                                        },
                                        modifier = Modifier
                                            .navigationBarsPadding()
                                            .padding(16.dp),
                                        content = {
                                            Spacer(Modifier.width(16.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                                contentDescription = stringResource(R.string.pick_image_alt)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(stringResource(R.string.pick_image_alt))
                                            Spacer(Modifier.width(16.dp))
                                        }
                                    )
                                }
                            }

                            SimpleSheet(
                                visible = showSelectionFilterPicker,
                                onDismiss = {
                                    showSelectionFilterPicker = it
                                },
                                confirmButton = {
                                    EnhancedButton(
                                        onClick = {
                                            showSelectionFilterPicker = false
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(stringResource(id = R.string.close))
                                    }
                                },
                                sheetContent = {
                                    if (tempSelectionUris == null) {
                                        showSelectionFilterPicker = false
                                    }

                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Adaptive(250.dp),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            space = 12.dp,
                                            alignment = Alignment.CenterHorizontally
                                        ),
                                        verticalItemSpacing = 12.dp,
                                        contentPadding = PaddingValues(12.dp),
                                    ) {
                                        item {
                                            BasicFilterPreference(
                                                onClick = {
                                                    viewModel.setBasicFilter(tempSelectionUris)
                                                    showSelectionFilterPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        item {
                                            MaskFilterPreference(
                                                onClick = {
                                                    viewModel.setMaskFilter(tempSelectionUris?.firstOrNull())
                                                    showSelectionFilterPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                },
                                title = {
                                    TitleItem(
                                        text = stringResource(id = R.string.pick_file),
                                        icon = Icons.Rounded.FileOpen
                                    )
                                }
                            )
                        }

                        else -> {
                            Box(Modifier.fillMaxSize()) {
                                if (filterType is Screen.Filter.Type.Basic) {
                                    val filterList = viewModel.basicFilterState.filters

                                    content(filterType)

                                    PickImageFromUrisSheet(
                                        transformations = listOf(
                                            viewModel.imageInfoTransformationFactory(
                                                imageInfo = viewModel.imageInfo,
                                                transformations = filterList.map {
                                                    viewModel.filterProvider.filterToTransformation(
                                                        it
                                                    )
                                                }
                                            )
                                        ),
                                        visible = showPickImageFromUrisSheet,
                                        onDismiss = {
                                            showPickImageFromUrisSheet = false
                                        },
                                        uris = viewModel.basicFilterState.uris,
                                        selectedUri = viewModel.basicFilterState.selectedUri,
                                        onUriPicked = { uri ->
                                            try {
                                                viewModel.setBitmap(uri = uri)
                                            } catch (e: Exception) {
                                                scope.launch {
                                                    toastHostState.showError(context, e)
                                                }
                                            }
                                        },
                                        onUriRemoved = { uri ->
                                            viewModel.updateUrisSilently(removedUri = uri)
                                        },
                                        columns = if (isPortrait) 2 else 4,
                                    )
                                } else if (filterType is Screen.Filter.Type.Masking) {

                                    content(filterType)

                                    if (isPortrait || viewModel.bitmap == null) {
                                        Box(
                                            modifier = Modifier.align(settingsState.fabAlignment)
                                        ) {
                                            buttons(filterType)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.filterType is Screen.Filter.Type.Basic) {
                AddFiltersSheet(
                    visible = showAddFilterSheet,
                    onVisibleChange = { showAddFilterSheet = it },
                    previewBitmap = viewModel.previewBitmap,
                    onFilterPicked = { viewModel.addFilter(it.newInstance()) },
                    onFilterPickedWithParams = { viewModel.addFilter(it) }
                )

                FilterReorderSheet(
                    filterList = viewModel.basicFilterState.filters,
                    visible = showReorderSheet,
                    onDismiss = {
                        showReorderSheet = false
                    },
                    updateOrder = viewModel::updateFiltersOrder
                )
            } else if (viewModel.filterType is Screen.Filter.Type.Masking) {
                AddEditMaskSheet(
                    visible = showAddMaskSheet,
                    targetBitmapUri = viewModel.maskingFilterState.uri,
                    onMaskPicked = viewModel::addMask,
                    onDismiss = {
                        showAddMaskSheet = false
                    },
                    masks = viewModel.maskingFilterState.masks
                )

                MaskReorderSheet(
                    maskList = viewModel.maskingFilterState.masks,
                    visible = showReorderSheet,
                    onDismiss = {
                        showReorderSheet = false
                    },
                    updateOrder = viewModel::updateMasksOrder
                )
            }

            if (viewModel.isSaving) {
                LoadingDialog(
                    done = viewModel.done,
                    left = viewModel.left,
                    onCancelLoading = viewModel::cancelSaving
                )
            }

            ExitWithoutSavingDialog(
                onExit = {
                    if (viewModel.filterType != null) {
                        viewModel.clearType()
                    } else onGoBack()
                },
                onDismiss = { showExitDialog = false },
                visible = showExitDialog
            )

            BackHandler(onBack = onBack)
        }
    }
}