package dev.filipfan.polyengineinfer.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsBubble(stats: InferenceStats, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Stats", style = MaterialTheme.typography.titleSmall)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Time to first token: ${stats.timeToFirstToken} ms")
                Text("Prefill: ${stats.prefillTokens} tokens (${"%.2f".format(stats.prefillSpeed)} t/s)")
                Text("Decode: ${stats.decodeTokens} tokens (${"%.2f".format(stats.decodeSpeed)} t/s)")
                Text("Total Latency: ${stats.latency} ms")
            }
        }
    }
}
