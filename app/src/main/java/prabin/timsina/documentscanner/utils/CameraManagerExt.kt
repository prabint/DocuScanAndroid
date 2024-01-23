package prabin.timsina.documentscanner.utils

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.camera.core.CameraSelector
import timber.log.Timber

/**
 * https://github.com/googlesamples/mlkit/blob/master/android/automl/app/src/main/java/com/google/mlkit/vision/automl/demo/preference/CameraXLivePreviewPreferenceFragment.java#L56
 */
private fun CameraManager.getCameraResolutions(): List<Size> {
    val cameraCharacteristics = getCameraCharacteristics(
        lensFacing = CameraSelector.LENS_FACING_BACK,
    )

    val entries = mutableListOf<Size>()

    if (cameraCharacteristics != null) {
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputSizes = map!!.getOutputSizes(SurfaceTexture::class.java)
        for (i in outputSizes.indices) {
            val size = Size.parseSize(outputSizes[i].toString())
            entries.add(Size(size.height, size.width))
        }
    }

    Timber.i("Resolutions = $entries")

    return entries.toList()
}

private fun CameraManager.getCameraCharacteristics(lensFacing: Int): CameraCharacteristics? {
    try {
        val cameraList = listOf(*this.cameraIdList)
        for (availableCameraId in cameraList) {
            val availableCameraCharacteristics = this.getCameraCharacteristics(availableCameraId!!)
            val availableLensFacing = availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                ?: continue
            if (availableLensFacing == lensFacing) {
                return availableCameraCharacteristics
            }
        }
    } catch (e: CameraAccessException) {
        Timber.e(e, "getCameraCharacteristics failed")
    }
    return null
}

/**
 * Returns list of resolutions of ratio 3:4 sorted in ascending order by width
 */
fun CameraManager.get3by4Resolutions() = getCameraResolutions()
    .filter { it.width.toFloat() / it.height == 3f / 4 }
    .sortedBy { it.width }
