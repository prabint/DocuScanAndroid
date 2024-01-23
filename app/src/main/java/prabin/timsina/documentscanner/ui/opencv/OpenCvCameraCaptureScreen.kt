package prabin.timsina.documentscanner.ui.opencv

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.ui.common.pxToDp
import prabin.timsina.documentscanner.ui.destinations.EditPolygonScreenDestination
import prabin.timsina.documentscanner.ui.edit.EditPolygonScreenNavArgs
import prabin.timsina.documentscanner.ui.mlkit.MlkitImageDetectionProperties

@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
@Destination
fun OpenCvCameraCaptureScreen(
    viewModel: OpenCvCameraCaptureViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    val boxSize by viewModel.previewSize.collectAsState()
    val isObjectDetected by viewModel.isObjectDetected.collectAsState()
    val scaledBoundingBox by viewModel.scaledBoundingBox.collectAsState()

    // Photo picker
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                navigator.navigate(EditPolygonScreenDestination(EditPolygonScreenNavArgs(it.toString())))
            }
        },
    )

    // Camera Permission
    var showNavigateToSystemPermissionDialog by remember { mutableStateOf(false) }
    if (showNavigateToSystemPermissionDialog) {
        PermissionAlertDialog { showNavigateToSystemPermissionDialog = false }
    }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        showNavigateToSystemPermissionDialog = !granted
    }
    if (!cameraPermissionState.status.isGranted) {
        LaunchedEffect(
            key1 = Unit,
            block = {
                cameraPermissionState.launchPermissionRequest()
            },
        )
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .onGloballyPositioned { layoutCoordinates ->
                if (layoutCoordinates.size != boxSize) {
                    viewModel.onBoxPositioned(boxSize = layoutCoordinates.size)
                }
            },
    ) {
        CameraView(
            viewModel = viewModel,
            cameraController = cameraController,
        )

        ScanCanvas(
            scaledBoundingBox = scaledBoundingBox,
            isObjectDetected = isObjectDetected,
        )

        CameraControllerBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            cameraController = cameraController,
            onGalleryClicked = { singlePhotoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly)) },
            onCaptureClicked = {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                } else {
                    viewModel.capturePhoto(
                        context = context,
                        cameraController = cameraController,
                        onPhotoCaptured = {
                            navigator.navigate(EditPolygonScreenDestination(EditPolygonScreenNavArgs(it)))
                        },
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraView(viewModel: OpenCvCameraCaptureViewModel, cameraController: LifecycleCameraController) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSelector = viewModel.cameraSelector

    // Live camera preview
    AndroidView(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize(),
        factory = {
            PreviewView(it).apply {
                this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView ->
                cameraController.cameraSelector = cameraSelector
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
 * A canvas view that overlays on top of the document portion of an image
 */
@Composable
private fun ScanCanvas(scaledBoundingBox: MlkitImageDetectionProperties?, isObjectDetected: Boolean) {
    if (scaledBoundingBox == null) return

    // Animate using previousGoodCorners to canvas changes less flickery
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
private fun CameraControllerBottomBar(
    modifier: Modifier = Modifier,
    cameraController: LifecycleCameraController,
    onCaptureClicked: () -> Unit,
    onGalleryClicked: () -> Unit,
) {
    var torch by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Torch button
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape),
            onClick = {
                torch = !torch
                cameraController.enableTorch(torch)
            },
        ) {
            Icon(
                painterResource(
                    id = when (torch) {
                        true -> R.drawable.flashlight_on
                        false -> R.drawable.flashlight_off
                    },
                ),
                tint = Color.White,
                contentDescription = null,
            )
        }

        // Capture button
        IconButton(
            modifier = Modifier
                .size(64.dp)
                .border(width = 2.dp, color = Color.White, shape = CircleShape)
                .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape),
            onClick = { onCaptureClicked() },
        ) {
            Icon(
                painterResource(id = R.drawable.circle),
                tint = Color.White,
                contentDescription = stringResource(R.string.capture_photo),
            )
        }

        // Gallery button
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape),
            onClick = {
                onGalleryClicked()
            },
        ) {
            Icon(
                painterResource(id = R.drawable.photo_library),
                tint = Color.White,
                contentDescription = null,
            )
        }
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
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
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
