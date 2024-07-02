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

package ru.tech.imageresizershrinker.feature.scan_qr_code.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.olshevski.navigation.reimagined.hilt.hiltViewModel
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.filters.presentation.utils.LocalFavoriteFiltersInteractor
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.model.UiFontFamily
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.theme.Typography
import ru.tech.imageresizershrinker.core.ui.theme.takeColorFromScheme
import ru.tech.imageresizershrinker.core.ui.utils.confetti.LocalConfettiHostState
import ru.tech.imageresizershrinker.core.ui.utils.helper.asClip
import ru.tech.imageresizershrinker.core.ui.utils.helper.isLandscapeOrientationAsState
import ru.tech.imageresizershrinker.core.ui.utils.helper.parseSaveResult
import ru.tech.imageresizershrinker.core.ui.utils.helper.rememberQrCodeScanner
import ru.tech.imageresizershrinker.core.ui.widget.AdaptiveLayoutScreen
import ru.tech.imageresizershrinker.core.ui.widget.buttons.BottomButtonsBlock
import ru.tech.imageresizershrinker.core.ui.widget.buttons.EnhancedIconButton
import ru.tech.imageresizershrinker.core.ui.widget.buttons.ShareButton
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.FontSelector
import ru.tech.imageresizershrinker.core.ui.widget.controls.selection.ImageSelector
import ru.tech.imageresizershrinker.core.ui.widget.dialogs.OneTimeSaveLocationSelectionDialog
import ru.tech.imageresizershrinker.core.ui.widget.image.Picture
import ru.tech.imageresizershrinker.core.ui.widget.modifier.ContainerShapeDefaults
import ru.tech.imageresizershrinker.core.ui.widget.modifier.animateShape
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container
import ru.tech.imageresizershrinker.core.ui.widget.other.BoxAnimatedVisibility
import ru.tech.imageresizershrinker.core.ui.widget.other.LoadingDialog
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.other.QrCode
import ru.tech.imageresizershrinker.core.ui.widget.other.TopAppBarEmoji
import ru.tech.imageresizershrinker.core.ui.widget.text.RoundedTextField
import ru.tech.imageresizershrinker.core.ui.widget.text.marquee
import ru.tech.imageresizershrinker.feature.scan_qr_code.presentation.viewModel.ScanQrCodeViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeApi::class)
@Composable
fun ScanQrCodeContent(
    qrCodeContent: String?,
    onGoBack: () -> Unit,
    viewModel: ScanQrCodeViewModel = hiltViewModel()
) {
    val context = LocalContext.current as ComponentActivity
    val toastHostState = LocalToastHostState.current

    val confettiHostState = LocalConfettiHostState.current

    val scope = rememberCoroutineScope()

    var qrContent by rememberSaveable(qrCodeContent) { mutableStateOf(qrCodeContent ?: "") }

    val scanner = rememberQrCodeScanner {
        qrContent = it
    }

    val interactor = LocalFavoriteFiltersInteractor.current

    LaunchedEffect(qrContent, interactor) {
        if (interactor.isValidTemplateFilter(qrContent)) {
            interactor.addTemplateFilterFromString(
                string = qrContent,
                onSuccess = { filterName, filtersCount ->
                    toastHostState.showToast(
                        message = context.getString(
                            R.string.added_filter_template,
                            filterName,
                            filtersCount
                        ),
                        icon = Icons.Outlined.AutoFixHigh
                    )
                },
                onError = {}
            )
        }
    }

    var qrImageUri by rememberSaveable {
        mutableStateOf<Uri?>(null)
    }

    var qrDescription by rememberSaveable {
        mutableStateOf("")
    }

    val settingsState = LocalSettingsState.current
    var qrDescriptionFont by rememberSaveable(
        inputs = arrayOf(settingsState.font),
        stateSaver = UiFontFamily.Saver
    ) {
        mutableStateOf(settingsState.font)
    }

    val captureController = rememberCaptureController()

    val saveBitmap: (oneTimeSaveLocationUri: String?, bitmap: Bitmap) -> Unit =
        { oneTimeSaveLocationUri, bitmap ->
            viewModel.saveBitmap(bitmap, oneTimeSaveLocationUri) { saveResult ->
                context.parseSaveResult(
                    saveResult = saveResult,
                    onSuccess = {
                        confettiHostState.showConfetti()
                    },
                    toastHostState = toastHostState,
                    scope = scope
                )
            }
        }

    val showConfetti: () -> Unit = {
        scope.launch {
            confettiHostState.showConfetti()
        }
    }

    val isLandscape by isLandscapeOrientationAsState()

    @Composable
    fun QrCodePreview() {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.capturable(captureController)) {
                if (qrImageUri != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .then(
                            if ((qrImageUri != null || qrDescription.isNotEmpty()) && qrContent.isNotEmpty()) {
                                Modifier
                                    .background(
                                        color = takeColorFromScheme {
                                            if (isLandscape) {
                                                surfaceContainerLowest
                                            } else surfaceContainerLow
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            } else Modifier
                        )
                ) {
                    val targetSize = min(min(maxWidth, maxHeight), 300.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QrCode(
                            content = qrContent,
                            modifier = Modifier
                                .padding(
                                    top = if (qrImageUri != null) 36.dp else 0.dp,
                                    bottom = if (qrDescription.isNotEmpty()) 16.dp else 0.dp
                                )
                                .then(
                                    if (isLandscape) {
                                        Modifier
                                            .weight(1f, false)
                                            .aspectRatio(1f)
                                    } else Modifier
                                )
                                .size(targetSize)
                        )

                        BoxAnimatedVisibility(visible = qrDescription.isNotEmpty() && qrContent.isNotEmpty()) {
                            MaterialTheme(
                                typography = Typography(qrDescriptionFont)
                            ) {
                                Text(
                                    text = qrDescription,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(targetSize)
                                )
                            }
                        }
                    }

                    if (qrImageUri != null && qrContent.isNotEmpty()) {
                        Picture(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-48).dp)
                                .size(64.dp),
                            model = qrImageUri,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }
    }

    AdaptiveLayoutScreen(
        title = {
            Text(
                text = stringResource(R.string.qr_code),
                textAlign = TextAlign.Center,
                modifier = Modifier.marquee()
            )
        },
        onGoBack = onGoBack,
        actions = {
            ShareButton(
                enabled = qrContent.isNotEmpty(),
                onShare = {
                    scope.launch {
                        val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                        viewModel.shareImage(bitmap, showConfetti)
                    }
                },
                onCopy = { manager ->
                    scope.launch {
                        val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                        viewModel.cacheImage(bitmap) { uri ->
                            manager.setClip(uri.asClip(context))
                            showConfetti()
                        }
                    }
                }
            )
        },
        topAppBarPersistentActions = {
            TopAppBarEmoji()
        },
        showImagePreviewAsStickyHeader = false,
        imagePreview = {
            if (isLandscape) QrCodePreview()
        },
        controls = {
            if (!isLandscape) {
                Spacer(modifier = Modifier.height(20.dp))
                QrCodePreview()
                Spacer(modifier = Modifier.height(16.dp))
            }
            RoundedTextField(
                modifier = Modifier
                    .container(shape = RoundedCornerShape(24.dp))
                    .padding(8.dp),
                value = qrContent,
                onValueChange = {
                    qrContent = it
                },
                singleLine = false,
                label = {
                    Text(stringResource(id = R.string.code_content))
                },
                endIcon = {
                    AnimatedVisibility(qrContent.isNotBlank()) {
                        EnhancedIconButton(
                            containerColor = Color.Transparent,
                            contentColor = LocalContentColor.current,
                            enableAutoShadowAndBorder = false,
                            onClick = { qrContent = "" },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .container(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(0.2f),
                        resultPadding = 0.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.scan_qr_code_to_replace_content),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(visible = qrContent.isNotEmpty()) {
                Column {
                    Row(
                        modifier = Modifier.height(intrinsicSize = IntrinsicSize.Max)
                    ) {
                        ImageSelector(
                            value = qrImageUri,
                            subtitle = stringResource(id = R.string.watermarking_image_sub),
                            onValueChange = {
                                qrImageUri = it
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        BoxAnimatedVisibility(visible = qrImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 8.dp)
                                    .container(color = MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 8.dp)
                                    .clickable {
                                        qrImageUri = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedTextField(
                        modifier = Modifier
                            .container(
                                shape = animateShape(
                                    if (qrDescription.isNotEmpty()) ContainerShapeDefaults.topShape
                                    else ContainerShapeDefaults.defaultShape
                                )
                            )
                            .padding(8.dp),
                        value = qrDescription,
                        onValueChange = {
                            qrDescription = it
                        },
                        singleLine = false,
                        label = {
                            Text(stringResource(id = R.string.qr_description))
                        }
                    )
                    BoxAnimatedVisibility(visible = qrDescription.isNotEmpty()) {
                        FontSelector(
                            font = qrDescriptionFont,
                            onValueChange = { qrDescriptionFont = it },
                            color = Color.Unspecified,
                            shape = ContainerShapeDefaults.bottomShape,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        buttons = { actions ->
            var showFolderSelectionDialog by rememberSaveable {
                mutableStateOf(false)
            }
            BottomButtonsBlock(
                targetState = (false) to !isLandscape,
                secondaryButtonIcon = Icons.Outlined.QrCodeScanner,
                onSecondaryButtonClick = scanner::scan,
                onPrimaryButtonClick = {
                    scope.launch {
                        val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                        saveBitmap(null, bitmap)
                    }
                },
                onPrimaryButtonLongClick = {
                    showFolderSelectionDialog = true
                },
                actions = {
                    if (!isLandscape) actions()
                }
            )
            if (showFolderSelectionDialog) {
                OneTimeSaveLocationSelectionDialog(
                    onDismiss = { showFolderSelectionDialog = false },
                    onSaveRequest = {
                        scope.launch {
                            val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                            saveBitmap(it, bitmap)
                        }
                    }
                )
            }
        },
        canShowScreenData = true,
        isPortrait = !isLandscape
    )

    if (viewModel.isSaving) {
        LoadingDialog(
            onCancelLoading = viewModel::cancelSaving
        )
    }
}