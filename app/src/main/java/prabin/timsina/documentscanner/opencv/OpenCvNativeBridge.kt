package prabin.timsina.documentscanner.opencv

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.util.Collections
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * https://github.com/adityaarora1/LiveEdgeDetection
 * https://github.com/zynkware/Document-Scanning-Android-SDK/blob/master/DocumentScanner/src/main/java/com/zynksoftware/documentscanner/common/utils/OpenCvNativeBridge.kt
 */
object OpenCvNativeBridge {
    private const val ANGLES_NUMBER = 4
    private const val EPSILON_CONSTANT = 0.02
    private const val CLOSE_KERNEL_SIZE = 10.0
    private const val BLURRING_KERNEL_SIZE = 5.0
    private const val CANNY_THRESHOLD_LOW = 75.0
    private const val CANNY_THRESHOLD_HIGH = 200.0
    private const val CUTOFF_THRESHOLD = 155.0
    private const val TRUNCATE_THRESHOLD = 150.0
    private const val NORMALIZATION_MIN_VALUE = 0.0
    private const val NORMALIZATION_MAX_VALUE = 255.0
    private const val FIRST_MAX_CONTOURS = 10

    private fun sortPoints(src: List<Point>): List<Point> {
        val srcPoints: ArrayList<Point> = ArrayList(src.toList())
        val result = arrayOf<Point?>(null, null, null, null)

        val sumComparator = Comparator<Point> { lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x) }
        val diffComparator = Comparator<Point> { lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x) }

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)

        return result.map { it!! }
    }

    fun detectLargestQuadrilateral(src: Mat): Quadrilateral? {
        return findLargestContours(morph(src))?.let { findQuadrilateral(it) }
    }

    private fun findQuadrilateral(mContourList: List<MatOfPoint>): Quadrilateral? {
        for (c in mContourList) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()

            Imgproc.approxPolyDP(c2f, approx, EPSILON_CONSTANT * peri, true)
            val points = approx.toList()

            // Select biggest 4 angles polygon
            if (approx.rows() == ANGLES_NUMBER) {
                return Quadrilateral(approx, sortPoints(points))
            } else if (approx.rows() == 5) {
                // If document has a bent corner
                // https://github.com/zynkware/Document-Scanning-Android-SDK/blob/master/DocumentScanner/src/main/java/com/zynksoftware/documentscanner/common/utils/OpenCvNativeBridge.kt#L127
                var shortestDistance = Int.MAX_VALUE.toDouble()
                var shortestPoint1: Point? = null
                var shortestPoint2: Point? = null

                var diagonal = 0.toDouble()
                var diagonalPoint1: Point? = null
                var diagonalPoint2: Point? = null

                for (i in 0 until 4) {
                    for (j in i + 1 until 5) {
                        val d = distance(points[i], points[j])
                        if (d < shortestDistance) {
                            shortestDistance = d
                            shortestPoint1 = points[i]
                            shortestPoint2 = points[j]
                        }
                        if (d > diagonal) {
                            diagonal = d
                            diagonalPoint1 = points[i]
                            diagonalPoint2 = points[j]
                        }
                    }
                }

                val trianglePointWithHypotenuse: Point? = points.minus(
                    arrayListOf(
                        shortestPoint1,
                        shortestPoint2,
                        diagonalPoint1,
                        diagonalPoint2,
                    ).toSet(),
                )[0]

                val newPoint = if (trianglePointWithHypotenuse!!.x > shortestPoint1!!.x &&
                    trianglePointWithHypotenuse.x > shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y > shortestPoint1.y &&
                    trianglePointWithHypotenuse.y > shortestPoint2.y
                ) {
                    Point(
                        min(shortestPoint1.x, shortestPoint2.x),
                        min(shortestPoint1.y, shortestPoint2.y),
                    )
                } else if (trianglePointWithHypotenuse.x < shortestPoint1.x &&
                    trianglePointWithHypotenuse.x < shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y > shortestPoint1.y &&
                    trianglePointWithHypotenuse.y > shortestPoint2.y
                ) {
                    Point(
                        max(shortestPoint1.x, shortestPoint2.x),
                        min(shortestPoint1.y, shortestPoint2.y),
                    )
                } else if (trianglePointWithHypotenuse.x < shortestPoint1.x &&
                    trianglePointWithHypotenuse.x < shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y < shortestPoint1.y &&
                    trianglePointWithHypotenuse.y < shortestPoint2.y
                ) {
                    Point(
                        max(shortestPoint1.x, shortestPoint2.x),
                        max(shortestPoint1.y, shortestPoint2.y),
                    )
                } else if (trianglePointWithHypotenuse.x > shortestPoint1.x &&
                    trianglePointWithHypotenuse.x > shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y < shortestPoint1.y &&
                    trianglePointWithHypotenuse.y < shortestPoint2.y
                ) {
                    Point(
                        min(shortestPoint1.x, shortestPoint2.x),
                        max(shortestPoint1.y, shortestPoint2.y),
                    )
                } else {
                    Point(0.0, 0.0)
                }

                val sortedPoints = sortPoints(
                    listOf(
                        trianglePointWithHypotenuse,
                        diagonalPoint1!!,
                        diagonalPoint2!!,
                        newPoint,
                    ),
                )

                val newApprox = MatOfPoint2f()
                newApprox.fromList(sortedPoints)
                return Quadrilateral(newApprox, sortedPoints)
            }
        }

        return null
    }

    /**
     * Patch from Udayraj123 (https://github.com/Udayraj123/LiveEdgeDetection)
     * https://github.com/adityaarora1/LiveEdgeDetection/blob/master/liveedgedetection/src/main/java/com/adityaarora/liveedgedetection/util/ScanUtils.java#L336
     *
     *  1. We shall first blur and normalize the image for uniformity,
     *  2. Truncate light-gray to white and normalize,
     *  3. Apply canny edge detection,
     *  4. Cutoff weak edges,
     *  5. Apply closing(morphology), then proceed to finding contours.
     */
    private fun morph(src: Mat): Mat {
        val destination = Mat()

        // TODO: Makes live edge detection more stable. But has issue detecting round edge of an ID card.
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY, 4)

        // 1. Blur
        Imgproc.blur(src, src, Size(BLURRING_KERNEL_SIZE, BLURRING_KERNEL_SIZE))
        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        // 2. As most papers are bright in color, we can use truncation to make it uniformly bright.
        Imgproc.threshold(src, src, TRUNCATE_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TRUNC)
        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        // 3. After above preprocessing, canny edge detection can now work much better.
        Imgproc.Canny(src, destination, CANNY_THRESHOLD_HIGH, CANNY_THRESHOLD_LOW)

        // 4. Cutoff the remaining weak edges
        Imgproc.threshold(destination, destination, CUTOFF_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TOZERO)

        // 5. Closing - closes small gaps. Completes the edges on canny image; AND also reduces stringy lines near edge
        // of paper.
        Imgproc.morphologyEx(
            destination,
            destination,
            Imgproc.MORPH_CLOSE,
            Mat(Size(CLOSE_KERNEL_SIZE, CLOSE_KERNEL_SIZE), CvType.CV_8UC1, Scalar(NORMALIZATION_MAX_VALUE)),
            Point(-1.0, -1.0),
            1,
        )

        return destination
    }

    /**
     * Get only the 10 largest contours (each approximated to their convex hulls)
     */
    private fun findLargestContours(inputMat: Mat): List<MatOfPoint>? {
        val mHierarchy = Mat()
        val mContourList: List<MatOfPoint> = ArrayList()

        // finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        Imgproc.findContours(inputMat, mContourList, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour
        val mHullList: MutableList<MatOfPoint> = ArrayList()
        val tempHullIndices = MatOfInt()
        for (i in mContourList.indices) {
            Imgproc.convexHull(mContourList[i], tempHullIndices)
            mHullList.add(hull2Points(tempHullIndices, mContourList[i]))
        }
        // Release mContourList as its job is done
        for (c in mContourList) {
            c.release()
        }
        tempHullIndices.release()
        mHierarchy.release()
        if (mHullList.size != 0) {
            mHullList.sortWith { lhs, rhs ->
                Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
            }
            return mHullList.subList(0, min(mHullList.size, FIRST_MAX_CONTOURS))
        }
        return null
    }

    private fun distance(p1: Point, p2: Point) = sqrt((p1.x - p2.x).pow(2.0) + (p1.y - p2.y).pow(2.0))

    private fun hull2Points(hull: MatOfInt, contour: MatOfPoint): MatOfPoint {
        val indexes = hull.toList()
        val points: MutableList<Point> = ArrayList()
        val ctrList = contour.toList()
        for (index in indexes) {
            points.add(ctrList[index])
        }
        val point = MatOfPoint()
        point.fromList(points)
        return point
    }

    private fun getRectangleSize(rectangle: List<Point>): Size {
        val top = getDistance(rectangle[0], rectangle[1])
        val right = getDistance(rectangle[1], rectangle[2])
        val bottom = getDistance(rectangle[2], rectangle[3])
        val left = getDistance(rectangle[3], rectangle[0])
        val averageWidth = (top + bottom) / 2f
        val averageHeight = (right + left) / 2f
        return Size(Point(averageWidth, averageHeight))
    }

    private fun getDistance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    fun perspectiveTransformation(mat: Mat, corners: List<Point>): Mat {
        // Detect the document's corners
        if (corners.size != 4) return mat

        // Order the corners for perspective transformation
        val orderedCorners = sortPoints(corners)

        val size = getRectangleSize(orderedCorners)
        val result = Mat.zeros(size, mat.type())
        val imageOutline = getOutline(result)

        // Calculate the homography matrix
        val homography = Imgproc.getPerspectiveTransform(
            MatOfPoint2f(*orderedCorners.toTypedArray()),
            imageOutline,
        )

        val transformed = Mat()

        // Apply the perspective transformation
        Imgproc.warpPerspective(mat, transformed, homography, size)

        return transformed
    }

    private fun getOutline(image: Mat): MatOfPoint2f {
        val topLeft = Point(0.toDouble(), 0.toDouble())
        val topRight = Point(image.cols().toDouble(), 0.toDouble())
        val bottomRight = Point(image.cols().toDouble(), image.rows().toDouble())
        val bottomLeft = Point(0.toDouble(), image.rows().toDouble())
        val points = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        val result = MatOfPoint2f()
        result.fromArray(*points)
        return result
    }

    /**
     * https://stackoverflow.com/a/67071269/4731895
     */
    @ExperimentalGetImage
    fun ImageProxy.convertYUVtoMat(): Mat {
        val img = this.image!!
        val yBuffer = img.planes[0].buffer
        val uBuffer = img.planes[1].buffer
        val vBuffer = img.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuv = Mat(img.height + img.height / 2, img.width, CvType.CV_8UC1)
        yuv.put(0, 0, nv21)
        val rgb = Mat()
        Imgproc.cvtColor(yuv, rgb, Imgproc.COLOR_YUV2RGB_NV21, 3)
        Core.rotate(rgb, rgb, Core.ROTATE_90_CLOCKWISE)
        return rgb
    }
}
