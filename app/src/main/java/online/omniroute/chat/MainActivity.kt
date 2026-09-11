package online.omniroute.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import online.omniroute.chat.ui.ChatViewModel
import online.omniroute.chat.ui.OmniApp
import online.omniroute.chat.ui.OmniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ChatViewModel = viewModel()
            OmniTheme {
                OmniApp(vm = remember { vm })
            }
        }
    }
}
