package prabin.timsina.documentscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import prabin.timsina.documentscanner.ui.main.MainApp
import prabin.timsina.documentscanner.ui.theme.DocumentScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocumentScannerTheme {
                MainApp()
            }
        }
    }
}
