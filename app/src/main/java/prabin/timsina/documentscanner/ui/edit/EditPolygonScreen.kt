package prabin.timsina.documentscanner.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import org.opencv.core.Point
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.ui.common.BottomBar
import prabin.timsina.documentscanner.ui.common.pxToDp
import prabin.timsina.documentscanner.ui.destinations.PreviewScreenDestination
import prabin.timsina.documentscanner.ui.edit.EditPolygonViewModel.Companion.DIAMETER
import prabin.timsina.documentscanner.ui.edit.EditPolygonViewModel.Companion.RADIUS

const val TAG_TOP_LEFT_CORNER = "Top left corner"
const val TAG_TOP_RIGHT_CORNER = "Top right corner"
const val TAG_BOTTOM_RIGHT_CORNER = "Bottom right corner"
const val TAG_BOTTOM_LEFT_CORNER = "Bottom left corner"

/**
 * A screen that has image of a document and polygon view on top of it. The polygon view has 4 corners and can be
 * dragged to adjust to the boundary of the document on the image before performing the transformation.
 */
@Composable
@Destination(navArgsDelegate = EditPolygonScreenNavArgs::class)
fun EditPolygonScreen(navigator: DestinationsNavigator, viewModel: EditPolygonViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val scaledBitmap by viewModel.scaledBitmap.collectAsState()
    val previousBoxSize by viewModel.previousBoxSize.collectAsState()
    val imageOffsetInsideBox by viewModel.imageOffsetInsideBox.collectAsState()
    val adjustedScaledCornerPoints by viewModel.adjustedScaledCornerPoints.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ImageAndPolygonView(
            modifier = Modifier.weight(1f),
            previousBoxSize = previousBoxSize,
            viewModel = viewModel,
            scaledBitmap = scaledBitmap,
            adjustedScaledCornerPoints = adjustedScaledCornerPoints,
        )

        BottomBar(
            navigator = navigator,
            rightButtonText = R.string.preview,
        ) {
            val uri = viewModel.onClickPreview(
                context = context,
                imageOffsetInsideBox = imageOffsetInsideBox,
                adjustedScaledCornerPoints = adjustedScaledCornerPoints,
                scaledBitmap = scaledBitmap!!,
            )

            navigator.navigate(PreviewScreenDestination(uri))
        }
    }
}

@Composable
fun ImageAndPolygonView(
    modifier: Modifier,
    viewModel: EditPolygonViewModel,
    previousBoxSize: IntSize,
    scaledBitmap: Bitmap?,
    adjustedScaledCornerPoints: List<Point>,
) {
    /**
     *  When [Box] size changes, so does the image and so do the corner points. When new corner points are calculated,
     *  we force [PolygonView] to re-draw using this variable.
     *  Also see comment for [previousBoxSize].
     */
    var shouldRecomposePolygonView by remember { mutableStateOf(false) }
    // val scaledBitmap by viewModel.scaledBitmap.collectAsState()
    // val adjustedScaledCornerPoints by viewModel.adjustedScaledCornerPoints.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { boxCoordinates ->
                // If box size has changed
                if (boxCoordinates.size != previousBoxSize) {
                    viewModel.onPositioned(boxSize = boxCoordinates.size)

                    // Force recomposition of [PolygonView] as size of image container has changed
                    shouldRecomposePolygonView = true
                }
            },
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            contentDescription = null,
            bitmap = when (scaledBitmap == null) {
                true -> Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImageBitmap() // Placeholder
                false -> scaledBitmap.asImageBitmap()
            },
        )

        if (shouldRecomposePolygonView) {
            shouldRecomposePolygonView = false
        } else {
            PolygonView(
                corners = adjustedScaledCornerPoints,
                onMove = { viewModel.onMove(it) },
            )
        }
    }
}

/**
 * @param onMove Callback when user moves a corner.
 */
