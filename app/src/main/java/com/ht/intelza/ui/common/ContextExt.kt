package com.ht.intelza.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The activity behind a (possibly wrapped) context, if there is one. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
