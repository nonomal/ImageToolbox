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

package ru.tech.imageresizershrinker.feature.root.presentation

import android.content.ClipData
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.olshevski.navigation.reimagined.navigate
import kotlinx.coroutines.delay
import ru.tech.imageresizershrinker.core.crash.components.GlobalExceptionHandler
import ru.tech.imageresizershrinker.core.filters.presentation.utils.LocalFavoriteFiltersInteractor
import ru.tech.imageresizershrinker.core.resources.emoji.Emoji
import ru.tech.imageresizershrinker.core.settings.presentation.model.toUiState
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalEditPresetsController
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSimpleSettingInteractor
import ru.tech.imageresizershrinker.core.settings.presentation.provider.rememberEditPresetsController
import ru.tech.imageresizershrinker.core.ui.shapes.IconShapeDefaults
import ru.tech.imageresizershrinker.core.ui.theme.ImageToolboxTheme
import ru.tech.imageresizershrinker.core.ui.utils.confetti.ConfettiHost
import ru.tech.imageresizershrinker.core.ui.utils.confetti.LocalConfettiHostState
import ru.tech.imageresizershrinker.core.ui.utils.confetti.rememberConfettiHostState
import ru.tech.imageresizershrinker.core.ui.utils.helper.ContextUtils.isInstalledFromPlayStore
import ru.tech.imageresizershrinker.core.ui.utils.helper.ReviewHandler
import ru.tech.imageresizershrinker.core.ui.utils.navigation.currentDestination
import ru.tech.imageresizershrinker.core.ui.utils.provider.LocalImageLoader
import ru.tech.imageresizershrinker.core.ui.widget.UpdateSheet
import ru.tech.imageresizershrinker.core.ui.widget.haptics.rememberCustomHapticFeedback
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.other.SecureModeHandler
import ru.tech.imageresizershrinker.core.ui.widget.other.ToastHost
import ru.tech.imageresizershrinker.core.ui.widget.saver.EnhancedAutoSaverInit
import ru.tech.imageresizershrinker.core.ui.widget.sheets.ProcessImagesPreferenceSheet
import ru.tech.imageresizershrinker.feature.root.presentation.components.AppExitDialog
import ru.tech.imageresizershrinker.feature.root.presentation.components.EditPresetsSheet
import ru.tech.imageresizershrinker.feature.root.presentation.components.FirstLaunchSetupDialog
import ru.tech.imageresizershrinker.feature.root.presentation.components.GithubReviewDialog
import ru.tech.imageresizershrinker.feature.root.presentation.components.PermissionDialog
import ru.tech.imageresizershrinker.feature.root.presentation.components.ScreenSelector
import ru.tech.imageresizershrinker.feature.root.presentation.viewModel.RootViewModel
import ru.tech.imageresizershrinker.feature.settings.presentation.components.additional.DonateDialog