@Composable
private fun PolygonView(corners: List<Point>, onMove: (List<Point>) -> Unit) {
    if (corners.size != 4) return

    var topLeft by remember { mutableStateOf(corners[0]) }
    var topRight by remember { mutableStateOf(corners[1]) }
    var bottomRight by remember { mutableStateOf(corners[2]) }
    var bottomLeft by remember { mutableStateOf(corners[3]) }

    Circle(
        point = topLeft,
        color = Color.Red,
        tag = TAG_TOP_LEFT_CORNER,
        onMove = { offset ->
            topLeft = Point(offset.x.toDouble(), offset.y.toDouble())
            onMove(listOf(topLeft, topRight, bottomRight, bottomLeft))
        },
    )

    Circle(
        point = topRight,
        color = Color.Green,
        tag = TAG_TOP_RIGHT_CORNER,
        onMove = { offset ->
            topRight = Point(offset.x.toDouble(), offset.y.toDouble())
            onMove(listOf(topLeft, topRight, bottomRight, bottomLeft))
        },
    )

    Circle(
        point = bottomRight,
        color = Color.Blue,
        tag = TAG_BOTTOM_RIGHT_CORNER,
        onMove = { offset ->
            bottomRight = Point(offset.x.toDouble(), offset.y.toDouble())
            onMove(listOf(topLeft, topRight, bottomRight, bottomLeft))
        },
    )

    Circle(
        point = bottomLeft,
        color = Color.Yellow,
        tag = TAG_BOTTOM_LEFT_CORNER,
        onMove = { offset ->
            bottomLeft = Point(offset.x.toDouble(), offset.y.toDouble())
            onMove(listOf(topLeft, topRight, bottomRight, bottomLeft))
        },
    )

    DrawLines(
        topLeft = topLeft,
        topRight = topRight,
        bottomRight = bottomRight,
        bottomLeft = bottomLeft,
    )
}

/**
 * Use a [Box] to draw circle instead of Canvas.drawPoints() because Box takes pointerInput. The drawback of using box
 * is that it's center is not at center (0,0) of circle but at top-left corner. We have to consider some offset for this.
 */
@Composable
private fun Circle(point: Point, color: Color, tag: String, onMove: (IntOffset) -> Unit) {
    var touchOffset by remember {
        mutableStateOf(
            IntOffset(
                x = point.x.roundToInt(),
                y = point.y.roundToInt(),
            ),
        )
    }

    Box(
        modifier = Modifier
            .offset { touchOffset } // Must be at top
            .size(DIAMETER.pxToDp())
            .border(width = 1.dp, color = Color.White, shape = CircleShape)
            .aspectRatio(1f)
            .background(
                brush = SolidColor(value = color),
                shape = CircleShape,
                alpha = 0.5f,
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    val x = (touchOffset.x + dragAmount.x).roundToInt()
                    val y = (touchOffset.y + dragAmount.y).roundToInt()

                    touchOffset = IntOffset(x, y)
                    onMove(touchOffset)
                }
            }
            .semantics { contentDescription = tag },
    )
}

@Composable
private fun DrawLines(topLeft: Point, topRight: Point, bottomRight: Point, bottomLeft: Point) {
    val strokeWidth = 10f

    /**
     * Extend the lines slightly by [RADIUS] amount. This is needed so that the line starts and ends right at the center
     * of the circles (box). But, we moved the circles in adjustedScaledCornerPoints, so need to compensate for that.
     * We wouldn't need this if we used Canvas.drawPoints() instead of Box.
     */
    val topLeftAdjusted = Point(
        topLeft.x + RADIUS,
        topLeft.y + RADIUS,
    )

    val topRightAdjusted = Point(
        topRight.x + RADIUS,
        topRight.y + RADIUS,
    )

    val bottomRightAdjusted = Point(
        bottomRight.x + RADIUS,
        bottomRight.y + RADIUS,
    )

    val bottomLeftAdjusted = Point(
        bottomLeft.x + RADIUS,
        bottomLeft.y + RADIUS,
    )

    Canvas(
        modifier = Modifier,
        onDraw = {
            drawLine(
                color = Color.Red,
                start = Offset(topLeftAdjusted.x.toFloat(), topLeftAdjusted.y.toFloat()),
                end = Offset(topRightAdjusted.x.toFloat(), topRightAdjusted.y.toFloat()),
                cap = StrokeCap.Round,
                strokeWidth = strokeWidth,
            )

            drawLine(
                color = Color.Green,
                start = Offset(topRightAdjusted.x.toFloat(), topRightAdjusted.y.toFloat()),
                end = Offset(bottomRightAdjusted.x.toFloat(), bottomRightAdjusted.y.toFloat()),
                cap = StrokeCap.Round,
                strokeWidth = strokeWidth,
            )

            drawLine(
                color = Color.Blue,
                start = Offset(bottomRightAdjusted.x.toFloat(), bottomRightAdjusted.y.toFloat()),
                end = Offset(bottomLeftAdjusted.x.toFloat(), bottomLeftAdjusted.y.toFloat()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            drawLine(
                color = Color.Yellow,
                start = Offset(bottomLeftAdjusted.x.toFloat(), bottomLeftAdjusted.y.toFloat()),
                end = Offset(topLeftAdjusted.x.toFloat(), topLeftAdjusted.y.toFloat()),
                cap = StrokeCap.Round,
                strokeWidth = strokeWidth,
            )
        },
    )
}
