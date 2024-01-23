package prabin.timsina.documentscanner.opencv

import kotlin.math.abs
import kotlin.math.sqrt
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import prabin.timsina.documentscanner.ui.mlkit.QuadrilateralCoordinates
import prabin.timsina.documentscanner.ui.opencv.ScanHint
import timber.log.Timber

data class ImageDetectionProperties(
    val quadrilateralCoordinates: QuadrilateralCoordinates,
    val previewWidth: Double,
    val previewHeight: Double,
    val resultWidth: Int,
    val resultHeight: Int,
    val resultArea: Double,
    val previewArea: Double,
    var scanHint: ScanHint,
) {
    /**
     * Check if the document's [resultArea] occupies certain percentage of [previewArea] area
     */
    fun isDetectedAreaBeyondLimits() = (resultArea / previewArea) !in 0.12..0.65

    fun isDetectedAreaBelowLimits() = resultArea <= previewArea * 0.10

    fun isDetectedAreaAboveLimit() = resultArea > previewArea * 0.75

    fun isDetectedWidthAboveLimit() = resultWidth > previewWidth * 0.9

    fun isDetectedHeightAboveLimit() = resultHeight > previewHeight * 0.9

    fun isAngleNotCorrect(approx: MatOfPoint2f) =
        getMaxCosine(approx) || isLeftEdgeDistorted() || isRightEdgeDistorted()

    fun isEdgeTouching() =
        isTopEdgeTouching() || isBottomEdgeTouching() || isLeftEdgeTouching() || isRightEdgeTouching()

    private fun isBottomEdgeTouching() = quadrilateralCoordinates.bottomLeft.x >= previewHeight - 50 ||
        quadrilateralCoordinates.bottomRight.x >= previewHeight - 50

    private fun isTopEdgeTouching() = quadrilateralCoordinates.topLeft.x <= 10 ||
        quadrilateralCoordinates.topRight.x <= 10

    private fun isRightEdgeTouching() = quadrilateralCoordinates.topRight.y >= previewHeight - 50 ||
        quadrilateralCoordinates.bottomRight.y >= previewHeight - 50

    private fun isLeftEdgeTouching() = quadrilateralCoordinates.topLeft.y <= 30 ||
        quadrilateralCoordinates.bottomLeft.y <= 30

    private fun isRightEdgeDistorted() =
        abs(quadrilateralCoordinates.topRight.x - quadrilateralCoordinates.bottomRight.x) > 100

    private fun isLeftEdgeDistorted() =
        abs(quadrilateralCoordinates.topLeft.x - quadrilateralCoordinates.bottomLeft.x) > 100

    private fun angle(p1: Point, p2: Point, p0: Point): Double {
        val dx1 = p1.x - p0.x
        val dy1 = p1.y - p0.y
        val dx2 = p2.x - p0.x
        val dy2 = p2.y - p0.y
        return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
    }

    private fun getMaxCosine(approx: MatOfPoint2f): Boolean {
        var maxCosine = 0.0
        val approxPoints = approx.toArray()
        maxCosine = getMaxCosine(maxCosine, approxPoints)
        // cosine(70) = 0.342. This is the tilt amount limit of camera/image.
        return maxCosine >= 0.342
    }

    private fun getMaxCosine(maxCosine: Double, approxPoints: Array<Point>): Double {
        var max = maxCosine
        for (i in 2..4) {
            val cosine = abs(angle(approxPoints[i % 4], approxPoints[i - 2], approxPoints[i - 1]))
            max = cosine.coerceAtLeast(max)
        }
        return max
    }
}

fun ImageDetectionProperties.getScanHint(contour: MatOfPoint2f) = when {
    isDetectedAreaBeyondLimits() -> ScanHint.AREA_BEYOND_LIMITS
    isDetectedAreaBelowLimits() -> when {
        isEdgeTouching() -> ScanHint.AREAL_BELOW_LIMITS_AND_EDGE_TOUCHING
        else -> ScanHint.AREA_BELOW_LIMITS
    }

    isDetectedHeightAboveLimit() -> ScanHint.HEIGHT_ABOVE_LIMIT
    isDetectedWidthAboveLimit() -> ScanHint.WIDTH_ABOVE_LIMIT
    else -> when {
        isDetectedAreaAboveLimit() -> ScanHint.AREA_ABOVE_LIMITS
        else -> when {
            isEdgeTouching() -> ScanHint.EDGE_TOUCHING
            isAngleNotCorrect(contour) -> ScanHint.ADJUST_ANGLE
            else -> ScanHint.CAPTURING_IMAGE
        }
    }
}.also {
    if (it != ScanHint.CAPTURING_IMAGE) {
        Timber.w("ScanHint = $it")
    }
}
