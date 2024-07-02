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

@file:Suppress("FunctionName")

package ru.tech.imageresizershrinker.feature.gif_tools.presentation.viewModel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageCompressor
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.image.ShareProvider
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFormat
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFrames
import ru.tech.imageresizershrinker.core.domain.image.model.ImageInfo
import ru.tech.imageresizershrinker.core.domain.image.model.Quality
import ru.tech.imageresizershrinker.core.domain.saving.FileController
import ru.tech.imageresizershrinker.core.domain.saving.model.FileSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveResult
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.onSuccess
import ru.tech.imageresizershrinker.core.domain.utils.smartJob
import ru.tech.imageresizershrinker.core.ui.utils.BaseViewModel
import ru.tech.imageresizershrinker.core.ui.utils.navigation.Screen
import ru.tech.imageresizershrinker.core.ui.utils.state.update
import ru.tech.imageresizershrinker.feature.gif_tools.domain.GifConverter
import ru.tech.imageresizershrinker.feature.gif_tools.domain.GifParams
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GifToolsViewModel @Inject constructor(
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val imageGetter: ImageGetter<Bitmap, ExifInterface>,
    private val fileController: FileController,
    private val gifConverter: GifConverter,
    private val shareProvider: ShareProvider<Bitmap>,
    dispatchersHolder: DispatchersHolder
) : BaseViewModel(dispatchersHolder) {

    private val _type: MutableState<Screen.GifTools.Type?> = mutableStateOf(null)
    val type by _type

    private val _isLoading: MutableState<Boolean> = mutableStateOf(false)
    val isLoading by _isLoading

    private val _isLoadingGifImages: MutableState<Boolean> = mutableStateOf(false)
    val isLoadingGifImages by _isLoadingGifImages

    private val _params: MutableState<GifParams> = mutableStateOf(GifParams.Default)
    val params by _params

    private val _convertedImageUris: MutableState<List<String>> = mutableStateOf(emptyList())
    val convertedImageUris by _convertedImageUris

    private val _imageFormat: MutableState<ImageFormat> = mutableStateOf(ImageFormat.Default())
    val imageFormat by _imageFormat

    private val _imageFrames: MutableState<ImageFrames> = mutableStateOf(ImageFrames.All)
    val gifFrames by _imageFrames

    private val _done: MutableState<Int> = mutableIntStateOf(0)
    val done by _done

    private val _left: MutableState<Int> = mutableIntStateOf(-1)
    val left by _left

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _jxlQuality: MutableState<Quality.Jxl> = mutableStateOf(Quality.Jxl())
    val jxlQuality by _jxlQuality

    private var gifData: ByteArray? = null

    fun setType(type: Screen.GifTools.Type) {
        when (type) {
            is Screen.GifTools.Type.GifToImage -> {
                type.gifUri?.let { setGifUri(it) } ?: _type.update { null }
            }

            is Screen.GifTools.Type.ImageToGif -> {
                _type.update { type }
            }

            is Screen.GifTools.Type.GifToJxl -> {
                _type.update { type }
            }
        }
    }

    fun setImageUris(uris: List<Uri>) {
        clearAll()
        _type.update {
            Screen.GifTools.Type.ImageToGif(uris)
        }
    }

    private var collectionJob: Job? by smartJob {
        _isLoading.update { false }
    }

    fun setGifUri(uri: Uri) {
        clearAll()
        _type.update {
            Screen.GifTools.Type.GifToImage(uri)
        }
        updateGifFrames(ImageFrames.All)

        collectionJob = viewModelScope.launch(defaultDispatcher) {
            _isLoading.update { true }
            _isLoadingGifImages.update { true }
            gifConverter.extractFramesFromGif(
                gifUri = uri.toString(),
                imageFormat = imageFormat,
                imageFrames = ImageFrames.All,
                quality = params.quality
            ).onCompletion {
                _isLoading.update { false }
                _isLoadingGifImages.update { false }
            }.collect { nextUri ->
                if (isLoading) {
                    _isLoading.update { false }
                }
                _convertedImageUris.update { it + nextUri }
            }
        }
    }

    fun clearAll() {
        collectionJob?.cancel()
        collectionJob = null
        _type.update { null }
        _convertedImageUris.update { emptyList() }
        gifData = null
        savingJob?.cancel()
        savingJob = null
        updateParams(GifParams.Default)
        registerChangesCleared()
    }

    fun updateGifFrames(imageFrames: ImageFrames) {
        _imageFrames.update { imageFrames }
        registerChanges()
    }

    fun clearConvertedImagesSelection() = updateGifFrames(ImageFrames.ManualSelection(emptyList()))

    fun selectAllConvertedImages() = updateGifFrames(ImageFrames.All)

    private var savingJob: Job? by smartJob {
        _isSaving.update { false }
    }

    fun saveGifTo(
        uri: Uri,
        onResult: (SaveResult) -> Unit
    ) {
        savingJob = viewModelScope.launch(defaultDispatcher) {
            _isSaving.value = true
            gifData?.let { byteArray ->
                fileController.writeBytes(
                    uri = uri.toString(),
                    block = { it.writeBytes(byteArray) }
                ).also(onResult).onSuccess(::registerSave)
            }
            _isSaving.value = false
            gifData = null
        }
    }

    fun saveBitmaps(
        oneTimeSaveLocationUri: String?,
        onGifSaveResult: (filename: String) -> Unit,
        onResult: (List<SaveResult>) -> Unit
    ) {
        savingJob = viewModelScope.launch(defaultDispatcher) {
            _isSaving.value = true
            _left.value = 1
            _done.value = 0
            when (val type = _type.value) {
                is Screen.GifTools.Type.GifToImage -> {
                    val results = mutableListOf<SaveResult>()
                    type.gifUri?.toString()?.also { gifUri ->
                        gifConverter.extractFramesFromGif(
                            gifUri = gifUri,
                            imageFormat = imageFormat,
                            imageFrames = gifFrames,
                            quality = params.quality,
                            onGetFramesCount = {
                                if (it == 0) {
                                    _isSaving.value = false
                                    savingJob?.cancel()
                                    onResult(
                                        listOf(SaveResult.Error.MissingPermissions)
                                    )
                                }
                                _left.value = gifFrames.getFramePositions(it).size
                            }
                        ).onCompletion {
                            onResult(results.onSuccess(::registerSave))
                        }.collect { uri ->
                            imageGetter.getImage(
                                data = uri,
                                originalSize = true
                            )?.let { localBitmap ->
                                val imageInfo = ImageInfo(
                                    imageFormat = imageFormat,
                                    width = localBitmap.width,
                                    height = localBitmap.height
                                )
                                results.add(
                                    fileController.save(
                                        saveTarget = ImageSaveTarget<ExifInterface>(
                                            imageInfo = imageInfo,
                                            originalUri = uri,
                                            sequenceNumber = _done.value + 1,
                                            data = imageCompressor.compressAndTransform(
                                                image = localBitmap,
                                                imageInfo = ImageInfo(
                                                    imageFormat = imageFormat,
                                                    quality = params.quality,
                                                    width = localBitmap.width,
                                                    height = localBitmap.height
                                                )
                                            )
                                        ),
                                        keepOriginalMetadata = false,
                                        oneTimeSaveLocationUri = oneTimeSaveLocationUri
                                    )
                                )
                            } ?: results.add(
                                SaveResult.Error.Exception(Throwable())
                            )
                            _done.value++
                        }
                    }
                }

                is Screen.GifTools.Type.ImageToGif -> {
                    _left.value = type.imageUris?.size ?: -1
                    gifData = type.imageUris?.map { it.toString() }?.let { list ->
                        gifConverter.createGifFromImageUris(
                            imageUris = list,
                            params = params,
                            onProgress = {
                                _done.update { it + 1 }
                            },
                            onError = {
                                onResult(listOf(SaveResult.Error.Exception(it)))
                            }
                        )?.also {
                            val timeStamp = SimpleDateFormat(
                                "yyyy-MM-dd_HH-mm-ss",
                                Locale.getDefault()
                            ).format(Date())
                            val gifName = "GIF_$timeStamp"
                            onGifSaveResult(gifName)
                        }
                    }
                }

                is Screen.GifTools.Type.GifToJxl -> {
                    val results = mutableListOf<SaveResult>()
                    val gifUris = type.gifUris?.map {
                        it.toString()
                    } ?: emptyList()

                    _left.value = gifUris.size
                    gifConverter.convertGifToJxl(
                        gifUris = gifUris,
                        quality = jxlQuality
                    ) { uri, jxlBytes ->
                        results.add(
                            fileController.save(
                                saveTarget = JxlSaveTarget(uri, jxlBytes),
                                keepOriginalMetadata = true,
                                oneTimeSaveLocationUri = oneTimeSaveLocationUri
                            )
                        )
                        _done.update { it + 1 }
                    }

                    onResult(results.onSuccess(::registerSave))
                }

                null -> Unit
            }
            _isSaving.value = false
        }
    }

    private fun JxlSaveTarget(
        uri: String,
        jxlBytes: ByteArray
    ): SaveTarget = FileSaveTarget(
        originalUri = uri,
        filename = jxlFilename(uri),
        data = jxlBytes,
        imageFormat = ImageFormat.Jxl.Lossless
    )

    private fun jxlFilename(
        uri: String
    ): String = fileController.constructImageFilename(
        ImageSaveTarget<ExifInterface>(
            imageInfo = ImageInfo(
                imageFormat = ImageFormat.Jxl.Lossless,
                originalUri = uri
            ),
            originalUri = uri,
            sequenceNumber = done + 1,
            metadata = null,
            data = ByteArray(0)
        ),
        forceNotAddSizeInFilename = true
    )

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
    }

    fun reorderImageUris(uris: List<Uri>?) {
        if (type is Screen.GifTools.Type.ImageToGif) {
            _type.update {
                Screen.GifTools.Type.ImageToGif(uris)
            }
            registerChanges()
        }
    }

    fun addImageToUris(uris: List<Uri>) {
        val type = _type.value
        if (type is Screen.GifTools.Type.ImageToGif) {
            _type.update {
                val newUris = type.imageUris?.plus(uris)?.toSet()?.toList()

                Screen.GifTools.Type.ImageToGif(newUris)
            }
            registerChanges()
        }
    }

    fun removeImageAt(index: Int) {
        val type = _type.value
        if (type is Screen.GifTools.Type.ImageToGif) {
            _type.update {
                val newUris = type.imageUris?.toMutableList()?.apply {
                    removeAt(index)
                }

                Screen.GifTools.Type.ImageToGif(newUris)
            }
            registerChanges()
        }
    }

    fun setImageFormat(imageFormat: ImageFormat) {
        _imageFormat.update { imageFormat }
        registerChanges()
    }

    fun setQuality(quality: Quality) {
        updateParams(params.copy(quality = quality))
    }

    fun updateParams(params: GifParams) {
        _params.update { params }
        registerChanges()
    }

    fun performSharing(onComplete: () -> Unit) {
        savingJob = viewModelScope.launch(defaultDispatcher) {
            _isSaving.value = true
            _left.value = 1
            _done.value = 0
            when (val type = _type.value) {
                is Screen.GifTools.Type.GifToImage -> {
                    _left.value = -1
                    val positions =
                        gifFrames.getFramePositions(convertedImageUris.size).map { it - 1 }
                    val uris = convertedImageUris.filterIndexed { index, _ ->
                        index in positions
                    }
                    shareProvider.shareUris(uris)
                    onComplete()
                }

                is Screen.GifTools.Type.ImageToGif -> {
                    _left.value = type.imageUris?.size ?: -1
                    type.imageUris?.map { it.toString() }?.let { list ->
                        gifConverter.createGifFromImageUris(
                            imageUris = list,
                            params = params,
                            onProgress = {
                                _done.update { it + 1 }
                            },
                            onError = { }
                        )?.also { byteArray ->
                            val timeStamp = SimpleDateFormat(
                                "yyyy-MM-dd_HH-mm-ss",
                                Locale.getDefault()
                            ).format(Date())
                            val gifName = "GIF_$timeStamp"
                            shareProvider.shareByteArray(
                                byteArray = byteArray,
                                filename = "$gifName.gif",
                                onComplete = onComplete
                            )
                        }
                    }
                }

                is Screen.GifTools.Type.GifToJxl -> {
                    val results = mutableListOf<String?>()
                    val gifUris = type.gifUris?.map {
                        it.toString()
                    } ?: emptyList()

                    _left.value = gifUris.size
                    gifConverter.convertGifToJxl(
                        gifUris = gifUris,
                        quality = jxlQuality
                    ) { uri, jxlBytes ->
                        results.add(
                            shareProvider.cacheByteArray(
                                byteArray = jxlBytes,
                                filename = jxlFilename(uri)
                            )
                        )
                        _done.update { it + 1 }
                    }

                    shareProvider.shareUris(results.filterNotNull())
                }

                null -> Unit
            }
            _isSaving.value = false
        }
    }

    fun setJxlQuality(quality: Quality) {
        _jxlQuality.update {
            (quality as? Quality.Jxl) ?: Quality.Jxl()
        }
        registerChanges()
    }

}