package prabin.timsina.documentscanner.ui.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import prabin.timsina.documentscanner.opencv.OpenCvNativeBridge
import prabin.timsina.documentscanner.ui.common.deleteCachePhoto
import prabin.timsina.documentscanner.ui.common.saveToCache
import prabin.timsina.documentscanner.ui.navArgs
import timber.log.Timber

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class EditPolygonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<EditPolygonScreenNavArgs>()

    private val originalBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    /**
     * Represents the top-left corner of the bitmap (not the document) inside the box. Image shifts because we Center it
     * inside the box.
     */
    private val _imageOffsetInsideBox = MutableStateFlow(Offset(0f, 0f))
    val imageOffsetInsideBox = _imageOffsetInsideBox.asStateFlow()

    /**
     * [adjustedScaledCornerPoints] represents 4 corner points for [scaledBitmap] after it has been offset to
     * position at the center of its container view. The offset amount is [imageOffsetInsideBox]. If we didn't Center
     * align the image and let would position at top-left corner, we wouldn't need [imageOffsetInsideBox].
     *
     * In addition to this offset, we also need to shift the corner circles by -[RADIUS] toward left and -[RADIUS]
     * up. This is because we are using [Box] to draw circles and [Box] has (0,0) top-left corner as origin,
     * (unlike Canvas.drawCircles() where the center (0,0) is at the center of circle). By moving the [Box] slightly
     * towards left and slightly up, the circle perfectly aligns with the document's corner.
     *
     * [_adjustedScaledCornerPoints] updates as user moves the corners. Then, when we do perspective transformation,
     * we need to undo the offsets we did above, i.e undo [RADIUS] and Center position offset.
     */
    private val _adjustedScaledCornerPoints = MutableStateFlow(emptyList<Point>())
    val adjustedScaledCornerPoints = _adjustedScaledCornerPoints.asStateFlow()

    /**
     * The scaled version of original bitmap to fit in container [Box] and maintaining aspect ratio. We don't use the
     * [originalBitmap] because if let's say [originalBitmap] is of 1000x1000 size and the phone screen is 500x500
     * size, then [originalBitmap] would go beyond the screen on the phone. That would also mean then [PolygonView]
     * would also go beyond the screen.
     */
    private val _scaledBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val scaledBitmap = _scaledBitmap.asStateFlow()

    /**
     * When the bitmap container [Box] is laid out, it goes through multiple [onGloballyPositioned] calls as elements
     * like bottom bar, top bar etc are added and removed form the screen. This causes the [Box] to resize and in turn
     * we should recalculate the amount by which we should resize original image to fit inside the [Box]. We use
     * [previousBoxSize] to determine if the box size changed and if so do the recalculation.
     */
    private val _previousBoxSize = MutableStateFlow(IntSize(0, 0))
    val previousBoxSize = _previousBoxSize.asStateFlow()

    init {
        val uri = Uri.parse(navArgs.uri)

        originalBitmap.value = when (uri.scheme) {
            "content" ->
                appContext
                    .contentResolver
                    .openInputStream(uri)
                    .use { BitmapFactory.decodeStream(it) }

            else -> BitmapFactory.decodeFile(uri.toString())
        }
    }

    /**
     * Gets called whenever the image container [Box] size changes. The size changes as elements like bottom bar, top
     * bar etc are added on the screen that causes box to resize.
     */
    fun onPositioned(boxSize: IntSize) {
        _previousBoxSize.value = boxSize

        // The ratio by which the original image needs to be scaled up or down to fit inside the box
        val ratio = min(
            a = boxSize.width.toFloat() / originalBitmap.value!!.width,
            b = boxSize.height.toFloat() / originalBitmap.value!!.height,
        )

        _scaledBitmap.value = Bitmap.createScaledBitmap(
            originalBitmap.value!!,
            (originalBitmap.value!!.width * ratio).roundToInt(),
            (originalBitmap.value!!.height * ratio).roundToInt(),
            true,
        )

        // Calculate corners for the scaled bitmap that has no offset, i.e. origin of image is at top-left (0,0)
        val scaledMat = Mat()
        Utils.bitmapToMat(scaledBitmap.value, scaledMat)

        val scaledCornerPoints = OpenCvNativeBridge.detectLargestQuadrilateral(scaledMat)?.points ?: listOf(
            // Default fallback rectangle if no corners were found
            Point(FALLBACK_OFFSET, FALLBACK_OFFSET),
            Point(scaledMat.width().toDouble() - FALLBACK_OFFSET, FALLBACK_OFFSET),
            Point(scaledMat.width().toDouble() - FALLBACK_OFFSET, scaledMat.height().toDouble() - FALLBACK_OFFSET),
            Point(FALLBACK_OFFSET, scaledMat.height().toDouble() - FALLBACK_OFFSET),
        )

        // Since we Center align the image inside the box, the image shifts. We calculate by how much the image shifted
        // inside the box and calculate the new top-left corner of the image.
        _imageOffsetInsideBox.value = Offset(
            (boxSize.width - scaledBitmap.value!!.width) / 2f,
            (boxSize.height - scaledBitmap.value!!.height) / 2f,
        )

        /**
         * Adjust scaledCornerPoints further to align them perfectly with document's corner:
         *
         * 1. -RADIUS because a [Box] in compose has (0,0) at top-left instead of center. -RADIUS moves the box
         * slightly towards top-left so that it's center is at the document's corner
         *
         * 2. +imageOffsetInsideBox because image is Center aligned inside the Box. So we need to move the
         * circle towards right-down.
         */
        _adjustedScaledCornerPoints.value = scaledCornerPoints.map { scaledPoint ->
            Point(
                scaledPoint.x - RADIUS + imageOffsetInsideBox.value.x,
                scaledPoint.y - RADIUS + imageOffsetInsideBox.value.y,
            )
        }

        Timber.d(
            "------------\n" +
                "Original Image Size: ${originalBitmap.value?.width}x${originalBitmap.value?.height}\n" +
                "Box size: ${boxSize}\n" +
                "ratio = $ratio\n" +
                "Scaled Image size: ${scaledBitmap.value?.width}x${scaledBitmap.value?.height}\n" +
                "scaledMat size: ${scaledMat.size()}\n" +
                "imageOffsetInsideBox = ${imageOffsetInsideBox.value}\n" +
                "scaledPoint = $scaledCornerPoints\n" +
                "adjustedScaledCornerPoints = ${adjustedScaledCornerPoints.value}\n" +
                "------------\n",
        )
    }

    fun onMove(points: List<Point>) {
        _adjustedScaledCornerPoints.value = points
    }

    fun onClickPreview(
        context: Context,
        scaledBitmap: Bitmap,
        imageOffsetInsideBox: Offset,
        adjustedScaledCornerPoints: List<Point>,
    ): String {
        val mat = Mat()
        Utils.bitmapToMat(scaledBitmap, mat)

        // We are doing transformation on scaled image. We could also do it on originalBitmap if needed by scaling
        // the points by some ratio.
        val transformedBitmap = OpenCvNativeBridge.perspectiveTransformation(
            mat = mat,

            // Undo offsets that were added for visual purpose
            corners = adjustedScaledCornerPoints.map { point ->
                Point(
                    point.x + RADIUS - imageOffsetInsideBox.x,
                    point.y + RADIUS - imageOffsetInsideBox.y,
                )
            },
        )

        val resultBitmap = Bitmap.createBitmap(
            transformedBitmap.cols(),
            transformedBitmap.rows(),
            Bitmap.Config.ARGB_8888,
        )

        Utils.matToBitmap(transformedBitmap, resultBitmap)
        return resultBitmap.saveToCache(context)
    }

    override fun onCleared() {
        deleteCachePhoto(appContext, navArgs.uri.substringAfterLast("/"))
        super.onCleared()
    }

    companion object {
        private const val FALLBACK_OFFSET = 100.0
        internal const val DIAMETER = 100
        internal const val RADIUS = DIAMETER / 2
    }
}
