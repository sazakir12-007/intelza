package com.ht.intelza.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ht.intelza.AppContainer
import com.ht.intelza.IntelzaApplication

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as IntelzaApplication).container

/**
 * Creates a ViewModel with access to the app's repositories and the navigation
 * arguments (through the [SavedStateHandle]).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (container: AppContainer, savedState: SavedStateHandle) -> VM,
): VM {
    val container = appContainer()
    return viewModel { create(container, createSavedStateHandle()) }
}
