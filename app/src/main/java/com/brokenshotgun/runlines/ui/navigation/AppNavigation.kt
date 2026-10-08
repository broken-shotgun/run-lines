package com.brokenshotgun.runlines.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.brokenshotgun.runlines.AppContainer
import com.brokenshotgun.runlines.ui.home.HomeScreen
import com.brokenshotgun.runlines.ui.home.HomeViewModel
import com.brokenshotgun.runlines.ui.reader.ReadSceneScreen
import com.brokenshotgun.runlines.ui.reader.ReaderViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class ReadScene(val scriptId: Long, val sceneIndex: Int) : AppRoute
}

@Composable
fun AppNavigation(
    onImportScript: (onImportCompleted: () -> Unit) -> Unit,
    appContainer: AppContainer,
    initialScriptId: Long = -1L,
    initialSceneIndex: Int = 0
) {
    val initialRoute: AppRoute = if (initialScriptId >= 0L) {
        AppRoute.ReadScene(initialScriptId, initialSceneIndex.coerceAtLeast(0))
    } else {
        AppRoute.Home
    }
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(initialRoute)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<AppRoute.Home> {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(appContainer.repository, appContainer.exporter)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onImportScript = onImportScript,
                    onScriptSelected = { script ->
                        backStack.add(AppRoute.ReadScene(script.id, 0))
                    }
                )
            }

            entry<AppRoute.ReadScene> { key ->
                val readerViewModel: ReaderViewModel = viewModel(
                    factory = ReaderViewModel.factory(appContainer.repository)
                )
                val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(key.scriptId) {
                    readerViewModel.loadScript(key.scriptId)
                }

                when {
                    readerState.script != null -> ReadSceneScreen(
                        script = readerState.script!!,
                        sceneIndex = key.sceneIndex,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        onSaveScript = readerViewModel::saveScript
                    )
                    readerState.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(readerState.errorMessage ?: "Could not open script")
                        TextButton(
                            onClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        ) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    )
}
