package com.brokenshotgun.runlines.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.ScriptNameNormalizer
import com.brokenshotgun.runlines.ui.reader.playback.TtsPlaybackController
import com.brokenshotgun.runlines.R
import android.widget.Toast

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportScript: (onImportCompleted: () -> Unit) -> Unit,
    onScriptSelected: (Script) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddScriptDialog by remember { mutableStateOf(false) }
    var newScriptName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exportedFileMessage = uiState.exportedFilePath?.let {
        stringResource(R.string.home_saved_fountain, it)
    }
    val errorFallbackMessage = uiState.errorMessageResourceId?.let { stringResource(it) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                TtsPlaybackController.clearActivePlayback(context)
                viewModel.refreshScripts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(exportedFileMessage) {
        exportedFileMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = { showAddScriptDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = stringResource(R.string.home_new_script))
        }

        Button(
            onClick = { onImportScript(viewModel::refreshScripts) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = stringResource(R.string.home_import_script))
        }

        Spacer(modifier = Modifier.height(16.dp))
        (uiState.errorMessage ?: errorFallbackMessage)?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        ScriptListContent(
            scripts = uiState.scripts,
            isLoading = uiState.isLoading,
            onScriptSelected = onScriptSelected,
            onRenameScript = viewModel::renameScript,
            onDeleteScript = viewModel::deleteScript,
            onExportScript = viewModel::exportScript,
            modifier = Modifier.weight(1f)
        )
    }

    if (showAddScriptDialog) {
        AlertDialog(
            onDismissRequest = { showAddScriptDialog = false },
            title = { Text(stringResource(R.string.home_add_script_title)) },
            text = {
                OutlinedTextField(
                    value = newScriptName,
                    onValueChange = { newScriptName = it },
                    label = { Text(stringResource(R.string.home_script_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createScript(newScriptName)
                        newScriptName = ""
                        showAddScriptDialog = false
                    },
                    enabled = ScriptNameNormalizer.normalize(newScriptName).isNotBlank()
                ) {
                    Text(stringResource(R.string.common_add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newScriptName = ""
                        showAddScriptDialog = false
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
