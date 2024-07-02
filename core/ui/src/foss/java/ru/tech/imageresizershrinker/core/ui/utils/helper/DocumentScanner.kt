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

package ru.tech.imageresizershrinker.core.ui.utils.helper

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.core.ui.widget.other.LocalToastHostState
import ru.tech.imageresizershrinker.core.ui.widget.other.showError
import com.websitebeaver.documentscanner.DocumentScanner as DocumentScannerImpl

class DocumentScanner internal constructor(
    private val scanner: DocumentScannerImpl,
    private val scannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {

    fun scan() {
        scanner.apply {
            scannerLauncher.launch(createDocumentScanIntent())
        }
    }

}


@Composable
fun rememberDocumentScanner(
    onSuccess: (ScanResult) -> Unit
): DocumentScanner {
    val scope = rememberCoroutineScope()
    val toastHostState = LocalToastHostState.current
    val context = LocalContext.current as ComponentActivity

    val scanner = remember(context) {
        DocumentScannerImpl(
            activity = context,
            successHandler = { imageUris ->
                onSuccess(
                    ScanResult(imageUris.map { it.toUri() })
                )
            },
            errorHandler = {
                scope.launch {
                    toastHostState.showError(
                        context = context,
                        error = Throwable(it)
                    )
                }
            }
        )
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scanner.handleDocumentScanIntentResult(result)
    }

    return remember(context, scannerLauncher) {
        DocumentScanner(
            scanner = scanner,
            scannerLauncher = scannerLauncher
        )
    }
}