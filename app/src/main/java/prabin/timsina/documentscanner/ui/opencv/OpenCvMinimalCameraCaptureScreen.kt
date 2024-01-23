package prabin.timsina.documentscanner.ui.opencv

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.annotation.Destination
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.opencv.ImageDetectionProperties
import prabin.timsina.documentscanner.opencv.OpenCvNativeBridge
import prabin.timsina.documentscanner.opencv.OpenCvNativeBridge.convertYUVtoMat
import prabin.timsina.documentscanner.opencv.Quadrilateral
import prabin.timsina.documentscanner.opencv.getScanHint
import prabin.timsina.documentscanner.ui.common.pxToDp
import prabin.timsina.documentscanner.ui.mlkit.QuadrilateralCoordinates
import timber.log.Timber

// Choose a 3:4 ratio for image analysis
private val analysisSize = android.util.Size(240, 320)

// Choose 3:4 ratio of higher resolution for camera preview resolution
private val previewSize = android.util.Size(2 * analysisSize.width, 2 * analysisSize.height)

/**
 * This is a very basic example of live camera preview and image analysis. It uses same aspect ratio for [analysisSize]
 * and [previewSize]. The [previewSize] is of higher resolution to demonstrate scaling of image analysis points to fit
 * camera preview. Since image analysis is done on [analysisSize], the corner points are based off that bound. But,
 * [previewSize] is twice as large. So, the points are multiplies (by 2 in this case) so they fit the preview.
 */
@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
@Destination
fun OpenCvMinimalCameraCaptureScreen() {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    var imageDetectionProperties by remember { mutableStateOf<ImageDetectionProperties?>(null) }

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

    Box {
        CameraView(cameraController) { imageDetectionProperties = it }
        ScanCanvas(imageDetectionProperties)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraView(cameraController: LifecycleCameraController, onDetect: (ImageDetectionProperties?) -> Unit) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    // Live camera preview
    AndroidView(
        modifier = Modifier.size(
            previewSize.width.pxToDp(),
            previewSize.height.pxToDp(),
        ),
        factory = {
            PreviewView(it)
                .apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                .also { previewView ->
                    cameraController.cameraSelector = cameraSelector
                    previewView.controller = cameraController

                    previewView.doOnLayout { _ ->
                        cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        cameraController.imageAnalysisTargetSize = CameraController.OutputSize(analysisSize)
                        cameraController.previewTargetSize = CameraController.OutputSize(previewSize)

                        Timber.i(
                            "previewTargetSize = ${cameraController.previewTargetSize}\n" +
                                "imageAnalysisTargetSize = ${cameraController.imageAnalysisTargetSize}",
                        )

                        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy: ImageProxy ->
                            /*
                             * Unused, just for note:
                             * ImageProxy cropRect: The ImageProxy object indeed has a width of 1280 pixels and a height of 960 pixels.
                             * This represents the overall dimensions of the image data it holds.
                             *
                             * Crop Rectangle: However, the crop rectangle specifies a smaller region within that image, measuring 1000 pixels in
                             * width and 960 pixels in height. This means that only those specific pixels within the crop rectangle are considered
                             * valid for processing or analysis.
                             *
                             * ---
                             * val applicableSize = imageProxy.cropRect
                             * val imageSize = android.util.Size(imageProxy.image!!.width, imageProxy.image!!.height)
                             * Timber.v("applicableSize = $applicableSize\n imageSize = $imageSize")
                             */
                            val mat = imageProxy.convertYUVtoMat()

                            OpenCvNativeBridge.detectLargestQuadrilateral(mat)?.let { quadrilateral ->
                                drawLargestRect(quadrilateral) { props -> onDetect(props) }
                            }

                            mat.release()
                            imageProxy.close()
                        }

                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                }
        },
    )
}

@Composable
private fun ScanCanvas(imageDetectionProperties: ImageDetectionProperties?) {
    if (imageDetectionProperties == null) return

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            drawPoints(
                points = listOf(
                    Offset(
                        imageDetectionProperties.quadrilateralCoordinates.topLeft.x.toFloat(),
                        imageDetectionProperties.quadrilateralCoordinates.topLeft.y.toFloat(),
                    ),
                ),
                pointMode = PointMode.Points,
                color = Color.Red.copy(alpha = 0.5f),
                cap = StrokeCap.Round,
                strokeWidth = 50f,
            )

            drawPoints(
                points = listOf(
                    Offset(
                        imageDetectionProperties.quadrilateralCoordinates.topRight.x.toFloat(),
                        imageDetectionProperties.quadrilateralCoordinates.topRight.y.toFloat(),
                    ),
                ),
                pointMode = PointMode.Points,
                color = Color.Green.copy(alpha = 0.5f),
                cap = StrokeCap.Round,
                strokeWidth = 50f,
            )

            drawPoints(
                points = listOf(
                    Offset(
                        imageDetectionProperties.quadrilateralCoordinates.bottomRight.x.toFloat(),
                        imageDetectionProperties.quadrilateralCoordinates.bottomRight.y.toFloat(),
                    ),
                ),
                pointMode = PointMode.Points,
                color = Color.Blue.copy(alpha = 0.5f),
                cap = StrokeCap.Round,
                strokeWidth = 50f,
            )

            drawPoints(
                points = listOf(
                    Offset(
                        imageDetectionProperties.quadrilateralCoordinates.bottomLeft.x.toFloat(),
                        imageDetectionProperties.quadrilateralCoordinates.bottomLeft.y.toFloat(),
                    ),
                ),
                pointMode = PointMode.Points,
                color = Color.Yellow.copy(alpha = 0.5f),
                cap = StrokeCap.Round,
                strokeWidth = 50f,
            )
        },
    )
}

