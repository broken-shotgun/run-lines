package com.brokenshotgun.runlines.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brokenshotgun.runlines.R
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.ScriptNameNormalizer

@Composable
fun ScriptListContent(
    scripts: List<Script>,
    isLoading: Boolean,
    onScriptSelected: (Script) -> Unit,
    onRenameScript: (Script, String) -> Unit,
    onDeleteScript: (Script) -> Unit,
    onExportScript: (Script) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedScriptId by remember { mutableStateOf<Long?>(null) }
    var scriptToRename by remember { mutableStateOf<Script?>(null) }
    var scriptToDelete by remember { mutableStateOf<Script?>(null) }
    var renameValue by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.script_list_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading && scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.script_list_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(scripts) { _, script ->
                    Box {
                        ListItem(
                            headlineContent = { Text(text = script.name) },
                            modifier = Modifier.clickable { onScriptSelected(script) }
                        )
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            IconButton(
                                onClick = { expandedScriptId = if (expandedScriptId == script.id) null else script.id }
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.script_options)
                                )
                            }
                            DropdownMenu(
                                expanded = expandedScriptId == script.id,
                                onDismissRequest = { expandedScriptId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.script_rename)) },
                                    onClick = {
                                        renameValue = script.name
                                        scriptToRename = script
                                        expandedScriptId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.script_save_fountain)) },
                                    onClick = {
                                        onExportScript(script)
                                        expandedScriptId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.script_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        expandedScriptId = null
                                        scriptToDelete = script
                                    }
                                )
                            }
                        }

                        scriptToRename?.let { script ->
                            AlertDialog(
                                onDismissRequest = { scriptToRename = null },
                                title = { Text(stringResource(R.string.script_rename)) },
                                text = {
                                    OutlinedTextField(
                                        value = renameValue,
                                        onValueChange = { renameValue = it },
                                        label = { Text(stringResource(R.string.home_script_name_label)) },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            onRenameScript(script, renameValue)
                                            scriptToRename = null
                                        },
                                        enabled = ScriptNameNormalizer.normalize(renameValue).isNotBlank()
                                    ) {
                                        Text(stringResource(R.string.common_rename))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { scriptToRename = null }) {
                                        Text(stringResource(R.string.common_cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        scriptToDelete?.let { script ->
            AlertDialog(
                onDismissRequest = { scriptToDelete = null },
                title = { Text(stringResource(R.string.script_delete_title)) },
                text = {
                    Text(stringResource(R.string.script_delete_confirmation, script.name))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteScript(script)
                            scriptToDelete = null
                        }
                    ) {
                        Text(
                            stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scriptToDelete = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}
