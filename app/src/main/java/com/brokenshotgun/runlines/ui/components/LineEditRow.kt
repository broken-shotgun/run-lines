package com.brokenshotgun.runlines.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Line

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineEditRow(
    line: Line,
    isSelected: Boolean,
    isEditing: Boolean,
    editingActorName: String,
    editingLineText: String,
    characterSuggestions: List<String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActorValueChange: (String) -> Unit,
    onLineValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val isActionLine = line.actor == Actor.ACTION
    var expanded by remember { mutableStateOf(false) }
    val matchingSuggestions = remember(editingActorName, characterSuggestions) {
        val query = editingActorName.trim()
        if (query.isBlank()) {
            characterSuggestions
        } else {
            characterSuggestions.filter { it.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded && matchingSuggestions.isNotEmpty(),
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = editingActorName,
                        onValueChange = {
                            expanded = true
                            onActorValueChange(it)
                        },
                        label = { Text("Character") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && matchingSuggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        matchingSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onActorValueChange(suggestion)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editingLineText,
                    onValueChange = onLineValueChange,
                    label = { Text("Line") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        } else {
            if (!isActionLine) {
                Text(
                    text = line.actor.name.uppercase(),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (line.line.isNotBlank()) {
                Text(
                    text = line.line,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = if (isActionLine) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
