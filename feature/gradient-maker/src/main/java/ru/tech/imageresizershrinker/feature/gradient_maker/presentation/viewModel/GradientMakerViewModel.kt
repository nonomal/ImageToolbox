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

package ru.tech.imageresizershrinker.feature.gradient_maker.presentation.viewModel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewModelScope
import coil.transform.Transformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import ru.tech.imageresizershrinker.core.data.utils.toCoil
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageCompressor
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.image.ShareProvider
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFormat
import ru.tech.imageresizershrinker.core.domain.image.model.ImageInfo
import ru.tech.imageresizershrinker.core.domain.model.IntegerSize
import ru.tech.imageresizershrinker.core.domain.saving.FileController
import ru.tech.imageresizershrinker.core.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveResult
import ru.tech.imageresizershrinker.core.domain.saving.model.onSuccess
import ru.tech.imageresizershrinker.core.domain.transformation.GenericTransformation
import ru.tech.imageresizershrinker.core.domain.utils.smartJob
import ru.tech.imageresizershrinker.core.ui.utils.BaseViewModel
import ru.tech.imageresizershrinker.core.ui.utils.helper.ImageUtils.safeAspectRatio
import ru.tech.imageresizershrinker.core.ui.utils.state.update
import ru.tech.imageresizershrinker.feature.gradient_maker.domain.GradientMaker
import ru.tech.imageresizershrinker.feature.gradient_maker.domain.GradientType
import ru.tech.imageresizershrinker.feature.gradient_maker.presentation.components.UiGradientState
import javax.inject.Inject

