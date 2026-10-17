package com.brokenshotgun.runlines

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.brokenshotgun.runlines.data.importing.FountainScriptContentParser
import com.brokenshotgun.runlines.R
import com.brokenshotgun.runlines.domain.usecase.ImportScriptUseCase
import com.brokenshotgun.runlines.ui.navigation.AppNavigation
import com.brokenshotgun.runlines.ui.reader.playback.TtsPlaybackController
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer
        get() = (application as RunLinesApplication).container
    private var onImportCompleted: (() -> Unit)? = null
    private var pendingPdfImport by mutableStateOf<PendingPdfImport?>(null)
    private var importedScriptId by mutableStateOf<Long?>(null)

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onImportCompleted = null
        } else {
            requestImport(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        onImportScript = { onImported ->
                            onImportCompleted = onImported
                            openDocument.launch(arrayOf("*/*"))
                        },
                        appContainer = appContainer,
                        initialScriptId = intent.getLongExtra("script_id", -1L),
                        initialSceneIndex = intent.getIntExtra("scene_index", 0),
                        importedScriptId = importedScriptId,
                        onImportedScriptOpened = { importedScriptId = null }
                    )

                    pendingPdfImport?.let { pendingImport ->
                        AlertDialog(
                            onDismissRequest = {
                                pendingPdfImport = null
                                onImportCompleted = null
                            },
                            title = { Text(stringResource(R.string.pdf_large_warning_title)) },
                            text = { Text(stringResource(R.string.pdf_large_warning_message)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        pendingPdfImport = null
                                        importScript(pendingImport.uri, pendingImport.fileName)
                                    }
                                ) {
                                    Text(stringResource(R.string.pdf_large_warning_import))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        pendingPdfImport = null
                                        onImportCompleted = null
                                    }
                                ) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            handleImportIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            TtsPlaybackController.hideNotification(this)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            TtsPlaybackController.hideNotification(this)
        }
        super.onDestroy()
    }

    private fun handleImportIntent(intent: Intent) {
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        } ?: return
        requestImport(uri)
    }

    private fun requestImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val (fileName, fileSize) = withContext(Dispatchers.IO) {
                    getDocumentMetadata(uri)
                }
                if (fileName.substringAfterLast('.', "").lowercase() !in SUPPORTED_EXTENSIONS) {
                    error(getString(R.string.error_unsupported_script_type))
                }
                if (
                    fileName.endsWith(".pdf", ignoreCase = true) &&
                    fileSize != null &&
                    fileSize > LARGE_PDF_WARNING_SIZE_BYTES
                ) {
                    pendingPdfImport = PendingPdfImport(uri, fileName)
                } else {
                    importScript(uri, fileName)
                }
            } catch (error: Exception) {
                showImportError(error)
            }
        }
    }

    private fun importScript(uri: Uri, fileName: String) {
        lifecycleScope.launch {
            try {
                val importedScript = withContext(Dispatchers.IO) {
                    val content = when (fileName.substringAfterLast('.', "").lowercase()) {
                        "pdf" -> extractPdfText(uri)
                        else -> contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error(getString(R.string.error_could_not_open_selected_file))
                    }
                    ImportScriptUseCase(
                        repository = appContainer.repository,
                        parser = FountainScriptContentParser(
                            untitledScriptName = getString(R.string.playback_untitled_script),
                            untitledSceneName = getString(R.string.scene_untitled)
                        )
                    )(fileName, content)
                }
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.home_script_imported),
                    Toast.LENGTH_SHORT
                ).show()
                onImportCompleted?.invoke()
                onImportCompleted = null
                importedScriptId = importedScript.id
            } catch (error: Exception) {
                onImportCompleted = null
                showImportError(error)
            }
        }
    }

    private fun showImportError(error: Exception) {
        onImportCompleted = null
        Toast.makeText(
            this,
            error.message ?: getString(R.string.error_could_not_import_script),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun getDocumentMetadata(uri: Uri): Pair<String, Long?> {
        var displayName: String? = null
        var size: Long? = null
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) {
                    displayName = cursor.getString(nameColumn)
                }
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    size = cursor.getLong(sizeColumn)
                }
            }
        }
        if (size == null || size < 0L) {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L }?.let { size = it }
            }
        }
        val resolvedName = displayName
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        if (resolvedName != null && resolvedName.contains('.')) {
            return resolvedName to size
        }
        val inferredExtension = when (contentResolver.getType(uri)) {
            "application/pdf" -> "pdf"
            "application/x-fountain" -> "fountain"
            "text/x-fountain" -> "fountain"
            "application/vnd.finaldraft" -> "fdx"
            "application/x-fdx" -> "fdx"
            "text/plain" -> "txt"
            else -> null
        }
        val name = when {
            resolvedName != null && inferredExtension != null -> "$resolvedName.$inferredExtension"
            resolvedName != null -> resolvedName
            inferredExtension != null -> "script.$inferredExtension"
            else -> getString(R.string.default_script_filename)
        }
        return name to size
    }

    private fun extractPdfText(uri: Uri): String {
        val input = contentResolver.openInputStream(uri)
            ?: error(getString(R.string.error_could_not_open_selected_pdf))
        return input.use {
            PDDocument.load(it, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    }

    private data class PendingPdfImport(val uri: Uri, val fileName: String)

    companion object {
        private const val LARGE_PDF_WARNING_SIZE_BYTES = 20L * 1024L * 1024L
        private val SUPPORTED_EXTENSIONS = setOf("txt", "fountain", "pdf", "fdx")
    }
}
