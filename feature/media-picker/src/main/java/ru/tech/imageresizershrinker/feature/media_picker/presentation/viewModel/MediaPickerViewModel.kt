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
package ru.tech.imageresizershrinker.feature.media_picker.presentation.viewModel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.t8rin.dynamic.theme.ColorTuple
import com.t8rin.dynamic.theme.extractPrimaryColor
import com.t8rin.logger.makeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.utils.smartJob
import ru.tech.imageresizershrinker.core.settings.domain.SettingsManager
import ru.tech.imageresizershrinker.core.settings.domain.model.SettingsState
import ru.tech.imageresizershrinker.core.ui.utils.BaseViewModel
import ru.tech.imageresizershrinker.feature.media_picker.data.utils.DateExt
import ru.tech.imageresizershrinker.feature.media_picker.data.utils.getDate
import ru.tech.imageresizershrinker.feature.media_picker.data.utils.getDateExt
import ru.tech.imageresizershrinker.feature.media_picker.data.utils.getDateHeader
import ru.tech.imageresizershrinker.feature.media_picker.data.utils.getMonth
import ru.tech.imageresizershrinker.feature.media_picker.domain.MediaRetriever
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.Album
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.AlbumState
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.AllowedMedia
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.Media
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.MediaItem
import ru.tech.imageresizershrinker.feature.media_picker.domain.model.MediaState
import javax.inject.Inject

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    val imageLoader: ImageLoader,
    private val imageGetter: ImageGetter<Bitmap, ExifInterface>,
    private val settingsManager: SettingsManager,
    private val mediaRetriever: MediaRetriever,
    dispatchersHolder: DispatchersHolder
) : BaseViewModel(dispatchersHolder) {

    private val _settingsState = mutableStateOf(SettingsState.Default)
    val settingsState: SettingsState by _settingsState

    val selectedMedia = mutableStateListOf<Media>()

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()

    private val _albumsState = MutableStateFlow(AlbumState())
    val albumsState = _albumsState.asStateFlow()

    fun init(allowedMedia: AllowedMedia) {
        this.allowedMedia = allowedMedia
        getMedia(selectedAlbumId, allowedMedia)
        getAlbums(allowedMedia)
    }

    fun getAlbum(albumId: Long) {
        this.selectedAlbumId = albumId
        getMedia(albumId, allowedMedia)
        getAlbums(allowedMedia)
    }

    private var allowedMedia: AllowedMedia = AllowedMedia.Photos(null)

    private var selectedAlbumId: Long = -1L

    private val emptyAlbum = Album(
        id = -1,
        label = "All",
        uri = "",
        pathToThumbnail = "",
        timestamp = 0,
        relativePath = ""
    )

    private var albumJob: Job? by smartJob()

    private fun getAlbums(allowedMedia: AllowedMedia) {
        albumJob = viewModelScope.launch(defaultDispatcher) {
            mediaRetriever.getAlbumsWithType(allowedMedia)
                .flowOn(defaultDispatcher)
                .collectLatest { result ->
                    val data = result.getOrNull() ?: emptyList()
                    val error = if (result.isFailure) result.exceptionOrNull().makeLog()?.message
                        ?: "An error occurred" else ""
                    if (data.isEmpty()) {
                        return@collectLatest _albumsState.emit(
                            AlbumState(
                                albums = listOf(emptyAlbum),
                                error = error
                            )
                        )
                    }
                    val albums = mutableListOf<Album>().apply {
                        add(emptyAlbum)
                        addAll(data)
                    }
                    _albumsState.emit(AlbumState(albums = albums, error = error))
                }
        }
    }

    private var mediaGettingJob: Job? by smartJob()

    private fun getMedia(
        albumId: Long,
        allowedMedia: AllowedMedia
    ) {
        mediaGettingJob = viewModelScope.launch(defaultDispatcher) {
            _mediaState.emit(mediaState.value.copy(isLoading = true))
            mediaRetriever.mediaFlowWithType(albumId, allowedMedia)
                .flowOn(defaultDispatcher)
                .collectLatest { result ->
                    val data = result.getOrNull()?.filter {
                        if (allowedMedia is AllowedMedia.Photos) {
                            val ext = allowedMedia.ext
                            if (ext != null && ext != "*") {
                                it.uri.endsWith(ext)
                            } else true
                        } else true
                    } ?: emptyList()
                    val error = if (result.isFailure) result.exceptionOrNull().makeLog()?.message
                        ?: "An error occurred" else ""
                    if (data.isEmpty()) {
                        return@collectLatest _mediaState.emit(MediaState(isLoading = false))
                    }
                    _mediaState.collectMedia(data, error, albumId)
                }
        }
    }

    private suspend fun MutableStateFlow<MediaState>.collectMedia(
        data: List<Media>,
        error: String,
        albumId: Long,
        groupByMonth: Boolean = false,
        withMonthHeader: Boolean = true
    ) {
        val mappedData = mutableListOf<MediaItem>()
        val mappedDataWithMonthly = mutableListOf<MediaItem>()
        val monthHeaderList: MutableSet<String> = mutableSetOf()
        withContext(defaultDispatcher) {
            val groupedData = data.groupBy {
                if (groupByMonth) {
                    it.timestamp.getMonth()
                } else {
                    /** Localized in composition */
                    it.timestamp.getDate(
                        stringToday = "Today",
                        stringYesterday = "Yesterday"
                    )
                }
            }
            groupedData.forEach { (date, data) ->
                val dateHeader = MediaItem.Header("header_$date", date, data)
                val groupedMedia = data.map {
                    MediaItem.MediaViewItem("media_${it.id}_${it.label}", it)
                }
                if (groupByMonth) {
                    mappedData.add(dateHeader)
                    mappedData.addAll(groupedMedia)
                    mappedDataWithMonthly.add(dateHeader)
                    mappedDataWithMonthly.addAll(groupedMedia)
                } else {
                    val month = getMonth(date)
                    if (month.isNotEmpty() && !monthHeaderList.contains(month)) {
                        monthHeaderList.add(month)
                        if (withMonthHeader && mappedDataWithMonthly.isNotEmpty()) {
                            mappedDataWithMonthly.add(
                                MediaItem.Header(
                                    "header_big_${month}_${data.size}",
                                    month,
                                    emptyList()
                                )
                            )
                        }
                    }
                    mappedData.add(dateHeader)
                    if (withMonthHeader) {
                        mappedDataWithMonthly.add(dateHeader)
                    }
                    mappedData.addAll(groupedMedia)
                    if (withMonthHeader) {
                        mappedDataWithMonthly.addAll(groupedMedia)
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            tryEmit(
                MediaState(
                    isLoading = false,
                    error = error,
                    media = data,
                    mappedMedia = mappedData,
                    mappedMediaWithMonthly = if (withMonthHeader) mappedDataWithMonthly else emptyList(),
                    dateHeader = data.dateHeader(albumId)
                )
            )
        }
    }

    private fun List<Media>.dateHeader(albumId: Long): String =
        if (albumId != -1L) {
            val startDate: DateExt = last().timestamp.getDateExt()
            val endDate: DateExt = first().timestamp.getDateExt()
            getDateHeader(startDate, endDate)
        } else ""

    suspend fun getColorTupleFromEmoji(
        emojiUri: String
    ): ColorTuple? = imageGetter
        .getImage(data = emojiUri)
        ?.extractPrimaryColor()
        ?.let { ColorTuple(it) }

    init {
        runBlocking {
            _settingsState.value = settingsManager.getSettingsState()
        }
        settingsManager.getSettingsStateFlow().onEach {
            _settingsState.value = it
        }.launchIn(viewModelScope)
    }

}