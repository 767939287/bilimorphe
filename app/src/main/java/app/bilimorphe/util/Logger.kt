package app.bilimorphe.util

import de.robv.android.xposed.XposedBridge

/**
 * Thin logging wrapper. Everything goes to both logcat and the Xposed/LSPosed log
 * so it can be inspected from the module manager.
 */
object Logger {
    private const val TAG = "BiliMorphe"

    fun i(message: String) {
        XposedBridge.log("$TAG: $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        XposedBridge.log("$TAG: $message")
        if (throwable != null) {
            XposedBridge.log(throwable)
        }
    }
}
