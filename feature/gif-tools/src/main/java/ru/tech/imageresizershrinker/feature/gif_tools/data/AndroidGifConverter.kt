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

package ru.tech.imageresizershrinker.feature.gif_tools.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlDecodingSpeed
import com.awxkee.jxlcoder.JxlEffort
import com.t8rin.gif_converter.GifDecoder
import com.t8rin.gif_converter.GifEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.image.ShareProvider
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFormat
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFrames
import ru.tech.imageresizershrinker.core.domain.image.model.ImageInfo
import ru.tech.imageresizershrinker.core.domain.image.model.Quality
import ru.tech.imageresizershrinker.feature.gif_tools.domain.GifConverter
import ru.tech.imageresizershrinker.feature.gif_tools.domain.GifParams
import java.io.ByteArrayOutputStream
import javax.inject.Inject


internal class AndroidGifConverter @Inject constructor(
    private val imageGetter: ImageGetter<Bitmap, ExifInterface>,
    private val imageShareProvider: ShareProvider<Bitmap>,
    @ApplicationContext private val context: Context,
    dispatchersHolder: DispatchersHolder
) : DispatchersHolder by dispatchersHolder, GifConverter {

    override fun extractFramesFromGif(
        gifUri: String,
        imageFormat: ImageFormat,
        imageFrames: ImageFrames,
        quality: Quality,
        onGetFramesCount: (frames: Int) -> Unit
    ): Flow<String> = flow {
        val bytes = gifUri.bytes
        val decoder = GifDecoder().apply {
            read(bytes)
        }
        onGetFramesCount(decoder.frameCount)
        val indexes = imageFrames
            .getFramePositions(decoder.frameCount)
            .map { it - 1 }
        repeat(decoder.frameCount) { pos ->
            if (!currentCoroutineContext().isActive) {
                currentCoroutineContext().cancel(null)
                return@repeat
            }
            decoder.advance()
            decoder.nextFrame?.let { frame ->
                imageShareProvider.cacheImage(
                    image = frame,
                    imageInfo = ImageInfo(
                        width = frame.width,
                        height = frame.height,
                        imageFormat = imageFormat,
                        quality = quality
                    )
                )
            }?.takeIf {
                pos in indexes
            }?.let { emit(it) }
        }
    }

    override suspend fun createGifFromImageUris(
        imageUris: List<String>,
        params: GifParams,
        onError: (Throwable) -> Unit,
        onProgress: () -> Unit
    ): ByteArray? = withContext(defaultDispatcher) {
        val out = ByteArrayOutputStream()
        val encoder = GifEncoder().apply {
            params.size?.let { size ->
                if (size.width <= 0 || size.height <= 0) {
                    onError(IllegalArgumentException("Width and height must be > 0"))
                    return@withContext null
                }

                setSize(
                    size.width,
                    size.height
                )
            }
            setRepeat(params.repeatCount)
            setQuality(
                (100 - ((params.quality.qualityValue - 1) * (100 / 19f))).toInt()
            )
            setFrameRate(params.fps.toFloat())
            start(out)
        }
        imageUris.forEach { uri ->
            imageGetter.getImage(
                data = uri,
                size = params.size
            )?.let(encoder::addFrame)
            onProgress()
        }
        encoder.finish()

        out.toByteArray()
    }

    override suspend fun convertGifToJxl(
        gifUris: List<String>,
        quality: Quality.Jxl,
        onProgress: suspend (String, ByteArray) -> Unit
    ) = withContext(defaultDispatcher) {
        gifUris.forEach { uri ->
            uri.bytes?.let { gifData ->
                runCatching {
                    JxlCoder.Convenience.gif2JXL(
                        gifData = gifData,
                        quality = quality.qualityValue,
                        effort = JxlEffort.entries.first { it.ordinal == quality.effort },
                        decodingSpeed = JxlDecodingSpeed.entries.first { it.ordinal == quality.speed }
                    ).let {
                        onProgress(uri, it)
                    }
                }
            }
        }
    }

    private val String.bytes: ByteArray?
        get() = context
            .contentResolver
            .openInputStream(toUri())?.use {
                it.readBytes()
            }
}