package prabin.timsina.documentscanner.ui.opencv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import prabin.timsina.documentscanner.opencv.ImageDetectionProperties
import prabin.timsina.documentscanner.opencv.OpenCvNativeBridge
import prabin.timsina.documentscanner.opencv.OpenCvNativeBridge.convertYUVtoMat
import prabin.timsina.documentscanner.opencv.Quadrilateral
import prabin.timsina.documentscanner.opencv.getScanHint
import prabin.timsina.documentscanner.ui.common.saveToCache
import prabin.timsina.documentscanner.ui.mlkit.MlkitImageDetectionProperties
import prabin.timsina.documentscanner.ui.mlkit.QuadrilateralCoordinates
import prabin.timsina.documentscanner.utils.get3by4Resolutions
import timber.log.Timber

@HiltViewModel
class OpenCvCameraCaptureViewModel @Inject constructor(
    cameraManager: CameraManager,
) : ViewModel() {
    /**
     * Keep track of [Box] size where image is contained within. Box size can change as elements like AppBar, BottomBar
     * etc are add or removed from screen. Box size is used to calculate the amount by which the image needs to be
     * scaled to fit within the box by maintaining aspect ratio.
     */
    private val _previewSize = MutableStateFlow(IntSize(0, 0))
    val previewSize = _previewSize.asStateFlow()

    /**
     * Image analysis returns bounding box based on [ImageProxy] size which is usually very small when compare to the
     * [previewSize]. So, we need to scale the coordinates to align the live canvas on the live preview.
     */
    private val _scaledBoundingBox = MutableStateFlow<MlkitImageDetectionProperties?>(null)
    val scaledBoundingBox = _scaledBoundingBox.asStateFlow()

    /**
     * true if document is detected
     */
    private val _isObjectDetected = MutableStateFlow(false)
    val isObjectDetected = _isObjectDetected.asStateFlow()

    /**
     * Important to use same ratio for image analysis and camera preview so that ImageProxy is not cropped.
     * If imageProxy.cropRect returns (0,0 - w,h), then great. But if it returns (a,b - w,h), then it means image
     * analysis started but from an  offset of a,b. So, portion of image was ignored.
     *
     * Common ratios are 3:4 and 9:16
     */
    private val resolutions = cameraManager.get3by4Resolutions()

    /**
     * Select a low-quality small resolution to do image analysis on
     */
    val analysisResolution = resolutions[0]

    /**
     * Select a mid-quality resolution to so user can see better quality image on camera
     */
    val previewResolution = resolutions[1]

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    @ExperimentalGetImage
    fun process(imageProxy: ImageProxy) {
        val mat = imageProxy.convertYUVtoMat()

        /**
         * For testing: This is the still image where analysis is done (like from gallery). Use this
         * to analyze bitmap which is saved in cache dir.
         *
         * val b = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
         * Utils.matToBitmap(mat, b)
         * val file = File(context.cacheDir, "test.jpg")
         * FileOutputStream(file).use { b.compress(Bitmap.CompressFormat.JPEG, 100, it) }
         */
        OpenCvNativeBridge.detectLargestQuadrilateral(mat)?.let { drawLargestRect(it) }
        mat.release()
        imageProxy.close()
    }

    fun onBoxPositioned(boxSize: IntSize) {
        this._previewSize.value = boxSize
        Timber.d("boxSize = $boxSize")
    }

    private fun drawLargestRect(quadrilateral: Quadrilateral) {
        val points = quadrilateral.points.map {
            Point(it.x.roundToInt(), it.y.roundToInt())
        }

        // Largest height of the detected document portion
        val leftHeight = points[3].y - points[0].y
        val rightHeight = points[2].y - points[1].y
        val resultHeight = max(leftHeight, rightHeight)

        // Largest width of the detected document portion
        val topWidth = points[1].x - points[0].x
        val bottomWidth = points[2].x - points[3].x
        val resultWidth = max(topWidth, bottomWidth)

        // Area occupied by the detected document inside [imageAnalysisArea]
        val resultArea = resultWidth * resultHeight

        val imageAnalysisArea = analysisResolution.width * analysisResolution.height

        val imageDetectionProperties = ImageDetectionProperties(
            quadrilateralCoordinates = QuadrilateralCoordinates(
                topLeft = points[0],
                topRight = points[1],
                bottomRight = points[2],
                bottomLeft = points[3],
            ),
            previewWidth = previewResolution.width.toDouble(),
            previewHeight = previewResolution.height.toDouble(),
            resultWidth = resultWidth,
            resultHeight = resultHeight,
            resultArea = resultArea.toDouble(),
            previewArea = imageAnalysisArea.toDouble(),
            scanHint = ScanHint.AREA_BEYOND_LIMITS,
        )

        _isObjectDetected.value =
            imageDetectionProperties.getScanHint(quadrilateral.contour) == ScanHint.CAPTURING_IMAGE

        _scaledBoundingBox.value = scaleOriginalPoints(points)
    }

    private fun scaleOriginalPoints(points: List<Point>): MlkitImageDetectionProperties {
        val previewWidth = previewSize.value.width.toFloat()
        val previewHeight = previewSize.value.height.toFloat()
        val scaleFactor = previewHeight / analysisResolution.height
        val imageAspectRatio = analysisResolution.width.toFloat() / analysisResolution.height

        // This further helps align the canvas on the detected object when using camera's FILL_CENTER.
        // See: updateTransformationIfNeeded() from https://github.com/googlesamples/mlkit/blob/master/android/automl/app/src/main/java/com/google/mlkit/vision/automl/demo/GraphicOverlay.java#L199
        val postScaleWidthOffset = (previewHeight * imageAspectRatio - previewWidth) / 2

        return MlkitImageDetectionProperties(
            rect = QuadrilateralCoordinates(
                topLeft = Point(
                    (points[0].x * scaleFactor).roundToInt(),
                    (points[0].y * scaleFactor).roundToInt(),
                ),
                topRight = Point(
                    (points[1].x * scaleFactor).roundToInt(),
                    (points[1].y * scaleFactor).roundToInt(),
                ),
                bottomRight = Point(
                    (points[2].x * scaleFactor).roundToInt(),
                    (points[2].y * scaleFactor).roundToInt(),
                ),
                bottomLeft = Point(
                    (points[3].x * scaleFactor).roundToInt(),
                    (points[3].y * scaleFactor).roundToInt(),
                ),
            ),
            postScaleWidthOffset = postScaleWidthOffset,
        )
    }

    fun capturePhoto(context: Context, cameraController: LifecycleCameraController, onPhotoCaptured: (String) -> Unit) {
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        cameraController.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                    val originalBitmap = imageProxy.toBitmap()
                    val orientationFixedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        0,
                        0,
                        originalBitmap.width,
                        originalBitmap.height,
                        // Fix incorrect image rotation
                        Matrix().apply { postRotate(rotation) },
                        true,
                    )

                    onPhotoCaptured(orientationFixedBitmap.saveToCache(context))
                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Error capturing image")
                }
            },
        )
    }
}