@Composable
fun RootContent(
    viewModel: RootViewModel
) {
    EnhancedAutoSaverInit()

    val context = LocalContext.current as ComponentActivity

    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    var randomEmojiKey by remember {
        mutableIntStateOf(0)
    }

    val currentDestination = viewModel.navController.currentDestination()
    LaunchedEffect(currentDestination) {
        delay(200L) // Delay for transition
        randomEmojiKey++
    }

    val settingsState = viewModel.settingsState.toUiState(
        allEmojis = Emoji.allIcons(),
        allIconShapes = IconShapeDefaults.shapes,
        randomEmojiKey = randomEmojiKey,
        getEmojiColorTuple = viewModel::getColorTupleFromEmoji
    )

    val editPresetsController = rememberEditPresetsController()

    CompositionLocalProvider(
        LocalToastHostState provides viewModel.toastHostState,
        LocalSettingsState provides settingsState,
        LocalSimpleSettingInteractor provides viewModel.getSettingsInteractor(),
        LocalEditPresetsController provides editPresetsController,
        LocalConfettiHostState provides rememberConfettiHostState(),
        LocalImageLoader provides viewModel.imageLoader,
        LocalHapticFeedback provides rememberCustomHapticFeedback(viewModel.settingsState.hapticsStrength),
        LocalFavoriteFiltersInteractor provides viewModel.favoriteFiltersInteractor
    ) {
        SecureModeHandler()

        var showSelectSheet by rememberSaveable(viewModel.showSelectDialog) {
            mutableStateOf(viewModel.showSelectDialog)
        }
        var showUpdateSheet by rememberSaveable(viewModel.showUpdateDialog) {
            mutableStateOf(viewModel.showUpdateDialog)
        }
        LaunchedEffect(settingsState) {
            GlobalExceptionHandler.setAllowCollectCrashlytics(settingsState.allowCollectCrashlytics)
            GlobalExceptionHandler.setAnalyticsCollectionEnabled(settingsState.allowCollectAnalytics)
        }

        LaunchedEffect(showSelectSheet) {
            if (!showSelectSheet) {
                delay(600)
                viewModel.hideSelectDialog()
                viewModel.updateUris(null)
            }
        }
        LaunchedEffect(showUpdateSheet) {
            if (!showUpdateSheet) {
                delay(600)
                viewModel.cancelledUpdate()
            }
        }
        val conf = LocalConfiguration.current
        val systemUiController = rememberSystemUiController()
        LaunchedEffect(conf.orientation) {
            if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                systemUiController.isNavigationBarVisible = false
                systemUiController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                systemUiController.isNavigationBarVisible = true
                systemUiController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
        ImageToolboxTheme {
            val tiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            if (!tiramisu) {
                BackHandler {
                    if (viewModel.shouldShowDialog) showExitDialog = true
                    else context.finishAffinity()
                }
            }

            Surface(Modifier.fillMaxSize()) {
                ScreenSelector(
                    viewModel = viewModel,
                    onRegisterScreenOpen = GlobalExceptionHandler.Companion::registerScreenOpen
                )

                EditPresetsSheet(
                    visible = editPresetsController.isVisible,
                    onDismiss = editPresetsController::close,
                    onUpdatePresets = viewModel::setPresets
                )

                val clipboardManager = LocalClipboardManager.current.nativeClipboard
                ProcessImagesPreferenceSheet(
                    uris = viewModel.uris ?: emptyList(),
                    extraImageType = viewModel.extraImageType,
                    visible = showSelectSheet,
                    onDismiss = { showSelectSheet = it },
                    onNavigate = { screen ->
                        GlobalExceptionHandler.registerScreenOpen(screen)
                        viewModel.navController.navigate(screen)
                        showSelectSheet = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            runCatching {
                                clipboardManager.clearPrimaryClip()
                            }.onFailure {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(null, "")
                                )
                            }
                        } else {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(null, "")
                            )
                        }
                    }
                )
            }

            AppExitDialog(
                onDismiss = { showExitDialog = false },
                visible = showExitDialog && !tiramisu
            )

            UpdateSheet(
                tag = viewModel.tag,
                changelog = viewModel.changelog,
                visible = showUpdateSheet,
                onDismiss = {
                    showUpdateSheet = false
                }
            )

            ConfettiHost()

            ToastHost(
                hostState = LocalToastHostState.current
            )

            SideEffect {
                viewModel.tryGetUpdate(
                    isInstalledFromMarket = context.isInstalledFromPlayStore()
                )
            }

            FirstLaunchSetupDialog(
                toggleShowUpdateDialog = viewModel::toggleShowUpdateDialog,
                toggleAllowBetas = viewModel::toggleAllowBetas,
                adjustPerformance = viewModel::adjustPerformance
            )

            DonateDialog(
                onRegisterDonateDialogOpen = viewModel::registerDonateDialogOpen,
                onNotShowDonateDialogAgain = viewModel::notShowDonateDialogAgain
            )

            PermissionDialog()

            if (viewModel.showGithubReviewSheet) {
                GithubReviewDialog(
                    onDismiss = viewModel::hideReviewSheet,
                    onNotShowAgain = {
                        ReviewHandler.notShowReviewAgain(context)
                    },
                    isNotShowAgainButtonVisible = ReviewHandler.showNotShowAgainButton
                )
            }
        }
    }

}