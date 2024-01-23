package prabin.timsina.documentscanner.ui.mlkit

import android.net.Uri
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.annotation.Destination
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.ui.common.pxToDp

/**
 * Demo for mlkit object detection with an overlay on top of detected object. MlKit only returns rectangular bounding
 * box unlike OpenCV which returns quadrilateral. So, while openCv's bounding box overlays exactly on top of the
 * document, mlkit's overlay run will be a rectangular box on top of document, regardless of tilt.
 */
@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
@Destination
fun MlKitCameraCaptureScreen(viewModel: MlKitCameraCaptureViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    val label by viewModel.label.collectAsState()
    val boxSize by viewModel.previewSize.collectAsState()
    val isObjectDetected by viewModel.isObjectDetected.collectAsState()
    val scaledBoundingBox by viewModel.scaledBoundingBox.collectAsState()

    // Camera Permission
    var showNavigateToSystemPermissionDialog by remember { mutableStateOf(false) }
    if (showNavigateToSystemPermissionDialog) {
        PermissionAlertDialog { showNavigateToSystemPermissionDialog = false }
    }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA) { granted ->
        showNavigateToSystemPermissionDialog = !granted
    }
    if (!cameraPermissionState.status.isGranted) {
        LaunchedEffect(
            key1 = Unit,
            block = { cameraPermissionState.launchPermissionRequest() },
        )
    }

    Box(
        modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
            if (layoutCoordinates.size != boxSize) {
                viewModel.onBoxPositioned(boxSize = layoutCoordinates.size)
            }
        },
    ) {
        CameraView(
            cameraController = cameraController,
            viewModel = viewModel,
        )

        ScanCanvas(
            scaledBoundingBox = scaledBoundingBox,
            isObjectDetected = isObjectDetected,
        )

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = label.toString(),
            style = LocalTextStyle.current.merge(
                TextStyle(
                    color = Color.Magenta,
                    fontSize = 40.sp,
                    drawStyle = Stroke(width = 4f),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraView(viewModel: MlKitCameraCaptureViewModel, cameraController: LifecycleCameraController) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PreviewView(it).apply {
                this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView ->
                cameraController.cameraSelector = viewModel.cameraSelector
                previewView.controller = cameraController

                previewView.doOnLayout {
                    cameraController.previewTargetSize = CameraController.OutputSize(viewModel.previewResolution)
                    cameraController.imageAnalysisTargetSize = CameraController.OutputSize(viewModel.analysisResolution)
                    cameraController.setImageAnalysisAnalyzer(executor) { imageProxy -> viewModel.process(imageProxy) }
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            }
        },
    )
}

/**
 * A canvas view that overlays on top of detected object
 */
@Composable
private fun ScanCanvas(scaledBoundingBox: MlkitImageDetectionProperties?, isObjectDetected: Boolean) {
    if (scaledBoundingBox == null) return

    // Fade animation to reduce flicker
    AnimatedVisibility(
        visible = isObjectDetected,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(-scaledBoundingBox.postScaleWidthOffset.roundToInt().pxToDp()),
            onDraw = {
                val path = Path().apply {
                    moveTo(
                        scaledBoundingBox.rect.topLeft.x.toFloat(),
                        scaledBoundingBox.rect.topLeft.y.toFloat(),
                    )
                    lineTo(
                        scaledBoundingBox.rect.topRight.x.toFloat(),
                        scaledBoundingBox.rect.topRight.y.toFloat(),
                    )
                    lineTo(
                        scaledBoundingBox.rect.bottomRight.x.toFloat(),
                        scaledBoundingBox.rect.bottomRight.y.toFloat(),
                    )
                    lineTo(
                        scaledBoundingBox.rect.bottomLeft.x.toFloat(),
                        scaledBoundingBox.rect.bottomLeft.y.toFloat(),
                    )
                    lineTo(
                        scaledBoundingBox.rect.topLeft.x.toFloat(),
                        scaledBoundingBox.rect.topLeft.y.toFloat(),
                    )
                    close()
                }

                drawPath(path = path, color = Color.Green, alpha = 0.3f)
            },
        )
    }
}

@Composable
private fun PermissionAlertDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = stringResource(id = R.string.close))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    ContextCompat.startActivity(
                        context,
                        android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null)),
                        null,
                    )
                },
            ) {
                Text(stringResource(R.string.settings))
            }
        },
        title = { Text(text = stringResource(R.string.camera_permission_required)) },
        text = { Text(text = stringResource(R.string.camera_permission_required_desc)) },
    )
}
