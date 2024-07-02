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

package ru.tech.imageresizershrinker.feature.draw.domain

sealed class DrawMode(open val ordinal: Int) {
    data object Neon : DrawMode(2)
    data object Highlighter : DrawMode(3)
    data object Pen : DrawMode(0)

    sealed class PathEffect(override val ordinal: Int) : DrawMode(ordinal) {
        data class PrivacyBlur(
            val blurRadius: Int = 20
        ) : PathEffect(1)

        data class Pixelation(
            val pixelSize: Float = 35f
        ) : PathEffect(4)
    }

    data class Text(
        val text: String = "Text",
        val font: Int = 0,
        val isRepeated: Boolean = false,
        val repeatingInterval: Pt = Pt.Zero
    ) : DrawMode(5)

    data class Image(
        val imageData: Any = "file:///android_asset/svg/emotions/aasparkles.svg",
        val repeatingInterval: Pt = Pt.Zero
    ) : DrawMode(6)

    companion object {
        val entries by lazy {
            listOf(
                Pen,
                Text(),
                Neon,
                Highlighter,
                PathEffect.PrivacyBlur(),
                PathEffect.Pixelation(),
                Image()
            )
        }

        operator fun invoke(ordinal: Int) = entries.find {
            it.ordinal == ordinal
        } ?: Pen
    }
}