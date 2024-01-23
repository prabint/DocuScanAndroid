package prabin.timsina.documentscanner.opencv

import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point

class Quadrilateral(val contour: MatOfPoint2f, val points: List<Point>)
