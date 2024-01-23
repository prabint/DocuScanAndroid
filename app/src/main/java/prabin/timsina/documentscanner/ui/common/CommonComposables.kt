package prabin.timsina.documentscanner.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import prabin.timsina.documentscanner.R

@Composable
fun BottomBar(navigator: DestinationsNavigator, @StringRes rightButtonText: Int, onRightButtonClicked: () -> Unit) {
    val fontColor = Color.White
    val horizontalPadding = 34.dp

    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .background(Color.Black),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(horizontalPadding))
        TextButton(onClick = { navigator.popBackStack() }) {
            Text(
                text = stringResource(R.string.back),
                color = fontColor,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = { onRightButtonClicked() }) {
            Text(
                text = stringResource(rightButtonText),
                color = fontColor,
            )
        }
        Spacer(modifier = Modifier.width(horizontalPadding))
    }
}
