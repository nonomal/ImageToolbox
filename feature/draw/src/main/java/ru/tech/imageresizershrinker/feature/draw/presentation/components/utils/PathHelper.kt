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

package ru.tech.imageresizershrinker.feature.draw.presentation.components.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import ru.tech.imageresizershrinker.core.domain.model.IntegerSize
import ru.tech.imageresizershrinker.core.ui.utils.helper.rotateVector
import ru.tech.imageresizershrinker.feature.draw.domain.DrawPathMode
import ru.tech.imageresizershrinker.feature.draw.domain.Pt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Suppress("MemberVisibilityCanBePrivate")
data class PathHelper(
    val drawDownPosition: Offset,
    val currentDrawPosition: Offset,
    val onPathChange: (Path) -> Unit,
    val strokeWidth: Pt,
    val canvasSize: IntegerSize,
    val drawPathMode: DrawPathMode,
    val isEraserOn: Boolean,
) {

    private val drawArrowsScope by lazy {
        object : DrawArrowsScope {
            override fun drawArrowsIfNeeded(
                drawPath: Path,
            ) {
                when (drawPathMode) {
                    DrawPathMode.DoublePointingArrow,
                    DrawPathMode.DoubleLinePointingArrow -> {

                        drawEndArrow(
                            drawPath = drawPath,
                            strokeWidth = strokeWidth,
                            canvasSize = canvasSize
                        )

                        drawStartArrow(
                            drawPath = drawPath,
                            strokeWidth = strokeWidth,
                            canvasSize = canvasSize
                        )
                    }

                    DrawPathMode.PointingArrow,
                    DrawPathMode.LinePointingArrow -> {
                        drawEndArrow(
                            drawPath = drawPath,
                            strokeWidth = strokeWidth,
                            canvasSize = canvasSize
                        )
                    }

                    else -> Unit
                }
            }

            private fun drawEndArrow(
                drawPath: Path,
                strokeWidth: Pt,
                canvasSize: IntegerSize,
            ) {
                val (preLastPoint, lastPoint) = PathMeasure().apply {
                    setPath(drawPath, false)
                }.let {
                    Pair(
                        it.getPosition(it.length - strokeWidth.toPx(canvasSize) * 3f)
                            .takeOrElse { Offset.Zero },
                        it.getPosition(it.length).takeOrElse { Offset.Zero }
                    )
                }

                val arrowVector = lastPoint - preLastPoint
                fun drawArrow() {

                    val (rx1, ry1) = arrowVector.rotateVector(150.0)
                    val (rx2, ry2) = arrowVector.rotateVector(210.0)


                    drawPath.apply {
                        relativeLineTo(rx1, ry1)
                        moveTo(lastPoint.x, lastPoint.y)
                        relativeLineTo(rx2, ry2)
                    }
                }

                if (abs(arrowVector.x) < 3f * strokeWidth.toPx(canvasSize) && abs(
                        arrowVector.y
                    ) < 3f * strokeWidth.toPx(canvasSize) && preLastPoint != Offset.Zero
                ) {
                    drawArrow()
                }
            }

            private fun drawStartArrow(
                drawPath: Path,
                strokeWidth: Pt,
                canvasSize: IntegerSize,
            ) {
                val (firstPoint, secondPoint) = PathMeasure().apply {
                    setPath(drawPath, false)
                }.let {
                    Pair(
                        it.getPosition(0f).takeOrElse { Offset.Zero },
                        it.getPosition(strokeWidth.toPx(canvasSize) * 3f).takeOrElse { Offset.Zero }
                    )
                }

                val arrowVector = firstPoint - secondPoint
                fun drawArrow() {

                    val (rx1, ry1) = arrowVector.rotateVector(150.0)
                    val (rx2, ry2) = arrowVector.rotateVector(210.0)


                    drawPath.apply {
                        moveTo(firstPoint.x, firstPoint.y)
                        relativeLineTo(rx1, ry1)
                        moveTo(firstPoint.x, firstPoint.y)
                        relativeLineTo(rx2, ry2)
                    }
                }

                if (abs(arrowVector.x) < 3f * strokeWidth.toPx(canvasSize) && abs(
                        arrowVector.y
                    ) < 3f * strokeWidth.toPx(canvasSize) && secondPoint != Offset.Zero
                ) {
                    drawArrow()
                }
            }
        }
    }

    fun drawPolygon(
        vertices: Int,
        rotationDegrees: Int,
        isRegular: Boolean,
    ) {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val top = max(drawDownPosition.y, currentDrawPosition.y)
            val left = min(drawDownPosition.x, currentDrawPosition.x)
            val bottom = min(drawDownPosition.y, currentDrawPosition.y)
            val right = max(drawDownPosition.x, currentDrawPosition.x)

            val width = right - left
            val height = bottom - top
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val radius = min(width, height) / 2f

            val newPath = Path().apply {
                if (isRegular) {
                    val angleStep = 360f / vertices
                    val startAngle = rotationDegrees - 180.0
                    moveTo(
                        centerX + radius * cos(Math.toRadians(startAngle)).toFloat(),
                        centerY + radius * sin(Math.toRadians(startAngle)).toFloat()
                    )
                    for (i in 1 until vertices) {
                        val angle = startAngle + i * angleStep
                        lineTo(
                            centerX + radius * cos(Math.toRadians(angle)).toFloat(),
                            centerY + radius * sin(Math.toRadians(angle)).toFloat()
                        )
                    }
                } else {
                    for (i in 0 until vertices) {
                        val angle = i * (360f / vertices) + rotationDegrees
                        val x =
                            centerX + width / 2f * cos(Math.toRadians(angle.toDouble())).toFloat()
                        val y =
                            centerY + height / 2f * sin(Math.toRadians(angle.toDouble())).toFloat()
                        if (i == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }
                close()
            }
            onPathChange(newPath)
        }
    }

    fun drawStar(
        vertices: Int,
        innerRadiusRatio: Float,
        rotationDegrees: Int,
        isRegular: Boolean,
    ) {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val top = max(drawDownPosition.y, currentDrawPosition.y)
            val left = min(drawDownPosition.x, currentDrawPosition.x)
            val bottom = min(drawDownPosition.y, currentDrawPosition.y)
            val right = max(drawDownPosition.x, currentDrawPosition.x)

            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val width = right - left
            val height = bottom - top

            val newPath = Path().apply {
                if (isRegular) {
                    val outerRadius = min(width, height) / 2f
                    val innerRadius = outerRadius * innerRadiusRatio

                    val angleStep = 360f / (2 * vertices)
                    val startAngle = rotationDegrees - 180.0

                    for (i in 0 until (2 * vertices)) {
                        val radius = if (i % 2 == 0) outerRadius else innerRadius
                        val angle = startAngle + i * angleStep
                        val x = centerX + radius * cos(Math.toRadians(angle)).toFloat()
                        val y = centerY + radius * sin(Math.toRadians(angle)).toFloat()
                        if (i == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                } else {
                    for (i in 0 until (2 * vertices)) {
                        val angle =
                            i * (360f / (2 * vertices)) + rotationDegrees.toDouble()
                        val radiusX =
                            (if (i % 2 == 0) width else width * innerRadiusRatio) / 2f
                        val radiusY =
                            (if (i % 2 == 0) height else height * innerRadiusRatio) / 2f

                        val x = centerX + radiusX * cos(Math.toRadians(angle)).toFloat()
                        val y = centerY + radiusY * sin(Math.toRadians(angle)).toFloat()
                        if (i == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }
                close()
            }

            onPathChange(newPath)
        }
    }

    fun drawTriangle() {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val newPath = Path().apply {
                moveTo(drawDownPosition.x, drawDownPosition.y)

                lineTo(currentDrawPosition.x, drawDownPosition.y)
                lineTo(
                    (drawDownPosition.x + currentDrawPosition.x) / 2,
                    currentDrawPosition.y
                )
                lineTo(drawDownPosition.x, drawDownPosition.y)
                close()
            }
            onPathChange(newPath)
        }
    }

    fun drawRect() {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val top = max(drawDownPosition.y, currentDrawPosition.y)
            val left = min(drawDownPosition.x, currentDrawPosition.x)
            val bottom = min(drawDownPosition.y, currentDrawPosition.y)
            val right = max(drawDownPosition.x, currentDrawPosition.x)

            val newPath = Path().apply {
                moveTo(left, top)
                lineTo(right, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                lineTo(left, top)
                close()
            }
            onPathChange(newPath)
        }
    }

    fun drawOval() {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val newPath = Path().apply {
                addOval(
                    Rect(
                        top = max(
                            drawDownPosition.y,
                            currentDrawPosition.y
                        ),
                        left = min(
                            drawDownPosition.x,
                            currentDrawPosition.x
                        ),
                        bottom = min(
                            drawDownPosition.y,
                            currentDrawPosition.y
                        ),
                        right = max(
                            drawDownPosition.x,
                            currentDrawPosition.x
                        ),
                    )
                )
            }
            onPathChange(newPath)
        }
    }

    fun drawLine() {
        if (drawDownPosition.isSpecified && currentDrawPosition.isSpecified) {
            val newPath = Path().apply {
                moveTo(drawDownPosition.x, drawDownPosition.y)
                lineTo(currentDrawPosition.x, currentDrawPosition.y)
            }
            drawArrowsScope.drawArrowsIfNeeded(newPath)

            onPathChange(newPath)
        }
    }

    fun drawPath(
        onDrawFreeArrow: DrawArrowsScope.() -> Unit,
        onBaseDraw: () -> Unit,
    ) = if (!isEraserOn) {
        when (drawPathMode) {
            DrawPathMode.PointingArrow,
            DrawPathMode.DoublePointingArrow -> onDrawFreeArrow(drawArrowsScope)

            DrawPathMode.DoubleLinePointingArrow,
            DrawPathMode.Line,
            DrawPathMode.LinePointingArrow -> drawLine()

            DrawPathMode.Rect,
            DrawPathMode.OutlinedRect -> drawRect()

            DrawPathMode.Triangle,
            DrawPathMode.OutlinedTriangle -> drawTriangle()

            is DrawPathMode.Polygon -> {
                drawPolygon(
                    vertices = drawPathMode.vertices,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )
            }

            is DrawPathMode.OutlinedPolygon -> {
                drawPolygon(
                    vertices = drawPathMode.vertices,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )
            }

            is DrawPathMode.Star -> {
                drawStar(
                    vertices = drawPathMode.vertices,
                    innerRadiusRatio = drawPathMode.innerRadiusRatio,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )
            }

            is DrawPathMode.OutlinedStar -> {
                drawStar(
                    vertices = drawPathMode.vertices,
                    innerRadiusRatio = drawPathMode.innerRadiusRatio,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )
            }

            DrawPathMode.Oval,
            DrawPathMode.OutlinedOval -> drawOval()

            DrawPathMode.Free,
            DrawPathMode.Lasso -> onBaseDraw()
        }
    } else onBaseDraw()
}

interface DrawArrowsScope {
    fun drawArrowsIfNeeded(
        drawPath: Path,
    )
}

@Composable
fun rememberPathHelper(
    drawDownPosition: Offset,
    currentDrawPosition: Offset,
    onPathChange: (Path) -> Unit,
    strokeWidth: Pt,
    canvasSize: IntegerSize,
    drawPathMode: DrawPathMode,
    isEraserOn: Boolean,
): State<PathHelper> = remember(
    drawDownPosition,
    currentDrawPosition,
    onPathChange,
    strokeWidth,
    canvasSize,
    drawPathMode,
    isEraserOn
) {
    derivedStateOf {
        PathHelper(
            drawDownPosition = drawDownPosition,
            currentDrawPosition = currentDrawPosition,
            onPathChange = onPathChange,
            strokeWidth = strokeWidth,
            canvasSize = canvasSize,
            drawPathMode = drawPathMode,
            isEraserOn = isEraserOn
        )
    }
}