@HiltViewModel
class GradientMakerViewModel @Inject constructor(
    private val fileController: FileController,
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val shareProvider: ShareProvider<Bitmap>,
    private val imageGetter: ImageGetter<Bitmap, ExifInterface>,
    private val gradientMaker: GradientMaker<Bitmap, ShaderBrush, Size, Color, TileMode, Offset>,
    dispatchersHolder: DispatchersHolder
) : BaseViewModel(dispatchersHolder) {

    private var _gradientState = UiGradientState()
    private val gradientState: UiGradientState get() = _gradientState

    val brush: ShaderBrush? get() = gradientState.brush
    val gradientType: GradientType get() = gradientState.gradientType
    val colorStops: List<Pair<Float, Color>> get() = gradientState.colorStops
    val tileMode: TileMode get() = gradientState.tileMode
    val angle: Float get() = gradientState.linearGradientAngle
    val centerFriction: Offset get() = gradientState.centerFriction
    val radiusFriction: Float get() = gradientState.radiusFriction

    private var _gradientAlpha: MutableState<Float> = mutableFloatStateOf(1f)
    val gradientAlpha by _gradientAlpha

    private val _keepExif = mutableStateOf(false)
    val keepExif by _keepExif

    private val _selectedUri = mutableStateOf(Uri.EMPTY)
    val selectedUri: Uri by _selectedUri

    private val _uris = mutableStateOf<List<Uri>>(emptyList())
    val uris by _uris

    private val _imageAspectRatio: MutableState<Float> = mutableFloatStateOf(1f)
    val imageAspectRatio by _imageAspectRatio

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _imageFormat = mutableStateOf(ImageFormat.Default())
    val imageFormat by _imageFormat

    private val _gradientSize: MutableState<IntegerSize> = mutableStateOf(IntegerSize(1000, 1000))
    val gradientSize by _gradientSize

    suspend fun createGradientBitmap(
        data: Any,
        integerSize: IntegerSize = gradientSize,
        useBitmapOriginalSizeIfAvailable: Boolean = false
    ): Bitmap? {
        return if (selectedUri == Uri.EMPTY) {
            gradientMaker.createGradientBitmap(
                integerSize = integerSize,
                gradientState = gradientState
            )
        } else {
            imageGetter.getImage(
                data = data,
                originalSize = useBitmapOriginalSizeIfAvailable
            )?.let {
                gradientMaker.createGradientBitmap(
                    src = it,
                    gradientState = gradientState,
                    gradientAlpha = gradientAlpha
                )
            }
        }
    }

    private val _done: MutableState<Int> = mutableIntStateOf(0)
    val done by _done

    private val _left: MutableState<Int> = mutableIntStateOf(-1)
    val left by _left

    private var savingJob: Job? by smartJob {
        _isSaving.update { false }
    }

    fun saveBitmaps(
        oneTimeSaveLocationUri: String?,
        onStandaloneGradientSaveResult: (SaveResult) -> Unit,
        onResult: (List<SaveResult>) -> Unit
    ) {
        savingJob = viewModelScope.launch(defaultDispatcher) {
            _left.value = -1
            _isSaving.value = true
            if (uris.isEmpty()) {
                createGradientBitmap(
                    data = Unit,
                    useBitmapOriginalSizeIfAvailable = true
                )?.let { localBitmap ->
                    val imageInfo = ImageInfo(
                        imageFormat = imageFormat,
                        width = localBitmap.width,
                        height = localBitmap.height
                    )
                    onStandaloneGradientSaveResult(
                        fileController.save(
                            saveTarget = ImageSaveTarget<ExifInterface>(
                                imageInfo = imageInfo,
                                originalUri = "Gradient",
                                sequenceNumber = null,
                                data = imageCompressor.compressAndTransform(
                                    image = localBitmap,
                                    imageInfo = imageInfo
                                )
                            ),
                            keepOriginalMetadata = false,
                            oneTimeSaveLocationUri = oneTimeSaveLocationUri
                        ).onSuccess(::registerSave)
                    )
                }
            } else {
                val results = mutableListOf<SaveResult>()
                _done.value = 0
                _left.value = uris.size
                uris.forEach { uri ->
                    createGradientBitmap(
                        data = uri,
                        useBitmapOriginalSizeIfAvailable = true
                    )?.let { localBitmap ->
                        val imageInfo = ImageInfo(
                            imageFormat = imageFormat,
                            width = localBitmap.width,
                            height = localBitmap.height,
                            originalUri = uri.toString()
                        )
                        results.add(
                            fileController.save(
                                saveTarget = ImageSaveTarget<ExifInterface>(
                                    imageInfo = imageInfo,
                                    originalUri = uri.toString(),
                                    sequenceNumber = _done.value + 1,
                                    data = imageCompressor.compressAndTransform(
                                        image = localBitmap,
                                        imageInfo = imageInfo
                                    )
                                ),
                                keepOriginalMetadata = keepExif,
                                oneTimeSaveLocationUri = oneTimeSaveLocationUri
                            )
                        )
                    } ?: results.add(
                        SaveResult.Error.Exception(Throwable())
                    )

                    _done.value += 1
                }
                onResult(results.onSuccess(::registerSave))
            }
            _isSaving.value = false
        }
    }

    fun shareBitmaps(onComplete: () -> Unit) {
        savingJob = viewModelScope.launch {
            _left.value = -1
            _isSaving.value = true
            if (uris.isEmpty()) {
                createGradientBitmap(
                    data = Unit,
                    useBitmapOriginalSizeIfAvailable = true
                )?.let {
                    shareProvider.shareImage(
                        image = it,
                        imageInfo = ImageInfo(
                            imageFormat = imageFormat,
                            width = it.width,
                            height = it.height
                        ),
                        onComplete = onComplete
                    )
                }
            } else {
                _done.value = 0
                _left.value = uris.size
                shareProvider.shareImages(
                    uris.map { it.toString() },
                    imageLoader = { uri ->
                        createGradientBitmap(
                            data = uri,
                            useBitmapOriginalSizeIfAvailable = true
                        )?.let {
                            it to ImageInfo(
                                width = it.width,
                                height = it.height,
                                imageFormat = imageFormat
                            )
                        }
                    },
                    onProgressChange = {
                        if (it == -1) {
                            onComplete()
                            _isSaving.value = false
                            _done.value = 0
                        } else {
                            _done.value = it
                        }
                    }
                )
            }
            _isSaving.value = false
            _left.value = -1
        }
    }

    fun cancelSaving() {
        savingJob = null
        _isSaving.value = false
        _left.value = -1
    }

    fun updateHeight(value: Int) {
        _gradientSize.update {
            it.copy(height = value)
        }
        registerChanges()
    }

    fun updateWidth(value: Int) {
        _gradientSize.update {
            it.copy(width = value)
        }
        registerChanges()
    }

    fun setGradientType(gradientType: GradientType) {
        gradientState.gradientType = gradientType
        registerChanges()
    }

    fun setPreviewSize(size: Size) {
        gradientState.size = size
    }

    fun setImageFormat(imageFormat: ImageFormat) {
        _imageFormat.update { imageFormat }
        registerChanges()
    }

    fun updateLinearAngle(angle: Float) {
        gradientState.linearGradientAngle = angle
        registerChanges()
    }

    fun setRadialProperties(
        center: Offset,
        radius: Float
    ) {
        gradientState.centerFriction = center
        gradientState.radiusFriction = radius
        registerChanges()
    }

    fun setTileMode(tileMode: TileMode) {
        gradientState.tileMode = tileMode
        registerChanges()
    }

    fun addColorStop(pair: Pair<Float, Color>) {
        gradientState.colorStops.add(pair)
        registerChanges()
    }

    fun updateColorStop(
        index: Int,
        pair: Pair<Float, Color>
    ) {
        gradientState.colorStops[index] = pair.copy()
        registerChanges()
    }

    fun removeColorStop(index: Int) {
        if (gradientState.colorStops.size > 2) {
            gradientState.colorStops.removeAt(index)
            registerChanges()
        }
    }

    fun setUri(
        uri: Uri,
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        _selectedUri.value = uri
        _isImageLoading.value = true
        imageGetter.getImageAsync(
            uri = uri.toString(),
            originalSize = false,
            onGetImage = { imageData ->
                _imageAspectRatio.update {
                    imageData.image.safeAspectRatio
                }
                _isImageLoading.value = false
                setImageFormat(imageData.imageInfo.imageFormat)
            },
            onError = {
                _isImageLoading.value = false
                onError(it)
            }
        )
    }

    fun updateGradientAlpha(value: Float) {
        _gradientAlpha.update { value }
        registerChanges()
    }

    fun clearState() {
        _selectedUri.update { Uri.EMPTY }
        _uris.update { emptyList() }
        _gradientAlpha.update { 1f }
        _gradientState = UiGradientState()
        registerChangesCleared()
    }

    fun updateUrisSilently(
        removedUri: Uri
    ) = viewModelScope.launch(defaultDispatcher) {
        if (selectedUri == removedUri) {
            val index = uris.indexOf(removedUri)
            if (index == 0) {
                uris.getOrNull(1)?.let(::setUri)
            } else {
                uris.getOrNull(index - 1)?.let(::setUri)
            }
        }
        _uris.update {
            it.toMutableList().apply {
                remove(removedUri)
            }
        }
    }

    fun setUris(
        uris: List<Uri>,
        onError: (Throwable) -> Unit = {}
    ) {
        _uris.update { uris }
        uris.firstOrNull()?.let { setUri(it, onError) }
    }

    fun getGradientTransformation(): Transformation =
        GenericTransformation<Bitmap>(brush) { input ->
            createGradientBitmap(
                data = input,
                useBitmapOriginalSizeIfAvailable = false
            ) ?: input
        }.toCoil()

    fun toggleKeepExif(value: Boolean) {
        _keepExif.update { value }
        registerChanges()
    }

    fun cacheCurrentImage(onComplete: (Uri) -> Unit) {
        _isSaving.value = false
        savingJob?.cancel()
        savingJob = viewModelScope.launch {
            _isSaving.value = true
            createGradientBitmap(
                data = selectedUri,
                useBitmapOriginalSizeIfAvailable = true
            )?.let { image ->
                shareProvider.cacheImage(
                    image = image,
                    imageInfo = ImageInfo(
                        imageFormat = imageFormat,
                        width = image.width,
                        height = image.height
                    )
                )?.let { uri ->
                    onComplete(uri.toUri())
                }
            }
            _isSaving.value = false
        }
    }

    fun cacheImages(
        onComplete: (List<Uri>) -> Unit
    ) {
        savingJob = viewModelScope.launch {
            val list = mutableListOf<Uri>()

            _left.value = -1
            _isSaving.value = true

            if (uris.isEmpty()) {
                createGradientBitmap(
                    data = Unit,
                    useBitmapOriginalSizeIfAvailable = true
                )?.let { localBitmap ->
                    val imageInfo = ImageInfo(
                        imageFormat = imageFormat,
                        width = localBitmap.width,
                        height = localBitmap.height
                    )
                    shareProvider.cacheImage(
                        image = localBitmap,
                        imageInfo = imageInfo
                    )?.toUri()?.let(list::add)
                }
            } else {
                _done.value = 0
                _left.value = uris.size
                uris.forEach { uri ->
                    createGradientBitmap(
                        data = uri,
                        useBitmapOriginalSizeIfAvailable = true
                    )?.let { localBitmap ->
                        val imageInfo = ImageInfo(
                            imageFormat = imageFormat,
                            width = localBitmap.width,
                            height = localBitmap.height,
                            originalUri = uri.toString()
                        )

                        shareProvider.cacheImage(
                            image = localBitmap,
                            imageInfo = imageInfo
                        )?.toUri()?.let(list::add)
                    }

                    _done.value += 1
                }
            }
            _isSaving.value = false

            onComplete(list)
            _isSaving.value = false
        }
    }

}