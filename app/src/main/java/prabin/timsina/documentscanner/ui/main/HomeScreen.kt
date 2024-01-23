package prabin.timsina.documentscanner.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.ui.destinations.MlKitCameraCaptureScreenDestination
import prabin.timsina.documentscanner.ui.destinations.OpenCvCameraCaptureScreenDestination
import prabin.timsina.documentscanner.ui.destinations.OpenCvMinimalCameraCaptureScreenDestination

@Composable
@Destination(start = true)
fun HomeScreen(navigator: DestinationsNavigator) {
    Column {
        TextButton(
            onClick = {
                navigator.navigate(OpenCvMinimalCameraCaptureScreenDestination)
            },
        ) {
            Text(stringResource(R.string.opencv_document_scan_minimal))
        }

        Divider()

        TextButton(
            onClick = {
                navigator.navigate(OpenCvCameraCaptureScreenDestination)
            },
        ) {
            Text(stringResource(R.string.opencv_document_scan_full))
        }

        Divider()

        TextButton(
            onClick = {
                navigator.navigate(MlKitCameraCaptureScreenDestination)
            },
        ) {
            Text(stringResource(R.string.mlkit_object_detection))
        }
    }
}
