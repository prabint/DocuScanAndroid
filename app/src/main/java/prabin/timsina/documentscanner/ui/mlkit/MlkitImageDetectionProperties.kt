package prabin.timsina.documentscanner.ui.mlkit

import android.graphics.Point

data class MlkitImageDetectionProperties(
    val rect: QuadrilateralCoordinates,
    val postScaleWidthOffset: Float,
)

data class QuadrilateralCoordinates(
    val topLeft: Point,
    val topRight: Point,
    val bottomRight: Point,
    val bottomLeft: Point,
)
