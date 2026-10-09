package com.brokenshotgun.runlines

import android.app.Application
import android.os.Environment
import com.brokenshotgun.runlines.data.export.FountainFileExporter
import com.brokenshotgun.runlines.data.local.ScriptDatabase
import com.brokenshotgun.runlines.data.repository.LocalScriptRepository
import com.brokenshotgun.runlines.domain.repository.ScriptExporter
import com.brokenshotgun.runlines.domain.repository.ScriptRepository
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class RunLinesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        container = AppContainer(
            repository = LocalScriptRepository(ScriptDatabase(this)),
            exporter = FountainFileExporter(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            )
        )
    }
}

data class AppContainer(
    val repository: ScriptRepository,
    val exporter: ScriptExporter
)
