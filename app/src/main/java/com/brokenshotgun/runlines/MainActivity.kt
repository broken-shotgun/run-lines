package com.brokenshotgun.runlines

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.brokenshotgun.runlines.data.importing.FountainScriptContentParser
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

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onImportCompleted = null
        } else {
            importScript(uri)
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
                        initialSceneIndex = intent.getIntExtra("scene_index", 0)
                    )
                }
            }
        }
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

    private fun importScript(uri: Uri) {
        lifecycleScope.launch {
            try {
                val fileName = withContext(Dispatchers.IO) {
                    getDisplayName(uri)
                }
                withContext(Dispatchers.IO) {
                    val content = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                        extractPdfText(uri)
                    } else {
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not open the selected file")
                    }
                    ImportScriptUseCase(
                        repository = appContainer.repository,
                        parser = FountainScriptContentParser()
                    )(fileName, content)
                }
                Toast.makeText(this@MainActivity, "Script imported successfully", Toast.LENGTH_SHORT).show()
                onImportCompleted?.invoke()
                onImportCompleted = null
            } catch (error: Exception) {
                onImportCompleted = null
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: "Could not import script",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) {
                    cursor.getString(nameColumn)?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "script.txt"
    }

    private fun extractPdfText(uri: Uri): String {
        val input = contentResolver.openInputStream(uri) ?: error("Could not open the selected PDF")
        return input.use {
            PDDocument.load(it, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    }
}
