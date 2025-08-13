package dev.filipfan.polyengineinfer.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    onToggleStats: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true,
    ) {
        itemsIndexed(messages.asReversed()) { index, message ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
                ) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    MessageBubble(
                        message = message,
                        modifier = Modifier.widthIn(max = screenWidth * 0.8f),
                    )
                }

                // "Show/Hide stats" button.
                if (message.stats != null) {
                    TextButton(
                        onClick = { onToggleStats(messages.size - 1 - index) }, // Pass the correct index
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(if (message.showStats) "Hide stats" else "Show stats")
                    }
                }

                // Show the stats bubble if toggled on.
                if (message.showStats && message.stats != null) {
                    StatsBubble(stats = message.stats)
                }
            }
        }
    }
}
