package ru.tech.imageresizershrinker.presentation.draw_screen.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolfactory.colordetector.util.ColorUtil.roundToTwoDigits
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.presentation.root.theme.outlineVariant
import ru.tech.imageresizershrinker.presentation.root.utils.modifier.block
import ru.tech.imageresizershrinker.presentation.root.widget.utils.LocalSettingsState

@Composable
fun BlurRadiusSelector(
    modifier: Modifier,
    blurRadius: Float,
    onRadiusChange: (Float) -> Unit
) {
    val settingsState = LocalSettingsState.current
    Column(
        modifier = modifier
            .block(shape = RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.BlurCircular,
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        start = 16.dp
                    )
            )
            Text(
                text = stringResource(R.string.blur_radius),
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        end = 16.dp,
                        start = 16.dp
                    )
                    .weight(1f)
            )
            Text(
                text = "$blurRadius",
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.5f
                ),
                modifier = Modifier.padding(top = 16.dp),
                lineHeight = 18.sp
            )
            Text(
                maxLines = 1,
                text = "Px",
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.5f
                ),
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 16.dp,
                    end = 16.dp
                )
            )
        }
        Slider(
            modifier = Modifier
                .padding(
                    top = 16.dp,
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp
                )
                .offset(y = (-2).dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                )
                .height(40.dp)
                .border(
                    width = settingsState.borderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant(onTopOf = MaterialTheme.colorScheme.secondaryContainer),
                    shape = CircleShape
                )
                .padding(horizontal = 10.dp),
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant(onTopOf = MaterialTheme.colorScheme.secondaryContainer)
            ),
            value = blurRadius,
            valueRange = 0f..100f,
            onValueChange = {
                onRadiusChange(it.roundToTwoDigits())
            }
        )
    }
}