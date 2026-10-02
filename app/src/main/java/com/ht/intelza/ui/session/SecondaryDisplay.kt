package com.ht.intelza.ui.session

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ht.intelza.ui.theme.IntelzaTheme

/**
 * Shows the presenter view on a second display (HDMI or wireless) while one is connected
 * (requirement E2). Returns the name of the display in use, or null.
 */
@Composable
fun SecondaryDisplayPresenter(state: PresenterState?): String? {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? ComponentActivity } ?: return null
    val displayManager = remember(context) { context.getSystemService(DisplayManager::class.java) } ?: return null
    var display by remember { mutableStateOf(displayManager.presentationDisplay()) }

    DisposableEffect(displayManager) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                display = displayManager.presentationDisplay()
            }

            override fun onDisplayRemoved(displayId: Int) {
                display = displayManager.presentationDisplay()
            }

            override fun onDisplayChanged(displayId: Int) = Unit
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }

    // Shared with the presentation's own composition, which reads it on every change.
    val shared: MutableState<PresenterState?> = remember { mutableStateOf(state) }
    SideEffect { shared.value = state }

    val target = display ?: return null
    var shown by remember(target) { mutableStateOf(false) }
    DisposableEffect(target) {
        val presentation = Presentation(activity, target)
        val view = ComposeView(presentation.context).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setContent { IntelzaTheme(darkTheme = true) { PresenterContent(shared.value) } }
        }
        presentation.setContentView(view)
        presentation.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(activity)
            decor.setViewTreeViewModelStoreOwner(activity)
            decor.setViewTreeSavedStateRegistryOwner(activity)
        }
        shown = try {
            presentation.show()
            true
        } catch (e: WindowManager.InvalidDisplayException) {
            Log.w("Presenter", "Second display went away", e)
            false
        }
        onDispose { presentation.dismiss() }
    }
    return if (shown) target.name else null
}

private fun DisplayManager.presentationDisplay(): Display? =
    getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
