package prabin.timsina.documentscanner.ui.common

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.opencv.android.Utils
import org.opencv.core.Mat

const val PATH_PHOTOS = "photos"

fun Bitmap.saveToCache(context: Context, saveAsFileName: String = UUID.randomUUID().toString()): String {
    val photosDir = File(context.cacheDir, PATH_PHOTOS)
    if (!photosDir.exists()) {
        photosDir.mkdirs()
    }

    val file = File(photosDir, "$saveAsFileName.jpg")
    FileOutputStream(file).use { this.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    return file.absolutePath
}

fun deleteCachePhoto(context: Context, filename: String) {
    val photosDir = File(context.cacheDir, PATH_PHOTOS)
    val file = File(photosDir, filename)
    if (photosDir.exists() && file.exists()) {
        file.delete()
    }
}

fun Bitmap.toMat() = Mat().apply { Utils.bitmapToMat(this@toMat, this) }
