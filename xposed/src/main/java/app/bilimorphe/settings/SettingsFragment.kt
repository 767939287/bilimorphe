package app.bilimorphe.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.bilimorphe.R

/**
 * 设置页（照 1.28.6 的 XML 组织方式）。
 *
 * 首版只提供开关与占位入口；开关值由 [SettingsStore] 落到模块私有 SharedPreferences，
 * 宿主进程通过 `RemotePreferences` / 模块进程写入后由各 FeatureInstaller 读取。
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.bilimorphe_settings, rootKey)

        val placeholders = listOf(
            SettingsKeys.AI_CONFIG,
            SettingsKeys.AI_SUBTITLE_TRANSLATION_PROMPT,
            SettingsKeys.AI_VIDEO_FILTER_PROMPT,
            SettingsKeys.SB_CATEGORY,
            SettingsKeys.CATEGORY_DANMAKU,
            SettingsKeys.REPORT,
            SettingsKeys.SUPPORT,
        )
        for (key in placeholders) {
            findPreference<Preference>(key)?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), "「${it.title}」尚未实现", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
