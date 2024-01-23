package prabin.timsina.documentscanner

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import prabin.timsina.documentscanner.opencv.ImageComparator
import prabin.timsina.documentscanner.ui.edit.TAG_BOTTOM_LEFT_CORNER
import prabin.timsina.documentscanner.ui.edit.TAG_BOTTOM_RIGHT_CORNER
import prabin.timsina.documentscanner.ui.edit.TAG_TOP_LEFT_CORNER
import prabin.timsina.documentscanner.ui.edit.TAG_TOP_RIGHT_CORNER

/**
 * This test goes through various screens and takes screenshot. If using ./gradlew to run the test, the screenshots
 * will be saved in:
 * build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/deviceName/image.png
 *
 * Assuming test passed, copy over these screenshots to androidTest/assets folder. These saved screenshots will be
 * be used as golden values and will be used to compare against screenshots taken during test in the future. No need
 * to copy screenshots again unless there is new UI change.
 */
class MainScreenshotTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val uiDevice = UiDevice.getInstance(getInstrumentation())
    private val targetContext = getInstrumentation().targetContext
    private val assets = getInstrumentation().context.assets

    private fun launchApp() {
        val pkg = targetContext.packageName
        val intent = targetContext.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        targetContext.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(pkg).depth(0)), 5000)
    }

    @Test
    fun openCvMinimalImplTest() {
        launchApp()

        uiDevice
            .wait(Until.findObject(By.text(targetContext.getString(R.string.opencv_document_scan_minimal))), 5000)
            .click()

        val similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().apply { writeToTestStorage(name = "opencv_minimal") },
            image2 = assets.open("opencv_minimal.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "opencv_minimal.png didn't match: $similarity" }
    }

    @Test
    fun mlKitPreviewTest() {
        launchApp()

        uiDevice
            .wait(Until.findObject(By.text(targetContext.getString(R.string.mlkit_object_detection))), 5000)
            .click()

        Thread.sleep(2000)

        val similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().apply { writeToTestStorage(name = "mlkit_preview") },
            image2 = assets.open("mlkit_preview.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "mlkit_preview.png didn't match: $similarity" }
    }

    @Test
    fun screenShotCompareTest() {
        launchApp()

        uiDevice
            .wait(Until.findObject(By.text(targetContext.getString(R.string.opencv_document_scan_full))), 5000)
            .click()

        // Live camera screen
        val captureButton =
            uiDevice.wait(Until.findObject(By.desc(targetContext.getString(R.string.capture_photo))), 5000)

        // Wait for camera and opencv to init
        Thread.sleep(2000)

        var similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().apply { writeToTestStorage(name = "opencv_full_1") },
            image2 = assets.open("opencv_full_1.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "opencv_full_1.png didn't match: $similarity" }

        captureButton.click()

        // Polygon edit screen
        val previewButton = uiDevice.wait(Until.findObject(By.text(targetContext.getString(R.string.preview))), 5000)

        similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().also { it.writeToTestStorage(name = "polygon_1") },
            image2 = assets.open("polygon_1.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "polygon_1.png didn't match: $similarity" }

        previewButton.click()

        // Preview screen
        uiDevice.wait(Until.findObject(By.text(targetContext.getString(R.string.save))), 5000)

        similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().also { it.writeToTestStorage(name = "preview_1") },
            image2 = assets.open("preview_1.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "preview_1.png didn't match: $similarity" }

        uiDevice.findObject(By.text(targetContext.getString(R.string.back))).click()

        // Polygon edit screen
        dragCorners()

        similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().also { it.writeToTestStorage(name = "polygon_2") },
            image2 = assets.open("polygon_2.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "polygon_2.png didn't match: $similarity" }

        uiDevice
            .wait(Until.findObject(By.text(targetContext.getString(R.string.preview))), 5000)
            .click()

        // Preview screen
        uiDevice.wait(Until.findObject(By.text(targetContext.getString(R.string.save))), 5000)

        similarity = ImageComparator.isSimilar(
            image1 = onView(isRoot()).captureToBitmap().also { it.writeToTestStorage(name = "preview_2") },
            image2 = assets.open("preview_2.png").use { BitmapFactory.decodeStream(it) },
        )

        assert(similarity) { "preview_2.png didn't match: $similarity" }
    }

    private fun dragCorners() {
        val speed = 1000
        val offset = 100

        val topLeftCorner = uiDevice.wait(Until.findObject(By.desc(TAG_TOP_LEFT_CORNER)), 5000)
        topLeftCorner.drag(
            Point(
                topLeftCorner.visibleCenter.x + offset,
                topLeftCorner.visibleCenter.y - offset,
            ),
            speed,
        )

        val topRightCorner = uiDevice.wait(Until.findObject(By.desc(TAG_TOP_RIGHT_CORNER)), 5000)
        topRightCorner.drag(
            Point(
                topRightCorner.visibleCenter.x - offset,
                topRightCorner.visibleCenter.y - offset,
            ),
            speed,
        )

        val bottomRightCorner = uiDevice.wait(Until.findObject(By.desc(TAG_BOTTOM_RIGHT_CORNER)), 5000)
        bottomRightCorner.drag(
            Point(
                bottomRightCorner.visibleCenter.x - offset,
                bottomRightCorner.visibleCenter.y + offset,
            ),
            speed,
        )

        val bottomLeftCorner = uiDevice.wait(Until.findObject(By.desc(TAG_BOTTOM_LEFT_CORNER)), 5000)
        bottomLeftCorner.drag(
            Point(
                bottomLeftCorner.visibleCenter.x + offset,
                bottomLeftCorner.visibleCenter.y + offset,
            ),
            speed,
        )
    }
}
