package prabin.timsina.documentscanner.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

// Add top and bottom bars here if required.
@Composable
fun MainScaffold(content: @Composable (PaddingValues) -> Unit) {
    /**
     * This code changes the size of the image container box by changing the size of the top app bar. This is just to
     * verify that the live canvas preview also scales as box (or image) size changes.
     */
    // var height by remember { mutableStateOf(0.dp) }
    // var ticks by remember { mutableIntStateOf(0) }
    // LaunchedEffect(Unit) {
    //     while (true) {
    //         delay(5.seconds)
    //         ticks++
    //         height = if (height == 0.dp) 100.dp else 0.dp
    //     }
    // }

    Scaffold(
        content = content,
        // topBar = { Box(modifier = Modifier.height(height)) }
    )
}
