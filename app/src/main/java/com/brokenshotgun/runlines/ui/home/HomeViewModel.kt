package com.brokenshotgun.runlines.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.ScriptNameNormalizer
import com.brokenshotgun.runlines.domain.repository.ScriptRepository
import com.brokenshotgun.runlines.domain.repository.ScriptExporter
import com.brokenshotgun.runlines.domain.usecase.CreateScriptUseCase
import com.brokenshotgun.runlines.domain.usecase.DeleteScriptUseCase
import com.brokenshotgun.runlines.domain.usecase.ExportScriptUseCase
import com.brokenshotgun.runlines.domain.usecase.GetScriptsUseCase
import com.brokenshotgun.runlines.domain.usecase.UpdateScriptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class HomeUiState(
    val scripts: List<Script> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val exportedFilePath: String? = null
)

class HomeViewModel(
    repository: ScriptRepository,
    exporter: ScriptExporter
) : ViewModel() {
    private val getScripts = GetScriptsUseCase(repository)
    private val createScriptUseCase = CreateScriptUseCase(repository)
    private val updateScriptUseCase = UpdateScriptUseCase(repository)
    private val deleteScriptUseCase = DeleteScriptUseCase(repository)
    private val exportScriptUseCase = ExportScriptUseCase(exporter)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshScripts()
    }

    fun refreshScripts() {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            try {
                val scripts = getScripts()
                _uiState.update { it.copy(scripts = scripts, isLoading = false) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not load scripts"
                    )
                }
            }
        }
    }

    fun createScript(name: String) {
        val normalizedName = ScriptNameNormalizer.normalize(name)
        if (normalizedName.isEmpty()) return
        viewModelScope.launch {
            try {
                createScriptUseCase(normalizedName)
                refreshScripts()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not create script")
                }
            }
        }
    }

    fun renameScript(script: Script, name: String) {
        val normalizedName = ScriptNameNormalizer.normalize(name)
        if (normalizedName.isEmpty()) return
        viewModelScope.launch {
            try {
                val renamedScript = script.copy(name = normalizedName).also {
                    it.defaultVoice = script.defaultVoice
                }
                updateScriptUseCase(renamedScript)
                refreshScripts()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not rename script")
                }
            }
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            try {
                deleteScriptUseCase(script)
                refreshScripts()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not delete script")
                }
            }
        }
    }

    fun exportScript(script: Script) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filePath = exportScriptUseCase(script)
                _uiState.update {
                    it.copy(exportedFilePath = filePath, errorMessage = null)
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not export script")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, exportedFilePath = null) }
    }

    companion object {
        fun factory(repository: ScriptRepository, exporter: ScriptExporter) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                    return HomeViewModel(repository, exporter) as T
                }
            }
    }
}
