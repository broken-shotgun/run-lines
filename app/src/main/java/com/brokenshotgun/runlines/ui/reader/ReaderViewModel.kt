package com.brokenshotgun.runlines.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.repository.ScriptRepository
import com.brokenshotgun.runlines.domain.usecase.GetScriptUseCase
import com.brokenshotgun.runlines.domain.usecase.UpdateScriptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

data class ReaderUiState(
    val script: Script? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ReaderViewModel(repository: ScriptRepository) : ViewModel() {
    private val getScript = GetScriptUseCase(repository)
    private val updateScript = UpdateScriptUseCase(repository)
    private val scriptSaveMutex = Mutex()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadScript(scriptId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val script = getScript(scriptId)
                _uiState.update {
                    it.copy(
                        script = script,
                        isLoading = false,
                        errorMessage = if (script == null) "Script not found" else null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not load script"
                    )
                }
            }
        }
    }

    suspend fun loadScene(scriptId: Long, sceneIndex: Int): Scene? =
        getScript.getScene(scriptId, sceneIndex)

    fun saveScript(script: Script) {
        viewModelScope.launch {
            scriptSaveMutex.withLock {
                try {
                    updateScript(script)
                    _uiState.update { it.copy(script = script, errorMessage = null) }
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Could not save script")
                    }
                }
            }
        }
    }

    companion object {
        fun factory(repository: ScriptRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ReaderViewModel::class.java))
                return ReaderViewModel(repository) as T
            }
        }
    }
}
