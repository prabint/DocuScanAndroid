package prabin.timsina.documentscanner.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import prabin.timsina.documentscanner.ui.NavGraphs

@Composable
fun MainApp() {
    val navController = rememberNavController()

    MainScaffold {
        DestinationsNavHost(
            navController = navController,
            navGraph = NavGraphs.root,
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        )
    }
}
