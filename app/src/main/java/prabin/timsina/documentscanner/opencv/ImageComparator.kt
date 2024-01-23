package prabin.timsina.documentscanner.opencv

import android.graphics.Bitmap
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import prabin.timsina.documentscanner.ui.common.toMat
import timber.log.Timber

object ImageComparator {

    /**
     * Percentage two bitmaps expected to be similar
     */
    private const val EXPECTED_SIMILARITY = 97.0

    /**
     * We can't use bitmap1.sameAs(bitmap2) because the emulator camera is shaky (mimics human hands). So, the captured
     * images are always very slightly different.
     */
    fun isSimilar(image1: Bitmap, image2: Bitmap) = getSimilarityPercentage(image1, image2) > EXPECTED_SIMILARITY

    /**
     * Uses OpenCV absDiff to compare images and return the percentage of similarity.
     */
    private fun getSimilarityPercentage(bitmap1: Bitmap, bitmap2: Bitmap): Double {
        val mat1 = bitmap1.toMat()
        val mat2 = bitmap2.toMat()

        Imgproc.cvtColor(mat1, mat1, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_BGR2GRAY)

        val diffMat = Mat()
        Core.absdiff(mat1, mat2, diffMat)

        val sumAbsDiff = Core.sumElems(diffMat)

        val totalPixels = mat1.rows() * mat1.cols()

        // Calculate percentage similarity based on the average absolute difference
        val avgAbsDiff = sumAbsDiff.`val`[0] / totalPixels
        val similarityPercentage = (1.0 - avgAbsDiff / 255.0) * 100.0

        Timber.d("getSimilarityPercentage similarityPercentage = $similarityPercentage")

        return similarityPercentage
    }
}
