package app.bilimorphe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.bilimorphe.settings.SettingsFragment

/**
 * 模块设置入口（LSPosed 管理器里点「模块设置」进入）。
 *
 * 内容照 1.28.6 的 XML 风格：单个 PreferenceScreen + 分组，见
 * `res/xml/bilimorphe_settings.xml`。
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}
