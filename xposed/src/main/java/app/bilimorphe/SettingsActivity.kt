package app.bilimorphe

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import app.bilimorphe.patch.Patches

/**
 * Minimal settings screen so the module shows up in the LSPosed manager.
 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = buildString {
            appendLine("BiliMorphe")
            appendLine()
            appendLine("LSPosed runtime module for ${Patches.BILI_PACKAGE}.")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine()
            append("No settings yet.")
        }

        val view = TextView(this).apply {
            this.text = text
            setPadding(64, 120, 64, 64)
            textSize = 15f
            setTextIsSelectable(true)
        }

        setContentView(ScrollView(this).apply { addView(view) })
    }
}
