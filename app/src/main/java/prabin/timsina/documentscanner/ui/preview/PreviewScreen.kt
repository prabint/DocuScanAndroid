package prabin.timsina.documentscanner.ui.preview

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import prabin.timsina.documentscanner.R
import prabin.timsina.documentscanner.ui.NavGraphs
import prabin.timsina.documentscanner.ui.common.BottomBar

/**
 * Displays the final image after perspective transformation is applied.
 */
@Composable
@Destination(navArgsDelegate = PreviewScreenNavArgs::class)
fun PreviewScreen(navigator: DestinationsNavigator, viewModel: PreviewViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val startForResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val destUri = result.data?.data!!

                context.contentResolver.openOutputStream(destUri)?.use {
                    viewModel.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                navigator.navigate(NavGraphs.root) { popUpTo(NavGraphs.root) }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            bitmap = viewModel.bitmap.asImageBitmap(),
            contentDescription = null,
        )

        BottomBar(
            navigator = navigator,
            rightButtonText = R.string.save,
            onRightButtonClicked = {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TITLE, "document.jpeg")
                }
                startForResult.launch(intent)
            },
        )
    }
}
