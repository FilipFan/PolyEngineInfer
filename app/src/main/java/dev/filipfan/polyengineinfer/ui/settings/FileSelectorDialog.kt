package dev.filipfan.polyengineinfer.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

private const val TAG = "FileSelectorDialog"

@Composable
fun FileSelectorDialog(
    onFileSelected: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Start at the app's specific external files directory.
    val startDir = context.getExternalFilesDir(null) ?: run {
        Log.e(TAG, "External storage directory is not available.")
        return
    }
    var currentDir by remember { mutableStateOf(startDir) }

    val files by remember(currentDir) {
        derivedStateOf {
            currentDir.listFiles()?.sorted()?.toList() ?: emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            Column {
                // Header with current path and back button.
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentDir.absolutePath != startDir.absolutePath) {
                        IconButton(onClick = { currentDir = currentDir.parentFile ?: startDir }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = currentDir.path.substringAfter(startDir.path),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider()

                // File and directory list.
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(files) { file ->
                        FileListItem(file = file) { selectedFile ->
                            if (selectedFile.isDirectory) {
                                // Navigate into directory.
                                currentDir = selectedFile
                            } else {
                                // Select file.
                                onFileSelected(selectedFile)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Select current directory button.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = { onFileSelected(currentDir) }) {
                        Text("Select Current Directory")
                    }
                }
            }
        }
    }
}
