package dev.filipfan.polyengineinfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.filipfan.polyengineinfer.ui.chat.ChatScreen
import dev.filipfan.polyengineinfer.ui.chat.ChatViewModel
import dev.filipfan.polyengineinfer.ui.settings.SettingsViewModel
import dev.filipfan.polyengineinfer.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChatScreen(chatViewModel, settingsViewModel)
                }
            }
        }
    }
}