private fun drawLargestRect(
    quadrilateral: Quadrilateral,
    onImageDetectionProperties: (ImageDetectionProperties?) -> Unit,
) {
    val points = quadrilateral.points

    // Largest height
    val leftHeight = points[3].y - points[0].y
    val rightHeight = points[2].y - points[1].y
    val resultHeight = max(leftHeight, rightHeight)

    // Largest width
    val topWidth = points[1].x - points[0].x
    val bottomWidth = points[2].x - points[3].x
    val resultWidth = max(topWidth, bottomWidth)

    val ratio = kotlin.math.min(
        previewSize.width.toFloat() / analysisSize.width,
        previewSize.height.toFloat() / analysisSize.height,
    )

    val resultArea = resultWidth * resultHeight
    val previewArea = analysisSize.width * analysisSize.height

    // New scaled points that can be rendered to match [previewSize]
    // Tip: Set [ratio] is set to 1, then we can the original unscaled corners on the screen
    val pointsScaled = quadrilateral.points.map { pointF ->
        android.graphics.Point(
            (ratio * pointF.x).roundToInt(),
            (ratio * pointF.y).roundToInt(),
        )
    }

    val imgDetectionPropsObj = ImageDetectionProperties(
        QuadrilateralCoordinates(
            pointsScaled[0],
            pointsScaled[1],
            pointsScaled[2],
            pointsScaled[3],
        ),
        previewWidth = previewSize.width.toDouble(),
        previewHeight = previewSize.height.toDouble(),
        resultWidth = resultWidth.roundToInt(),
        resultHeight = resultHeight.roundToInt(),
        resultArea = resultArea,
        previewArea = previewArea.toDouble(),
        scanHint = ScanHint.AREA_BEYOND_LIMITS,
    )

    val scan = imgDetectionPropsObj.getScanHint(quadrilateral.contour)

    if (scan == ScanHint.CAPTURING_IMAGE) {
        onImageDetectionProperties(imgDetectionPropsObj)
    } else {
        onImageDetectionProperties(null)
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
