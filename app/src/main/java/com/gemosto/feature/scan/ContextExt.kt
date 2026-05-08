package com.gemosto.feature.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walk ContextWrapper chain sampai ketemu Activity.
 * Dipakai untuk:
 *  - shouldShowRequestPermissionRationale (butuh Activity)
 *  - lock screen orientation di camera screen
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
