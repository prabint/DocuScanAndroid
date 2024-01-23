package prabin.timsina.documentscanner.ui.mlkit

import android.graphics.Point
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import prabin.timsina.documentscanner.utils.get3by4Resolutions
import timber.log.Timber

/**
 * Guides:
 * https://developers.google.com/ml-kit/vision/object-detection/android
 * https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart/app/src/main/assets/automl
 */
@HiltViewModel
class MlKitCameraCaptureViewModel @Inject constructor(
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
     * MlKit returns bounding box based on [ImageProxy] size which is usually very small when compare to the [previewSize].
     * So, we need to scale the coordinates to align the live canvas on the live preview.
     */
    private val _scaledBoundingBox = MutableStateFlow<MlkitImageDetectionProperties?>(null)
    val scaledBoundingBox = _scaledBoundingBox.asStateFlow()

    /**
     * true, if MlKit detects an object
     */
    private val _isObjectDetected = MutableStateFlow(false)
    val isObjectDetected = _isObjectDetected.asStateFlow()

    /**
     * The name of object detected by MlKit
     */
    private val _label = MutableStateFlow<String?>(null)
    val label = _label.asStateFlow()

    /**
     * In some cases you might have to use "org.tensorflow:tensorflow-lite-task-vision" to load the model:
     * https://www.tensorflow.org/lite/examples/object_detection/overview
     *
     * https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/app/download_models.gradle
     * https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/image_classification/android/mobilenet_v1_1.0_224_quantized_1_metadata_1.tflite
     */
    private val localModel = LocalModel.Builder()
        .setAssetFilePath("automl/mobilenet_v1_1.0_224_quantized_1_metadata_1.tflite")
        .build()

    private val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .setClassificationConfidenceThreshold(0.5f)
        .setMaxPerObjectLabelCount(1)
        .build()

    private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

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
        val mlImage = MediaMlImageBuilder(imageProxy.image!!)
            .setRotation(imageProxy.imageInfo.rotationDegrees)
            .build()

        objectDetector.process(mlImage)
            .addOnSuccessListener { detectedObjects ->
                _isObjectDetected.value = detectedObjects.size > 0

                if (detectedObjects.size == 1) {
                    _scaledBoundingBox.value = scaleOriginalPoints(detectedObjects)
                    _label.value = detectedObjects[0].labels.getOrNull(0)?.text
                }
            }
            .addOnFailureListener { Timber.e(it, "objectDetector process failed") }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun onBoxPositioned(boxSize: IntSize) {
        this._previewSize.value = boxSize
        Timber.d("boxSize = $boxSize")
    }

    private fun scaleOriginalPoints(detectedObjects: MutableList<DetectedObject>): MlkitImageDetectionProperties? {
        val previewWidth = previewSize.value.width.toFloat()
        val previewHeight = previewSize.value.height.toFloat()
        val scaleFactor = previewHeight / analysisResolution.height
        val imageAspectRatio = analysisResolution.width.toFloat() / analysisResolution.height

        // This further helps align the canvas on the detected object when using camera's FILL_CENTER.
        // See: updateTransformationIfNeeded() from https://github.com/googlesamples/mlkit/blob/master/android/automl/app/src/main/java/com/google/mlkit/vision/automl/demo/GraphicOverlay.java#L199
        val postScaleWidthOffset = (previewHeight * imageAspectRatio - previewWidth) / 2

        // These corners are based on the smaller [analysisResolution] size. These will appear vert small on the screen.
        // So, need to scale by [ratio]
        val originalCorners = detectedObjects.getOrNull(0)?.boundingBox ?: return null

        return MlkitImageDetectionProperties(
            rect = QuadrilateralCoordinates(
                topLeft = Point(
                    (originalCorners.left * scaleFactor).roundToInt(),
                    (originalCorners.top * scaleFactor).roundToInt(),
                ),
                topRight = Point(
                    (originalCorners.right * scaleFactor).roundToInt(),
                    (originalCorners.top * scaleFactor).roundToInt(),
                ),
                bottomRight = Point(
                    (originalCorners.right * scaleFactor).roundToInt(),
                    (originalCorners.bottom * scaleFactor).roundToInt(),
                ),
                bottomLeft = Point(
                    (originalCorners.left * scaleFactor).roundToInt(),
                    (originalCorners.bottom * scaleFactor).roundToInt(),
                ),
            ),
            postScaleWidthOffset = postScaleWidthOffset,
        )
    }
}
