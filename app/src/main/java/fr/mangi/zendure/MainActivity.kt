package fr.mangi.zendure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mangi.zendure.ui.navigation.ZendureNavGraph
import fr.mangi.zendure.ui.theme.ZendureTheme
import fr.mangi.zendure.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZendureTheme {
                val viewModel: MainViewModel = viewModel()
                ZendureNavGraph(viewModel)
            }
        }
    }
}
